import { describe, expect, it } from "vitest";
import { dec } from "../decimal";
import { DEMO_PAYER, PRESETS, demoProfiles, demoRiskState } from "../presets";
import { composeScore, createSimulatorState, decide, evaluate } from "../pipeline";
import type { AccountProfile, LayerResult, Transaction } from "../types";

const NOW = Date.UTC(2026, 7, 16, 14, 0, 0);
const DAY = 86_400_000;

function state(profileOverrides: Partial<AccountProfile> = {}, withRiskState = true) {
  const [profile] = demoProfiles(NOW);
  return createSimulatorState(
    [{ ...profile, ...profileOverrides }],
    withRiskState ? demoRiskState(NOW) : [],
  );
}

function txn(overrides: Partial<Transaction> = {}): Transaction {
  return {
    transactionId: "txn-1",
    payerAccountId: DEMO_PAYER,
    payeeAccountId: "acct-utility-8842",
    amount: dec("184.20"),
    currency: "USD",
    channel: "MOBILE_APP",
    deviceId: "dev-8f21c4",
    rail: "FEDNOW",
    initiatedAt: NOW,
    ...overrides,
  };
}

const codes = (result: { riskFactors: { code: string }[] }) =>
  result.riskFactors.map((f) => f.code);

describe("the demo presets produce the outcomes the landing page claims", () => {
  it.each(PRESETS.map((preset) => [preset.label, preset] as const))("%s", (_label, preset) => {
    expect(evaluate(preset.build(NOW), state(), NOW).decision).toBe(preset.expected);
  });

  it("decides the collector scenario partly on Layer 3, not only on Layers 1-2", () => {
    const collector = PRESETS.find((preset) => preset.id === "collector")!;
    const result = evaluate(collector.build(NOW), state(), NOW);
    const layer3 = result.layerResults.find((r) => r.layer === "COUNTERPARTY_NETWORK")!;

    expect(layer3.status).toBe("EVALUATED");
    expect(layer3.contribution).toBeGreaterThan(0);
    expect(codes(result)).toContain("COUNTERPARTY_FAN_IN_PATTERN");
    // The state version read is recorded, so the decision can be reconstructed.
    expect(layer3.stateVersion).toBe("acct-9931-collector@v4");
  });
});

describe("threshold boundaries", () => {
  it("fires at exactly mean plus three sigma, and not one cent below", () => {
    // mean 240.00, stdDev 60.00, so the threshold is exactly 420.00
    expect(codes(evaluate(txn({ amount: dec("420.00") }), state(), NOW))).toContain(
      "AMOUNT_DEVIATION_HIGH",
    );
    expect(codes(evaluate(txn({ amount: dec("419.99") }), state(), NOW))).not.toContain(
      "AMOUNT_DEVIATION_HIGH",
    );
  });

  it("fires at exactly the no-baseline fallback amount", () => {
    const thin = { transactionCount: 3 };
    expect(codes(evaluate(txn({ amount: dec("2500.00") }), state(thin), NOW))).toContain(
      "AMOUNT_DEVIATION_HIGH",
    );

    const justBelow = evaluate(txn({ amount: dec("2499.99") }), state(thin), NOW);
    expect(codes(justBelow)).not.toContain("AMOUNT_DEVIATION_HIGH");
    // The fallback is recorded even when it does not fire, so the audit trail
    // shows there was no baseline to compare against.
    expect(codes(justBelow)).toContain("BASELINE_INSUFFICIENT");
  });

  it("treats the account age threshold as exclusive", () => {
    expect(codes(evaluate(txn(), state({ openedAt: NOW - 30 * DAY }), NOW))).not.toContain(
      "ACCOUNT_AGE_LOW",
    );
    expect(codes(evaluate(txn(), state({ openedAt: NOW - 29 * DAY }), NOW))).toContain(
      "ACCOUNT_AGE_LOW",
    );
  });

  it("treats the active hour bounds as inclusive", () => {
    // The demo profile is active 07:00-22:00 UTC.
    expect(
      codes(evaluate(txn({ initiatedAt: Date.UTC(2026, 7, 16, 7, 0) }), state(), NOW)),
    ).not.toContain("TIMING_UNUSUAL_HOUR");
    expect(
      codes(evaluate(txn({ initiatedAt: Date.UTC(2026, 7, 16, 6, 59) }), state(), NOW)),
    ).toContain("TIMING_UNUSUAL_HOUR");
  });
});

