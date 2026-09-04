"use client";

import { useMemo, useState } from "react";
import { dec } from "@/simulator/decimal";
import { createSimulatorState, evaluate } from "@/simulator/pipeline";
import { CHANNELS, PRESETS, demoProfiles, demoRiskState, DEMO_PAYER } from "@/simulator/presets";
import { CONTROL_LAYERS, type Channel, type EvaluationResult } from "@/simulator/types";
import { REASON_CODES, type ReasonCode } from "@/simulator/reasonCodes";
import { DecisionPill, SyntheticBadge } from "@/components/ui";

/**
 * A fixed evaluation instant.
 *
 * The engine is deterministic, and the demo has to be too: the same click must
 * produce the same decision for every visitor, on any day. Wall-clock time
 * would shift account age and the active-hour comparison underneath the reader.
 */
const NOW = Date.UTC(2026, 7, 16, 12, 0, 0);

interface FormState {
  payeeAccountId: string;
  amount: string;
  channel: Channel;
  deviceId: string;
  hourUtc: number;
}

function formFromPreset(presetId: string): FormState {
  const preset = PRESETS.find((p) => p.id === presetId) ?? PRESETS[0];
  const transaction = preset.build(NOW);
  return {
    payeeAccountId: transaction.payeeAccountId,
    amount: transaction.amount.toString(),
    channel: transaction.channel,
    deviceId: transaction.deviceId ?? "",
    hourUtc: new Date(transaction.initiatedAt).getUTCHours(),
  };
}

export function Demo() {
  const [presetId, setPresetId] = useState(PRESETS[0].id);
  const [form, setForm] = useState<FormState>(() => formFromPreset(PRESETS[0].id));

  const activePreset = PRESETS.find((p) => p.id === presetId);

  const result: EvaluationResult | { error: string } = useMemo(() => {
    let amount;
    try {
      amount = dec(form.amount);
      if (amount.signum() <= 0) {
        return { error: "Amount must be greater than zero." };
      }
    } catch {
      return { error: "Amount must be a decimal number, for example 184.20." };
    }
    if (!form.payeeAccountId.trim()) {
      return { error: "A payee account is required." };
    }

    // Rebuilt per evaluation so the velocity counter does not accumulate across
    // keystrokes and quietly trip the velocity rule.
    const state = createSimulatorState(demoProfiles(NOW), demoRiskState(NOW));
    const initiatedAt = new Date(NOW);
    initiatedAt.setUTCHours(form.hourUtc, 17, 0, 0);

    return evaluate(
      {
        transactionId: "txn-demo",
        payerAccountId: DEMO_PAYER,
        payeeAccountId: form.payeeAccountId.trim(),
        amount,
        currency: "USD",
        channel: form.channel,
        deviceId: form.deviceId.trim() === "" ? null : form.deviceId.trim(),
        rail: "FEDNOW",
        initiatedAt: initiatedAt.getTime(),
      },
      state,
      NOW,
    );
  }, [form]);

  function applyPreset(id: string) {
    setPresetId(id);
    setForm(formFromPreset(id));
  }

  function update<K extends keyof FormState>(key: K, value: FormState[K]) {
    setPresetId("");
    setForm((current) => ({ ...current, [key]: value }));
  }

  return (
    <div className="overflow-hidden rounded-lg border border-edge bg-ink-raised">
      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-edge bg-ink-high px-5 py-3">
        <p className="font-mono text-2xs uppercase tracking-[0.14em] text-fg-muted">
          POST /api/v1/transactions/evaluate
        </p>
        <SyntheticBadge />
      </div>

      <div className="grid gap-0 lg:grid-cols-[minmax(0,22rem)_minmax(0,1fr)]">
        {/* ---- input ---- */}
        <div className="border-edge p-5 lg:border-r">
          <fieldset>
            <legend className="mb-3 text-sm font-medium text-fg">Scenario</legend>
            <div className="flex flex-col gap-2">
              {PRESETS.map((preset) => (
                <button
                  key={preset.id}
                  type="button"
                  onClick={() => applyPreset(preset.id)}
                  aria-pressed={presetId === preset.id}
                  className={`rounded border px-3 py-2.5 text-left text-sm transition-colors ${
                    presetId === preset.id
                      ? "border-accent-dim bg-accent-wash text-fg"
                      : "border-edge bg-ink text-fg-muted hover:border-edge-strong hover:text-fg"
                  }`}
                >
                  {preset.label}
                </button>
              ))}
            </div>
          </fieldset>

          <div className="mt-6 space-y-4">
            <Field label="Amount (USD)">
              <input
                type="text"
                inputMode="decimal"
                value={form.amount}
                onChange={(event) => update("amount", event.target.value)}
                className={inputClass}
              />
            </Field>

            <Field label="Payee account">
              <input
                type="text"
                value={form.payeeAccountId}
                onChange={(event) => update("payeeAccountId", event.target.value)}
                className={inputClass}
              />
            </Field>

            <Field label="Device">
              <input
                type="text"
                value={form.deviceId}
                placeholder="none supplied"
                onChange={(event) => update("deviceId", event.target.value)}
                className={inputClass}
              />
            </Field>

            <div className="grid grid-cols-2 gap-3">
              <Field label="Channel">
                <select
                  value={form.channel}
                  onChange={(event) => update("channel", event.target.value as Channel)}
                  className={inputClass}
                >
                  {CHANNELS.map((channel) => (
                    <option key={channel} value={channel}>
                      {channel}
                    </option>
                  ))}
                </select>
              </Field>
              <Field label="Hour (UTC)">
                <input
                  type="number"
                  min={0}
                  max={23}
                  value={form.hourUtc}
                  onChange={(event) =>
                    update("hourUtc", Math.max(0, Math.min(23, Number(event.target.value) || 0)))
                  }
                  className={inputClass}
                />
              </Field>
            </div>
          </div>

          <p className="mt-5 border-t border-edge pt-4 text-xs leading-relaxed text-fg-dim">
            Payer <span className="font-mono text-fg-muted">{DEMO_PAYER}</span> is an established
            account: 640 days old, verified, typical amount 240.00 with a 60.00 standard
            deviation, active 07:00&ndash;22:00 UTC, known device{" "}
            <span className="font-mono text-fg-muted">dev-8f21c4</span>.
          </p>
        </div>

        {/* ---- output ---- */}
        <div className="p-5">
          {"error" in result ? (
            <p className="rounded border border-review/40 bg-review-wash px-4 py-3 text-sm text-review">
              {result.error}
            </p>
          ) : (
            <Result result={result} note={activePreset?.note} />
          )}
        </div>
      </div>
    </div>
  );
}

