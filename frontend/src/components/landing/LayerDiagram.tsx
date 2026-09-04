import { SITE } from "@/lib/site";

/**
 * The five control layers, with the sync/async boundary as the primary visual fact.
 *
 * Built from markup rather than a drawn SVG so the layer names and budgets are
 * real text — selectable, translatable, and readable by a screen reader in
 * order. A picture of words would have looked the same and said nothing.
 */

interface Layer {
  number: number;
  name: string;
  question: string;
  budget?: string;
}

const IN_PATH: Layer[] = [
  {
    number: 1,
    name: "Identity & Account Posture",
    question: "Is this account, device and channel in a posture consistent with this payment?",
    budget: "5 ms",
  },
  {
    number: 2,
    name: "Real-Time Behavioral Scoring",
    question: "Does this payment deviate from this payer's own baseline?",
    budget: "25 ms",
  },
  {
    number: 3,
    name: "Counterparty & Network Signals",
    question: "What do we already know about where the money is going?",
    budget: "15 ms",
  },
];

const ASYNC: Layer[] = [
  {
    number: 4,
    name: "External Enrichment",
    question: "What can outside intelligence add — for next time?",
  },
  {
    number: 5,
    name: "Post-Settlement Analysis",
    question: "What patterns are only visible afterwards, across many transactions?",
  },
];

export function LayerDiagram() {
  return (
    <div className="grid gap-4 lg:grid-cols-2">
      <Band
        tone="in-path"
        label="Sync — in-path"
        budget="50 ms p99 total"
        caption="Deterministic. Pre-computed state only: no live database query during authorization. Every layer degrades explicitly rather than blocking."
        layers={IN_PATH}
      />
      <Band
        tone="async"
        label="Async — off-path"
        budget="no latency budget"
        caption="Free to be slow, free to fail, free to retry. Feeds future decisions by updating pre-computed state. Nothing here can delay a payment."
        layers={ASYNC}
      />

      <div className="lg:col-span-2">
        <div className="rounded-lg border border-accent-dim/40 bg-accent-wash/40 p-5">
          <p className="font-mono text-2xs uppercase tracking-[0.14em] text-accent">
            The loop that makes it work
          </p>
          <p className="mt-2 text-pretty text-sm leading-relaxed text-fg-muted">
            Layer 5 detects a pattern no single transaction reveals — a receiving account
            collecting from many unrelated payers, say — and writes it into pre-computed state.
            Layer 3 reads that in-path on the <em className="not-italic text-fg">next</em>{" "}
            payment, at the cost of one lookup. The expensive analysis never touches the payment
            path, and nothing is traded away except immediacy, which was never available for
            that class of detection anyway.
          </p>
        </div>
      </div>
    </div>
  );
}

function Band({
  tone,
  label,
  budget,
  caption,
  layers,
}: {
  tone: "in-path" | "async";
  label: string;
  budget: string;
  caption: string;
  layers: Layer[];
}) {
  const inPath = tone === "in-path";
  return (
    <div
      className={`rounded-lg border p-5 ${
        inPath ? "border-accent-dim/50 bg-ink-raised" : "border-edge bg-ink-raised"
      }`}
    >
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <p
          className={`font-mono text-2xs uppercase tracking-[0.14em] ${
            inPath ? "text-accent" : "text-fg-dim"
          }`}
        >
          {label}
        </p>
        <p className="font-mono text-2xs text-fg-dim">{budget}</p>
      </div>

      <ol className="mt-4 space-y-2">
        {layers.map((layer) => (
          <li
            key={layer.number}
            className="rounded border border-edge bg-ink px-4 py-3"
          >
            <div className="flex items-baseline gap-3">
              <span
                className={`font-mono text-2xs ${inPath ? "text-accent" : "text-fg-dim"}`}
                aria-hidden
              >
                L{layer.number}
              </span>
              <span className="flex-1 text-sm font-medium text-fg">
                <span className="sr-only">Layer {layer.number}: </span>
                {layer.name}
              </span>
              {layer.budget ? (
                <span className="tnum font-mono text-2xs text-fg-dim">{layer.budget}</span>
              ) : null}
            </div>
            <p className="mt-1.5 text-xs leading-relaxed text-fg-muted">{layer.question}</p>
          </li>
        ))}
      </ol>

      <p className="mt-4 text-xs leading-relaxed text-fg-dim">{caption}</p>
    </div>
  );
}

export function BudgetNote() {
  return (
    <p className="mt-6 text-xs leading-relaxed text-fg-dim">
      The 50 ms budget is a design target derived from published rail requirements, not a
      measurement. Pix rejects a payment that has not settled within 40 seconds on the SPI
      primary channel, and the rail itself consumes 4.6 s at its own P99 — a fraud decision has
      to be small enough to be irrelevant to that arithmetic. Benchmarks are reported separately,
      as reproducible output.{" "}
      <a href={`${SITE.githubUrl}/blob/main/docs/framework/latency-model.md`} className="text-accent hover:text-accent-strong" rel="noreferrer noopener">
        Latency model
      </a>
      .
    </p>
  );
}
