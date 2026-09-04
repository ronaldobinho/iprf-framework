/**
 * Layer 2 — Real-Time Behavioral Scoring. Ported from BehavioralScoringEvaluator.
 *
 * Detects deviation from THIS payer's baseline. Absolute thresholds are a weak
 * control: trivially learned, trivially evaded, and they punish legitimately
 * high-value customers. A baseline the attacker cannot see is harder to dodge.
 *
 * Every signal here is individually weak and individually a false-positive
 * generator — a first payment to a new counterparty is what happens whenever
 * someone pays a new landlord. Composition is what makes them usable.
 */
import { Decimal, dec } from "./decimal";
import { BEHAVIORAL_RULES, VELOCITY_WINDOW_HOURS } from "./generated/rules";
import { degradedFactor } from "./reasonCodes";
import type { AccountProfile, LayerResult, RiskFactor, Transaction } from "./types";

const LAYER = "BEHAVIORAL_SCORING" as const;
const FALLBACK_AMOUNT = dec(BEHAVIORAL_RULES.fallbackAbsoluteAmount);
const SIGMAS = dec(BEHAVIORAL_RULES.amountDeviationSigmas);

const factor = (
  code: RiskFactor["code"],
  ruleId: string,
  contribution: number,
  explanation: string,
): RiskFactor => ({ code, layer: LAYER, contribution, ruleId, ruleVersion: "1.0.0", explanation });

/** Prior payments in the rolling window, counted BEFORE this one is recorded. */
export interface VelocityLookup {
  countWithin(accountId: string, windowHours: number, now: number): number;
  record(accountId: string, at: number): void;
}

