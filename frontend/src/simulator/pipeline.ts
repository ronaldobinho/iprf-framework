/**
 * The in-path pipeline: score composition, decision policy, explanation.
 * Ported from ScoreComposer, DecisionPolicy, ExplanationBuilder and
 * DecisionPipeline.
 */
import { DECISION_THRESHOLDS, LAYER_WEIGHTS } from "./generated/rules";
import { evaluateIdentityPosture } from "./layer1";
import { createVelocityStore, evaluateBehavioralScoring, type VelocityLookup } from "./layer2";
import { evaluateCounterpartyNetwork } from "./layer3";
import type {
  AccountProfile,
  CounterpartyRiskState,
  Decision,
  EvaluationResult,
  LayerResult,
  RiskFactor,
  Transaction,
} from "./types";
import { CONTROL_LAYERS, contributesToScore, isDegraded } from "./types";

export const FRAMEWORK_VERSION = "0.1.0-SNAPSHOT";

/**
 * Weighted mean over the layers that actually evaluated something.
 *
 * Layers that failed, timed out, or had no data are excluded from the
 * DENOMINATOR rather than counted as zero. Counting silence as zero risk would
 * make a failed dependency, or a counterparty never seen before, look like a
 * clean result.
 */
export function composeScore(results: LayerResult[]): number {
  let weightedSum = 0;
  let totalWeight = 0;
  for (const result of results) {
    if (!contributesToScore(result)) continue;
    const weight = LAYER_WEIGHTS[result.layer as keyof typeof LAYER_WEIGHTS] ?? 0;
    weightedSum += result.contribution * weight;
    totalWeight += weight;
  }
  // Zero here means no information, not no risk. The policy handles it.
  if (totalWeight === 0) return 0;
  return Math.max(0, Math.min(1, weightedSum / totalWeight));
}

/**
 * Thresholds, plus the rule that a degraded evaluation must never silently
 * become an ALLOW.
 *
 * Thresholds alone are not enough: a pipeline that scored 0.0 because every
 * layer failed would, on thresholds alone, approve the payment, under exactly
 * the conditions an attacker would want to create.
 */
export function decide(riskScore: number, results: LayerResult[]): Decision {
  const scored: Decision =
    riskScore >= DECISION_THRESHOLDS.decline
      ? "DECLINE"
      : riskScore >= DECISION_THRESHOLDS.review
        ? "REVIEW"
        : "ALLOW";

  // Nothing evaluated successfully: the score carries no information at all.
  if (results.length > 0 && results.every(isDegraded)) {
    return "REVIEW";
  }

  // Layers 1-2 are the primary signal. If either degraded, the remaining
  // evidence cannot support a confident approval. A degraded Layer 3 does not
  // escalate: it is designed to contribute nothing when unavailable.
  const primaryDegraded = results.some(
    (r) =>
      (r.layer === "IDENTITY_POSTURE" || r.layer === "BEHAVIORAL_SCORING") && isDegraded(r),
  );
  if (scored === "ALLOW" && primaryDegraded) {
    return "REVIEW";
  }

  return scored;
}

const VERDICT: Record<Decision, string> = {
  ALLOW: "Approved for the real-time path",
  REVIEW: "Held for review",
  DECLINE: "Declined",
};

const MAX_FACTORS_NAMED = 3;

/** Fixed decimal point, never a locale separator. Matches Locale.ROOT on the engine. */
const fixed2 = (value: number) => value.toFixed(2);

export function buildExplanation(
  decision: Decision,
  riskScore: number,
  factors: RiskFactor[],
  results: LayerResult[],
): string {
  let text = `${VERDICT[decision]} with a composite risk score of ${fixed2(riskScore)}.`;

  const contributing = factors
    .filter((f) => f.contribution > 0)
    .sort((a, b) => b.contribution - a.contribution)
    .slice(0, MAX_FACTORS_NAMED);

  if (contributing.length === 0) {
    text += " No rule produced a risk contribution.";
  } else {
    text +=
      " Principal factors: " +
      contributing
        .map((f) => `${f.explanation} (${f.code}, contribution ${fixed2(f.contribution)})`)
        .join("; ") +
      ".";
  }

  const degraded = results.filter(isDegraded).map((r) => CONTROL_LAYERS[r.layer].displayName);
  if (degraded.length > 0) {
    text += ` This decision was made on incomplete input: ${degraded.join(", ")} did not evaluate.`;
  }

  return text;
}

export interface SimulatorState {
  profiles: Map<string, AccountProfile>;
  riskState: Map<string, CounterpartyRiskState>;
  velocity: VelocityLookup;
  riskStoreAvailable?: boolean;
}

export function createSimulatorState(
  profiles: AccountProfile[],
  riskState: CounterpartyRiskState[] = [],
): SimulatorState {
  return {
    profiles: new Map(profiles.map((p) => [p.accountId, p])),
    riskState: new Map(riskState.map((s) => [s.counterpartyId, s])),
    velocity: createVelocityStore(),
    riskStoreAvailable: true,
  };
}

/** Runs Layers 1 to 3 in order and composes a decision. */
export function evaluate(
  transaction: Transaction,
  state: SimulatorState,
  now: number = transaction.initiatedAt,
): EvaluationResult {
  const start = clockNow();
  const profile = state.profiles.get(transaction.payerAccountId);

  const results: LayerResult[] = [
    timed(() => evaluateIdentityPosture(transaction, profile, now)),
    timed(() => evaluateBehavioralScoring(transaction, profile, now, state.velocity)),
    timed(() =>
      evaluateCounterpartyNetwork(
        transaction.payeeAccountId,
        state.riskState.get(transaction.payeeAccountId),
        now,
        state.riskStoreAvailable !== false,
      ),
    ),
  ];

  const riskFactors = results.flatMap((r) => r.riskFactors);
  const riskScore = composeScore(results);
  const decision = decide(riskScore, results);

  return {
    transactionId: transaction.transactionId,
    decision,
    riskScore,
    riskFactors,
    layerResults: results,
    explanation: buildExplanation(decision, riskScore, riskFactors, results),
    frameworkVersion: FRAMEWORK_VERSION,
    degraded: results.some(isDegraded),
    totalLatencyMicros: Math.round((clockNow() - start) * 1000),
  };
}

function clockNow(): number {
  return typeof performance !== "undefined" ? performance.now() : Date.now();
}

function timed(run: () => LayerResult): LayerResult {
  const start = clockNow();
  const result = run();
  return { ...result, latencyMicros: Math.round((clockNow() - start) * 1000) };
}