const inputClass =
  "w-full rounded border border-edge bg-ink px-3 py-2 font-mono text-sm text-fg outline-none transition-colors focus:border-accent-dim";

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label className="block">
      <span className="mb-1.5 block text-xs font-medium uppercase tracking-wide text-fg-dim">
        {label}
      </span>
      {children}
    </label>
  );
}

function Result({ result, note }: { result: EvaluationResult; note?: string }) {
  return (
    <div>
      <div className="flex flex-wrap items-baseline gap-x-6 gap-y-3">
        <DecisionPill decision={result.decision} large />
        <Metric label="Risk score" value={result.riskScore.toFixed(2)} />
        <Metric
          label="Latency"
          value={`${(result.totalLatencyMicros / 1000).toFixed(3)} ms`}
          hint="budget 50 ms"
        />
      </div>

      {note ? <p className="mt-4 text-sm leading-relaxed text-fg-muted">{note}</p> : null}

      <p className="mt-4 rounded border border-edge bg-ink p-3 text-sm leading-relaxed text-fg-muted">
        {result.explanation}
      </p>

      {/* Layers */}
      <h4 className="mb-2 mt-6 text-xs font-medium uppercase tracking-wide text-fg-dim">
        Layers
      </h4>
      <div className="space-y-1.5">
        {result.layerResults.map((layer) => {
          const meta = CONTROL_LAYERS[layer.layer];
          return (
            <div
              key={layer.layer}
              className="flex items-center gap-3 rounded border border-edge bg-ink px-3 py-2 text-sm"
            >
              <span className="font-mono text-2xs text-fg-dim">L{meta.number}</span>
              <span className="min-w-0 flex-1 truncate text-fg-muted">{meta.displayName}</span>
              <StatusTag status={layer.status} />
              <span className="tnum w-10 text-right font-mono text-xs text-fg">
                {layer.contribution.toFixed(2)}
              </span>
            </div>
          );
        })}
      </div>

      {/* Factors */}
      <h4 className="mb-2 mt-6 text-xs font-medium uppercase tracking-wide text-fg-dim">
        Risk factors
      </h4>
      <div className="space-y-1.5">
        {result.riskFactors.map((factor, index) => {
          const kind = REASON_CODES[factor.code as ReasonCode]?.kind ?? "detection";
          const fired = factor.contribution > 0;
          return (
            <div
              key={`${factor.code}-${index}`}
              className="rounded border border-edge bg-ink px-3 py-2"
            >
              <div className="flex items-center gap-3">
                <span
                  className={`font-mono text-2xs ${fired ? "text-accent" : "text-fg-dim"}`}
                >
                  {factor.code}
                </span>
                {!fired ? (
                  <span className="font-mono text-2xs uppercase tracking-wide text-fg-dim">
                    {kind}
                  </span>
                ) : null}
                <span className="tnum ml-auto font-mono text-xs text-fg">
                  {fired ? `+${factor.contribution.toFixed(2)}` : "—"}
                </span>
              </div>
              <p className="mt-1 text-xs leading-relaxed text-fg-muted">{factor.explanation}</p>
              <p className="mt-1 font-mono text-2xs text-fg-dim">
                {factor.ruleId} &middot; v{factor.ruleVersion}
              </p>
            </div>
          );
        })}
      </div>

      <p className="mt-5 border-t border-edge pt-4 text-xs leading-relaxed text-fg-dim">
        Evaluated in your browser by a TypeScript port of the engine. Its thresholds are
        generated at build time from the same{" "}
        <span className="font-mono">application-rules.yml</span> the Java service reads, so the
        two cannot drift apart. Framework version{" "}
        <span className="font-mono">{result.frameworkVersion}</span>.
      </p>
    </div>
  );
}

function Metric({ label, value, hint }: { label: string; value: string; hint?: string }) {
  return (
    <div>
      <p className="text-xs uppercase tracking-wide text-fg-dim">{label}</p>
      <p className="tnum mt-0.5 font-mono text-lg text-fg">
        {value}
        {hint ? <span className="ml-2 text-2xs text-fg-dim">{hint}</span> : null}
      </p>
    </div>
  );
}

const STATUS_STYLE: Record<string, string> = {
  EVALUATED: "text-fg-muted",
  NO_DATA: "text-fg-dim",
  DEGRADED: "text-review",
  TIMED_OUT: "text-review",
  SKIPPED: "text-fg-dim",
};

function StatusTag({ status }: { status: string }) {
  return (
    <span className={`font-mono text-2xs uppercase tracking-wide ${STATUS_STYLE[status]}`}>
      {status.replace("_", " ")}
    </span>
  );
}