export function evaluateBehavioralScoring(
  transaction: Transaction,
  profile: AccountProfile | undefined,
  now: number,
  velocity: VelocityLookup,
): LayerResult {
  if (!profile) {
    velocity.record(transaction.payerAccountId, now);
    return {
      layer: LAYER,
      status: "DEGRADED",
      contribution: 0,
      riskFactors: [degradedFactor("BASELINE_INSUFFICIENT", LAYER)],
      latencyMicros: 0,
      stateVersion: null,
    };
  }

  const factors: RiskFactor[] = [];
  const mean = profile.baselineAmountMean;
  const baselineUsable =
    profile.transactionCount >= BEHAVIORAL_RULES.minimumBaselineTransactions &&
    mean !== null &&
    mean.signum() > 0;

  // --- amount -------------------------------------------------------------
  if (baselineUsable && mean !== null) {
    const stdDev = profile.baselineAmountStdDev ?? Decimal.ZERO;
    const threshold = mean.add(stdDev.multiply(SIGMAS));

    // `>=` — exact equality at the threshold fires. This is why amounts are
    // decimal here and not floats.
    if (transaction.amount.gte(threshold)) {
      const sigmas = stdDev.isZero()
        ? "n/a (zero variance)"
        : transaction.amount.subtract(mean).divide(stdDev, 1).toString();
      factors.push(
        factor(
          "AMOUNT_DEVIATION_HIGH",
          "L2.AMOUNT_DEVIATION",
          BEHAVIORAL_RULES.amountDeviationWeight,
          `Amount ${transaction.amount.toString()} ${transaction.currency} is ${sigmas} standard deviations above this payer's mean of ${mean.toString()}`,
        ),
      );
    }
  } else {
    // New accounts are where mule activity concentrates, so an unusable
    // baseline must never resolve silently to "low risk". Fall back to a
    // conservative absolute threshold, and record that it happened.
    factors.push({
      code: "BASELINE_INSUFFICIENT",
      layer: LAYER,
      contribution: 0,
      ruleId: "L2.BASELINE_FALLBACK",
      ruleVersion: "1.0.0",
      explanation: `Payer has ${profile.transactionCount} transactions, below the ${BEHAVIORAL_RULES.minimumBaselineTransactions} required for deviation rules; absolute threshold applied`,
    });
    if (transaction.amount.gte(FALLBACK_AMOUNT)) {
      factors.push(
        factor(
          "AMOUNT_DEVIATION_HIGH",
          "L2.AMOUNT_DEVIATION",
          BEHAVIORAL_RULES.amountDeviationWeight,
          `Amount ${transaction.amount.toString()} ${transaction.currency} meets the absolute threshold of ${FALLBACK_AMOUNT.toString()} applied to payers without a baseline`,
        ),
      );
    }
  }

  // --- counterparty -------------------------------------------------------
  const selfTransfer = transaction.payerAccountId === transaction.payeeAccountId;
  if (!selfTransfer && !profile.knownCounterparties.includes(transaction.payeeAccountId)) {
    factors.push(
      factor(
        "COUNTERPARTY_NEW",
        "L2.NEW_COUNTERPARTY",
        BEHAVIORAL_RULES.newCounterpartyWeight,
        "First payment from this payer to this counterparty",
      ),
    );
  }

  // --- timing -------------------------------------------------------------
  // Compared in UTC. A production deployment should use the payer's local time;
  // the profile carries no timezone. Known limitation, not a modelling choice.
  const hour = new Date(transaction.initiatedAt).getUTCHours();
  const { activeHourStart: start, activeHourEnd: end } = profile;
  const outsideActiveHours =
    start === end
      ? false
      : start <= end
        ? hour < start || hour > end
        : // Window wraps past midnight: for 22:00-06:00 the outside region is
          // the contiguous span 07:00-21:00, so both bounds apply together.
          hour < start && hour > end;

  if (outsideActiveHours) {
    factors.push(
      factor(
        "TIMING_UNUSUAL_HOUR",
        "L2.UNUSUAL_HOUR",
        BEHAVIORAL_RULES.unusualHourWeight,
        `Initiated at ${hour}:00 UTC, outside this payer's active window of ${start}:00-${end}:00`,
      ),
    );
  }

  // --- velocity -----------------------------------------------------------
  const priorInWindow = velocity.countWithin(
    transaction.payerAccountId,
    VELOCITY_WINDOW_HOURS,
    now,
  );
  if (priorInWindow >= BEHAVIORAL_RULES.velocityMaxPerHour) {
    factors.push(
      factor(
        "VELOCITY_WINDOW_EXCEEDED",
        "L2.VELOCITY",
        BEHAVIORAL_RULES.velocityWeight,
        `${priorInWindow} payments in the preceding hour, at or above the configured maximum of ${BEHAVIORAL_RULES.velocityMaxPerHour}`,
      ),
    );
  }

  // --- channel ------------------------------------------------------------
  if (
    profile.typicalChannels.length > 0 &&
    !profile.typicalChannels.includes(transaction.channel)
  ) {
    factors.push(
      factor(
        "CHANNEL_UNUSUAL",
        "L2.CHANNEL_SWITCH",
        BEHAVIORAL_RULES.channelSwitchWeight,
        `Channel ${transaction.channel} is not among this payer's typical channels`,
      ),
    );
  }

  // Recorded after counting, so a payment never inflates its own reading.
  velocity.record(transaction.payerAccountId, now);

  return {
    layer: LAYER,
    status: "EVALUATED",
    contribution: Math.min(1, factors.reduce((sum, f) => sum + f.contribution, 0)),
    riskFactors: factors,
    latencyMicros: 0,
    stateVersion: null,
  };
}

/** In-memory sliding window, matching InMemoryVelocityCounterStore. */
export function createVelocityStore(): VelocityLookup & { clear(): void } {
  const timestamps = new Map<string, number[]>();
  return {
    countWithin(accountId, windowHours, now) {
      const entries = timestamps.get(accountId);
      if (!entries) return 0;
      const cutoff = now - windowHours * 3_600_000;
      const kept = entries.filter((at) => at >= cutoff);
      timestamps.set(accountId, kept);
      return kept.length;
    },
    record(accountId, at) {
      const entries = timestamps.get(accountId) ?? [];
      entries.push(at);
      timestamps.set(accountId, entries);
    },
    clear() {
      timestamps.clear();
    },
  };
}
