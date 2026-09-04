/**
 * Layer 1 — Identity & Account Posture. Ported from IdentityPostureEvaluator.
 *
 * Runs first because it is the cheapest layer and a decisive answer here avoids
 * the cost of everything downstream. Reads only the pre-computed profile.
 */
import { IDENTITY_RULES } from "./generated/rules";
import { degradedFactor } from "./reasonCodes";
import type { AccountProfile, LayerResult, RiskFactor, Transaction } from "./types";

const LAYER = "IDENTITY_POSTURE" as const;
const MS_PER_DAY = 86_400_000;
const MS_PER_HOUR = 3_600_000;

/** Duration.toDays() / toHours() truncate toward zero — Math.trunc, not floor. */
const daysBetween = (from: number, to: number) => Math.trunc((to - from) / MS_PER_DAY);
const hoursBetween = (from: number, to: number) => Math.trunc((to - from) / MS_PER_HOUR);

const factor = (
  code: RiskFactor["code"],
  ruleId: string,
  contribution: number,
  explanation: string,
): RiskFactor => ({
  code,
  layer: LAYER,
  contribution,
  ruleId,
  ruleVersion: "1.0.0",
  explanation,
});

export function evaluateIdentityPosture(
  transaction: Transaction,
  profile: AccountProfile | undefined,
  now: number,
): LayerResult {
  // An unknown payer is an operational gap, not evidence of fraud. It degrades
  // rather than declining — but loudly, and the decision policy escalates the
  // resulting ALLOW to REVIEW.
  if (!profile) {
    return {
      layer: LAYER,
      status: "DEGRADED",
      contribution: 0,
      riskFactors: [degradedFactor("IDENTITY_PROFILE_MISSING", LAYER)],
      latencyMicros: 0,
      stateVersion: null,
    };
  }

  // Staleness short-circuits the whole layer, before any rule runs.
  if (
    profile.computedAt !== null &&
    hoursBetween(profile.computedAt, now) >= IDENTITY_RULES.profileMaxAgeHours
  ) {
    return {
      layer: LAYER,
      status: "DEGRADED",
      contribution: 0,
      riskFactors: [degradedFactor("IDENTITY_PROFILE_STALE", LAYER)],
      latencyMicros: 0,
      stateVersion: null,
    };
  }

  const factors: RiskFactor[] = [];

  if (profile.openedAt !== null) {
    const ageDays = daysBetween(profile.openedAt, now);
    if (ageDays < IDENTITY_RULES.newAccountDays) {
      factors.push(
        factor(
          "ACCOUNT_AGE_LOW",
          "L1.ACCOUNT_AGE",
          IDENTITY_RULES.newAccountWeight,
          `Account is ${ageDays} days old, below the ${IDENTITY_RULES.newAccountDays}-day threshold`,
        ),
      );
    }
  }

  if (!profile.verified) {
    factors.push(
      factor(
        "VERIFICATION_INCOMPLETE",
        "L1.VERIFICATION",
        IDENTITY_RULES.unverifiedWeight,
        "Account identity verification is below the expected tier",
      ),
    );
  }

  // isKnownDevice is false for a null id, so an absent device identifier fires
  // this rule rather than being waved through.
  const knownDevice =
    transaction.deviceId !== null && profile.knownDeviceIds.includes(transaction.deviceId);
  if (!knownDevice) {
    factors.push(
      factor(
        "DEVICE_UNKNOWN",
        "L1.DEVICE",
        IDENTITY_RULES.unknownDeviceWeight,
        transaction.deviceId === null
          ? "No device identifier was supplied for this channel"
          : "Device has not been seen for this account before",
      ),
    );
  }

  if (profile.restricted) {
    factors.push(
      factor(
        "ACCOUNT_RESTRICTED",
        "L1.RESTRICTION",
        IDENTITY_RULES.restrictedWeight,
        "Account carries an active restriction or prior confirmed-fraud marker",
      ),
    );
  }

  return {
    layer: LAYER,
    status: "EVALUATED",
    // Additive then clamped, never averaged: three simultaneous posture
    // problems are worse than one, and averaging would let extra problems
    // dilute the signal.
    contribution: Math.min(1, factors.reduce((sum, f) => sum + f.contribution, 0)),
    riskFactors: factors,
    latencyMicros: 0,
    stateVersion: null,
  };
}
