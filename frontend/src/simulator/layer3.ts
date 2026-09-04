/**
 * Layer 3 — Counterparty & Network Signals. Ported from CounterpartyNetworkEvaluator.
 *
 * Answers what we already know about where the money is going, without any
 * lookup that could be slow. The questions it wants to ask are graph questions
 * and genuinely expensive; the resolution is to run them in Layer 5 and read
 * the result here at the cost of one pre-computed lookup.
 *
 * Designed to be droppable: most valuable when state exists, harmless when it
 * does not.
 */
import { NETWORK_RULES } from "./generated/rules";
import { degradedFactor } from "./reasonCodes";
import type { CounterpartyRiskState, LayerResult, RiskFactor } from "./types";
import { isAdverseTier } from "./types";

const LAYER = "COUNTERPARTY_NETWORK" as const;

const factor = (
  code: RiskFactor["code"],
  ruleId: string,
  contribution: number,
  explanation: string,
): RiskFactor => ({ code, layer: LAYER, contribution, ruleId, ruleVersion: "1.0.0", explanation });

const versionLabel = (state: CounterpartyRiskState) =>
  `${state.counterpartyId}@v${state.version}`;

export function evaluateCounterpartyNetwork(
  payeeAccountId: string,
  state: CounterpartyRiskState | undefined,
  now: number,
  storeAvailable = true,
): LayerResult {
  // Losing the store degrades this layer whole. Layers 1-2 carry the decision,
  // and the policy deliberately does not escalate on a Layer 3 degradation —
  // otherwise every blip becomes a review queue.
  if (!storeAvailable) {
    return {
      layer: LAYER,
      status: "DEGRADED",
      contribution: 0,
      riskFactors: [degradedFactor("NETWORK_STATE_UNAVAILABLE", LAYER)],
      latencyMicros: 0,
      stateVersion: null,
    };
  }

  // The layer looked and nothing is known. NO_DATA, not DEGRADED: it did not
  // fail. And not EVALUATED, because an EVALUATED zero would sit in the score
  // denominator and let an unknown counterparty act as evidence of safety.
  if (!state) {
    return {
      layer: LAYER,
      status: "NO_DATA",
      contribution: 0,
      riskFactors: [degradedFactor("NETWORK_STATE_ABSENT", LAYER)],
      latencyMicros: 0,
      stateVersion: null,
    };
  }

  const ageMinutes = (now - state.computedAt) / 60_000;
  if (ageMinutes >= NETWORK_RULES.stateTtlMinutes) {
    // Not a blocking refresh, and not a silent zero. The version is still
    // recorded — the audit trail must show what was seen, including that it
    // was out of date.
    return {
      layer: LAYER,
      status: "DEGRADED",
      contribution: 0,
      riskFactors: [degradedFactor("NETWORK_STATE_STALE", LAYER)],
      latencyMicros: 0,
      stateVersion: versionLabel(state),
    };
  }

  const factors: RiskFactor[] = [];
  const tierWeight = NETWORK_RULES.tierWeights[state.tier] ?? 0;

  if (isAdverseTier(state.tier) && tierWeight > 0) {
    factors.push(
      factor(
        "COUNTERPARTY_RISK_TIER_ELEVATED",
        "L3.RISK_TIER",
        tierWeight,
        `Receiving account is classified ${state.tier} in pre-computed risk state (version ${state.version})`,
      ),
    );
  }

  if (state.flags.includes("FAN_IN")) {
    factors.push(
      factor(
        "COUNTERPARTY_FAN_IN_PATTERN",
        "L3.FAN_IN",
        NETWORK_RULES.fanInWeight,
        state.distinctPayers > 0
          ? `Receiving account has taken payments from ${state.distinctPayers} distinct payers in the aggregation window`
          : "Receiving account shows a fan-in pattern consistent with mule activity",
      ),
    );
  }

  if (state.reportedTypologies.length > 0) {
    factors.push(
      factor(
        "COUNTERPARTY_REPORTED_TYPOLOGY",
        "L3.REPORTED_TYPOLOGY",
        NETWORK_RULES.reportedTypologyWeight,
        `Reported typology against the receiving account: ${state.reportedTypologies.join(", ")}`,
      ),
    );
  }

  return {
    layer: LAYER,
    status: "EVALUATED",
    contribution: Math.min(1, factors.reduce((sum, f) => sum + f.contribution, 0)),
    riskFactors: factors,
    latencyMicros: 0,
    stateVersion: versionLabel(state),
  };
}
