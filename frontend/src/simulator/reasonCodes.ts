/**
 * The reason code vocabulary, ported verbatim from ReasonCode.java.
 *
 * This is a published interface on the Java side: codes are added, never
 * renamed, because historical audit records and this simulator both depend on
 * them meaning the same thing indefinitely. The explanation strings are copied
 * exactly — the parity test compares rendered explanations byte for byte.
 */
import type { ControlLayerId } from "./types";

export interface ReasonCodeDefinition {
  readonly layer: ControlLayerId | null;
  readonly explanation: string;
  /**
   * Detections say something about the transaction; degradations say something
   * about the system's ability to evaluate it. The UI renders them differently
   * because conflating the two is how "we could not check" gets read as
   * "we checked and it was fine".
   */
  readonly kind: "detection" | "degradation";
}

export const REASON_CODES = {
  ACCOUNT_AGE_LOW: {
    layer: "IDENTITY_POSTURE",
    explanation: "Account was opened recently",
    kind: "detection",
  },
  VERIFICATION_INCOMPLETE: {
    layer: "IDENTITY_POSTURE",
    explanation: "Account identity verification is below the expected tier",
    kind: "detection",
  },
  DEVICE_UNKNOWN: {
    layer: "IDENTITY_POSTURE",
    explanation: "Payment initiated from a device not previously seen for this account",
    kind: "detection",
  },
  ACCOUNT_RESTRICTED: {
    layer: "IDENTITY_POSTURE",
    explanation: "Account carries an active restriction or prior confirmed-fraud marker",
    kind: "detection",
  },
  IDENTITY_PROFILE_MISSING: {
    layer: "IDENTITY_POSTURE",
    explanation:
      "No pre-loaded profile exists for this account; treated as maximum uncertainty",
    kind: "degradation",
  },
  IDENTITY_PROFILE_STALE: {
    layer: "IDENTITY_POSTURE",
    explanation: "Account profile is older than the configured freshness window",
    kind: "degradation",
  },
  AMOUNT_DEVIATION_HIGH: {
    layer: "BEHAVIORAL_SCORING",
    explanation: "Amount is far outside this payer's established range",
    kind: "detection",
  },
  COUNTERPARTY_NEW: {
    layer: "BEHAVIORAL_SCORING",
    explanation: "First payment from this payer to this counterparty",
    kind: "detection",
  },
  TIMING_UNUSUAL_HOUR: {
    layer: "BEHAVIORAL_SCORING",
    explanation: "Payment initiated outside this payer's typical active hours",
    kind: "detection",
  },
  VELOCITY_WINDOW_EXCEEDED: {
    layer: "BEHAVIORAL_SCORING",
    explanation: "Payment count in the rolling window exceeds this payer's normal rate",
    kind: "detection",
  },
  CHANNEL_UNUSUAL: {
    layer: "BEHAVIORAL_SCORING",
    explanation: "Payment initiated on a channel this payer does not normally use",
    kind: "detection",
  },
  BASELINE_INSUFFICIENT: {
    layer: "BEHAVIORAL_SCORING",
    explanation:
      "Payer has too little history for deviation rules; conservative absolute thresholds applied instead",
    kind: "degradation",
  },
  COUNTERPARTY_RISK_TIER_ELEVATED: {
    layer: "COUNTERPARTY_NETWORK",
    explanation: "Receiving account carries an elevated pre-computed risk tier",
    kind: "detection",
  },
  COUNTERPARTY_FAN_IN_PATTERN: {
    layer: "COUNTERPARTY_NETWORK",
    explanation: "Receiving account shows a fan-in pattern consistent with mule activity",
    kind: "detection",
  },
  COUNTERPARTY_REPORTED_TYPOLOGY: {
    layer: "COUNTERPARTY_NETWORK",
    explanation: "Receiving account has a previously reported fraud typology",
    kind: "detection",
  },
  NETWORK_STATE_ABSENT: {
    layer: "COUNTERPARTY_NETWORK",
    explanation: "No pre-computed state exists for this counterparty",
    kind: "degradation",
  },
  NETWORK_STATE_STALE: {
    layer: "COUNTERPARTY_NETWORK",
    explanation: "Counterparty state is older than the configured TTL",
    kind: "degradation",
  },
  NETWORK_STATE_UNAVAILABLE: {
    layer: "COUNTERPARTY_NETWORK",
    explanation: "The risk state store could not be reached; layer degraded whole",
    kind: "degradation",
  },
  LAYER_TIMEOUT: {
    layer: null,
    explanation: "Layer exceeded its latency budget and was cut off",
    kind: "degradation",
  },
  NO_SIGNAL: {
    layer: null,
    explanation: "No rule produced a contribution",
    kind: "degradation",
  },
} as const satisfies Record<string, ReasonCodeDefinition>;

export type ReasonCode = keyof typeof REASON_CODES;

export const reasonCode = (code: ReasonCode): ReasonCodeDefinition => REASON_CODES[code];

/** RiskFactor.degraded() in the Java engine: zero contribution, sentinel rule id. */
export function degradedFactor(code: ReasonCode, layer: ControlLayerId) {
  return {
    code,
    layer,
    contribution: 0,
    ruleId: "degradation",
    ruleVersion: "n/a",
    explanation: REASON_CODES[code].explanation,
  };
}
