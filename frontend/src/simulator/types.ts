/**
 * Domain types, ported from the Java engine.
 *
 * Every name here matches a Java counterpart exactly — the parity test compares
 * these strings against the engine's output, so renaming one silently breaks
 * the comparison rather than failing to compile.
 */
import type { Decimal } from "./decimal";

export type Decision = "ALLOW" | "REVIEW" | "DECLINE";

export type ControlLayerId =
  | "IDENTITY_POSTURE"
  | "BEHAVIORAL_SCORING"
  | "COUNTERPARTY_NETWORK"
  | "EXTERNAL_ENRICHMENT"
  | "POST_SETTLEMENT";

export type PathClassification = "IN_PATH" | "ASYNC";

/**
 * NO_DATA is neither EVALUATED nor DEGRADED, and the distinction is
 * load-bearing. A layer that looked and found nothing known must not sit in the
 * score denominator contributing zero — that would let an unknown counterparty
 * act as evidence of safety. Nor is it a failure, or nearly every decision
 * would report as degraded.
 */
export type LayerStatus = "EVALUATED" | "NO_DATA" | "DEGRADED" | "TIMED_OUT" | "SKIPPED";

export type Channel = "MOBILE_APP" | "WEB" | "API" | "BRANCH" | "PHONE" | "UNKNOWN";

export type Rail = "FEDNOW" | "PIX" | "FASTER_PAYMENTS" | "OTHER";

export type CounterpartyRiskTier = "UNKNOWN" | "LOW" | "ELEVATED" | "HIGH" | "CONFIRMED";

export type NetworkFlag =
  | "FAN_IN"
  | "FAN_OUT"
  | "STRUCTURING"
  | "VELOCITY_BURST"
  | "RAPID_PASS_THROUGH";

export interface ControlLayer {
  readonly id: ControlLayerId;
  readonly number: number;
  readonly displayName: string;
  readonly path: PathClassification;
}

export const CONTROL_LAYERS: Record<ControlLayerId, ControlLayer> = {
  IDENTITY_POSTURE: {
    id: "IDENTITY_POSTURE",
    number: 1,
    displayName: "Identity & Account Posture",
    path: "IN_PATH",
  },
  BEHAVIORAL_SCORING: {
    id: "BEHAVIORAL_SCORING",
    number: 2,
    displayName: "Real-Time Behavioral Scoring",
    path: "IN_PATH",
  },
  COUNTERPARTY_NETWORK: {
    id: "COUNTERPARTY_NETWORK",
    number: 3,
    displayName: "Counterparty & Network Signals",
    path: "IN_PATH",
  },
  EXTERNAL_ENRICHMENT: {
    id: "EXTERNAL_ENRICHMENT",
    number: 4,
    displayName: "External Enrichment",
    path: "ASYNC",
  },
  POST_SETTLEMENT: {
    id: "POST_SETTLEMENT",
    number: 5,
    displayName: "Post-Settlement Analysis",
    path: "ASYNC",
  },
};

/** In-path layers in evaluation order: 1, then 2, then 3. */
export const IN_PATH_LAYERS: ControlLayerId[] = [
  "IDENTITY_POSTURE",
  "BEHAVIORAL_SCORING",
  "COUNTERPARTY_NETWORK",
];

export interface Transaction {
  transactionId: string;
  payerAccountId: string;
  payeeAccountId: string;
  amount: Decimal;
  currency: string;
  channel: Channel;
  deviceId: string | null;
  rail: Rail;
  /** Epoch milliseconds. */
  initiatedAt: number;
}

export interface AccountProfile {
  accountId: string;
  /** Epoch millis, or null — the age rule is skipped when absent. */
  openedAt: number | null;
  verified: boolean;
  knownDeviceIds: string[];
  typicalChannels: Channel[];
  baselineAmountMean: Decimal | null;
  baselineAmountStdDev: Decimal | null;
  knownCounterparties: string[];
  activeHourStart: number;
  activeHourEnd: number;
  transactionCount: number;
  restricted: boolean;
  /** Epoch millis, or null — the staleness check is skipped when absent. */
  computedAt: number | null;
}

export interface CounterpartyRiskState {
  counterpartyId: string;
  tier: CounterpartyRiskTier;
  flags: NetworkFlag[];
  reportedTypologies: string[];
  distinctPayers: number;
  version: number;
  computedAt: number;
}

export interface RiskFactor {
  code: string;
  layer: ControlLayerId | null;
  contribution: number;
  ruleId: string;
  ruleVersion: string;
  explanation: string;
}

export interface LayerResult {
  layer: ControlLayerId;
  status: LayerStatus;
  contribution: number;
  riskFactors: RiskFactor[];
  latencyMicros: number;
  stateVersion: string | null;
}

export interface EvaluationResult {
  transactionId: string;
  decision: Decision;
  riskScore: number;
  riskFactors: RiskFactor[];
  layerResults: LayerResult[];
  explanation: string;
  frameworkVersion: string;
  degraded: boolean;
  totalLatencyMicros: number;
}

/** isDegraded() in LayerResult.java. NO_DATA is deliberately not degraded. */
export const isDegraded = (result: LayerResult): boolean =>
  result.status === "DEGRADED" || result.status === "TIMED_OUT";

/** contributesToScore() in LayerResult.java. Only EVALUATED enters the denominator. */
export const contributesToScore = (result: LayerResult): boolean =>
  result.status === "EVALUATED";

export const isAdverseTier = (tier: CounterpartyRiskTier): boolean =>
  tier === "ELEVATED" || tier === "HIGH" || tier === "CONFIRMED";