describe("degradation never becomes a silent approval", () => {
  it("holds an unknown payer for review rather than allowing it", () => {
    const result = evaluate(txn({ payerAccountId: "acct-nobody" }), state(), NOW);

    expect(result.riskScore).toBe(0);
    // On thresholds alone a score of zero approves. It must not.
    expect(result.decision).toBe("REVIEW");
    expect(result.degraded).toBe(true);
  });

  it("degrades on a stale profile and holds for review", () => {
    const result = evaluate(txn(), state({ computedAt: NOW - 25 * 3_600_000 }), NOW);

    expect(result.decision).toBe("REVIEW");
    expect(codes(result)).toContain("IDENTITY_PROFILE_STALE");
  });

  it("reports an unknown counterparty as NO_DATA without diluting the score", () => {
    const result = evaluate(txn({ payeeAccountId: "acct-never-seen" }), state(), NOW);
    const layer3 = result.layerResults.find((r) => r.layer === "COUNTERPARTY_NETWORK")!;

    expect(layer3.status).toBe("NO_DATA");
    // Not a failure, so the degraded metric stays meaningful...
    expect(result.degraded).toBe(false);
    // ...and excluded from scoring, so silence is not evidence of safety.
    expect(result.decision).toBe("ALLOW");
  });
});

describe("score composition", () => {
  const evaluated = (layer: LayerResult["layer"], contribution: number): LayerResult => ({
    layer,
    status: "EVALUATED",
    contribution,
    riskFactors: [],
    latencyMicros: 0,
    stateVersion: null,
  });

  it("is a weighted mean over the layers that evaluated", () => {
    // Weights are 0.25 / 0.50 / 0.25.
    expect(
      composeScore([
        evaluated("IDENTITY_POSTURE", 0.4),
        evaluated("BEHAVIORAL_SCORING", 0.6),
        evaluated("COUNTERPARTY_NETWORK", 0.2),
      ]),
    ).toBeCloseTo(0.45, 10);
  });

  it("excludes a NO_DATA layer from the denominator", () => {
    const noData: LayerResult = {
      layer: "COUNTERPARTY_NETWORK",
      status: "NO_DATA",
      contribution: 0,
      riskFactors: [],
      latencyMicros: 0,
      stateVersion: null,
    };
    const layers = [evaluated("IDENTITY_POSTURE", 0.4), evaluated("BEHAVIORAL_SCORING", 0.6)];

    // An unknown counterparty must score identically to having no Layer 3 at
    // all. An EVALUATED zero would drop the score instead.
    expect(composeScore([...layers, noData])).toBeCloseTo(composeScore(layers), 10);
  });

  it("returns zero when nothing evaluated, which the policy turns into REVIEW", () => {
    const allDegraded: LayerResult[] = [
      {
        layer: "IDENTITY_POSTURE",
        status: "DEGRADED",
        contribution: 0,
        riskFactors: [],
        latencyMicros: 0,
        stateVersion: null,
      },
    ];

    expect(composeScore(allDegraded)).toBe(0);
    expect(decide(0, allDegraded)).toBe("REVIEW");
  });
});

describe("explanations", () => {
  it("use a decimal point regardless of the host locale", () => {
    const result = evaluate(PRESETS[1].build(NOW), state(), NOW);

    expect(result.explanation).toMatch(/composite risk score of \d\.\d\d/);
    expect(result.explanation).not.toContain(",00");
  });

  it("name at most three factors", () => {
    const result = evaluate(PRESETS[2].build(NOW), state(), NOW);
    const named = result.explanation.match(/contribution \d\.\d\d/g) ?? [];

    expect(named.length).toBeLessThanOrEqual(3);
    expect(named.length).toBeGreaterThan(0);
  });

  it("say when the decision was made on incomplete input", () => {
    const result = evaluate(txn({ payerAccountId: "acct-nobody" }), state(), NOW);

    expect(result.explanation).toContain("incomplete input");
  });
});
