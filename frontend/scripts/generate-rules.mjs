/**
 * Generates the simulator's rule constants from the backend's
 * application-rules.yml.
 *
 * The YAML is the single source of truth for every threshold and weight. The
 * TypeScript simulator shown on the landing page must not be able to drift from
 * the Java engine, and the reliable way to guarantee that is to make the
 * constants unwritable by hand. Runs on `prebuild` and `pretest`.
 *
 * Structural note: `post-settlement` lives at `iprf.post-settlement`, NOT under
 * `iprf.rules`. A generator that assumes one subtree silently drops it.
 */
import { readFileSync, writeFileSync, mkdirSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import yaml from "js-yaml";

const here = dirname(fileURLToPath(import.meta.url));
const SOURCE = resolve(
  here,
  "../../backend/risk-engine/src/main/resources/application-rules.yml",
);
const TARGET = resolve(here, "../src/simulator/generated/rules.ts");

function fail(message) {
  console.error(`\n  generate-rules: ${message}\n`);
  process.exit(1);
}

let parsed;
try {
  parsed = yaml.load(readFileSync(SOURCE, "utf8"));
} catch (error) {
  fail(`could not read ${SOURCE}\n  ${error.message}`);
}

const iprf = parsed?.iprf;
if (!iprf?.rules) {
  fail("expected an `iprf.rules` block in application-rules.yml");
}
const rules = iprf.rules;

/** Mirrors the startup validation in RuleProperties.java. A malformed
 *  configuration must stop the build, not produce a quietly wrong simulator. */
function requireUnit(value, name) {
  if (typeof value !== "number" || value < 0 || value > 1) {
    fail(`${name} must be a number within [0, 1], was ${value}`);
  }
  return value;
}
function requirePositive(value, name) {
  if (typeof value !== "number" || value <= 0) {
    fail(`${name} must be a positive number, was ${value}`);
  }
  return value;
}

const decision = rules.decision ?? fail("missing iprf.rules.decision");
requireUnit(decision["review-threshold"], "decision.review-threshold");
requireUnit(decision["decline-threshold"], "decision.decline-threshold");
if (decision["review-threshold"] > decision["decline-threshold"]) {
  fail("review-threshold must not exceed decline-threshold — REVIEW would be unreachable");
}

const network = rules.network ?? fail("missing iprf.rules.network");
const tierWeights = network["tier-weights"] ?? fail("missing network.tier-weights");
if ((tierWeights.UNKNOWN ?? 0) > 0) {
  fail(
    "network.tier-weights.UNKNOWN must be 0 — absence of information is not evidence of risk",
  );
}
requirePositive(network["state-ttl-minutes"], "network.state-ttl-minutes");

const behavioral = rules.behavioral ?? fail("missing iprf.rules.behavioral");
requirePositive(behavioral["amount-deviation-sigmas"], "behavioral.amount-deviation-sigmas");
requirePositive(behavioral["velocity-max-per-hour"], "behavioral.velocity-max-per-hour");
if (!(Number(behavioral["fallback-absolute-amount"]) > 0)) {
  fail("behavioral.fallback-absolute-amount must be positive");
}

const identity = rules.identity ?? fail("missing iprf.rules.identity");
const postSettlement = iprf["post-settlement"] ?? {};

/**
 * Amounts are emitted as STRINGS, never as JS numbers.
 *
 * The Java engine compares BigDecimal with `>=`, so exact equality at a
 * threshold FIRES the rule. Rounding 2500.00 through a float here would make
 * the boundary case the one that diverges — and boundary cases are exactly what
 * the parity test checks. The simulator parses these with its own decimal type.
 */
function amountLiteral(value) {
  return JSON.stringify(String(value));
}

const out = `// GENERATED FILE — DO NOT EDIT.
//
// Emitted by frontend/scripts/generate-rules.mjs from
// backend/risk-engine/src/main/resources/application-rules.yml
//
// Change a threshold in the YAML, not here. This file is regenerated on every
// build, so hand edits are silently lost — which is the point: the browser
// simulator and the Java engine cannot drift apart.

export const LAYER_WEIGHTS = {
  IDENTITY_POSTURE: ${rules["layer-weights"].IDENTITY_POSTURE},
  BEHAVIORAL_SCORING: ${rules["layer-weights"].BEHAVIORAL_SCORING},
  COUNTERPARTY_NETWORK: ${rules["layer-weights"].COUNTERPARTY_NETWORK},
} as const;

export const DECISION_THRESHOLDS = {
  review: ${decision["review-threshold"]},
  decline: ${decision["decline-threshold"]},
} as const;

export const IDENTITY_RULES = {
  newAccountDays: ${identity["new-account-days"]},
  newAccountWeight: ${identity["new-account-weight"]},
  unverifiedWeight: ${identity["unverified-weight"]},
  unknownDeviceWeight: ${identity["unknown-device-weight"]},
  restrictedWeight: ${identity["restricted-weight"]},
  profileMaxAgeHours: ${identity["profile-max-age-hours"]},
} as const;

export const BEHAVIORAL_RULES = {
  amountDeviationSigmas: ${behavioral["amount-deviation-sigmas"]},
  amountDeviationWeight: ${behavioral["amount-deviation-weight"]},
  newCounterpartyWeight: ${behavioral["new-counterparty-weight"]},
  unusualHourWeight: ${behavioral["unusual-hour-weight"]},
  channelSwitchWeight: ${behavioral["channel-switch-weight"]},
  velocityMaxPerHour: ${behavioral["velocity-max-per-hour"]},
  velocityWeight: ${behavioral["velocity-weight"]},
  minimumBaselineTransactions: ${behavioral["minimum-baseline-transactions"]},
  /** Decimal string. Compared with the simulator's decimal type, never a float. */
  fallbackAbsoluteAmount: ${amountLiteral(behavioral["fallback-absolute-amount"])},
} as const;

export const NETWORK_RULES = {
  stateTtlMinutes: ${network["state-ttl-minutes"]},
  tierWeights: {
    UNKNOWN: ${tierWeights.UNKNOWN},
    LOW: ${tierWeights.LOW},
    ELEVATED: ${tierWeights.ELEVATED},
    HIGH: ${tierWeights.HIGH},
    CONFIRMED: ${tierWeights.CONFIRMED},
  },
  fanInWeight: ${network["fan-in-weight"]},
  reportedTypologyWeight: ${network["reported-typology-weight"]},
} as const;

/** Layer 5 detector thresholds. Not used in-path; shown on the landing page. */
export const POST_SETTLEMENT_RULES = {
  windowHours: ${postSettlement["window-hours"]},
  fanInDistinctPayers: ${postSettlement["fan-in-distinct-payers"]},
  fanOutDistinctReceivers: ${postSettlement["fan-out-distinct-receivers"]},
  structuringMinRepeats: ${postSettlement["structuring-min-repeats"]},
  structuringThresholds: ${JSON.stringify(
    (postSettlement["structuring-thresholds"] ?? []).map(String),
  )},
  structuringBandPercent: ${postSettlement["structuring-band-percent"]},
} as const;

/** The rolling velocity window is a constant in the Java evaluator, not YAML. */
export const VELOCITY_WINDOW_HOURS = 1;
`;

mkdirSync(dirname(TARGET), { recursive: true });
writeFileSync(TARGET, out, "utf8");
console.log(`generate-rules: wrote ${TARGET}`);
