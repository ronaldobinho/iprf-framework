import type { ReactNode } from "react";
import type { Decision } from "@/simulator/types";

export function Section({
  id,
  eyebrow,
  title,
  lede,
  children,
  bordered = true,
}: {
  id?: string;
  eyebrow?: string;
  title?: string;
  lede?: ReactNode;
  children: ReactNode;
  bordered?: boolean;
}) {
  return (
    <section
      id={id}
      className={`px-6 py-20 sm:py-24 ${bordered ? "border-t border-edge" : ""}`}
    >
      <div className="mx-auto w-full max-w-content">
        {eyebrow ? (
          <p className="mb-3 font-mono text-2xs uppercase tracking-[0.18em] text-accent">
            {eyebrow}
          </p>
        ) : null}
        {title ? (
          <h2 className="max-w-prose text-balance text-2xl font-semibold tracking-tight sm:text-3xl">
            {title}
          </h2>
        ) : null}
        {lede ? (
          <div className="mt-4 max-w-prose text-pretty text-base leading-relaxed text-fg-muted">
            {lede}
          </div>
        ) : null}
        <div className={title || lede ? "mt-10" : ""}>{children}</div>
      </div>
    </section>
  );
}

/**
 * Every surface showing a figure carries this. The project's honesty rules
 * require it, and on a page whose purpose is credibility it is an asset rather
 * than a disclaimer: it says the numbers are what they are.
 */
export function SyntheticBadge({ className = "" }: { className?: string }) {
  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded border border-edge-strong bg-ink-high px-2 py-1 font-mono text-2xs uppercase tracking-[0.12em] text-fg-muted ${className}`}
    >
      <span aria-hidden className="h-1.5 w-1.5 rounded-full bg-fg-dim" />
      Synthetic data
    </span>
  );
}

const DECISION_STYLE: Record<Decision, string> = {
  ALLOW: "border-allow/40 bg-allow-wash text-allow",
  REVIEW: "border-review/40 bg-review-wash text-review",
  DECLINE: "border-decline/40 bg-decline-wash text-decline",
};

export function DecisionPill({ decision, large = false }: { decision: Decision; large?: boolean }) {
  return (
    <span
      className={`inline-flex items-center rounded border font-mono font-semibold uppercase tracking-[0.12em] ${
        DECISION_STYLE[decision]
      } ${large ? "px-3 py-1.5 text-sm" : "px-2 py-1 text-2xs"}`}
    >
      {decision}
    </span>
  );
}

export function Card({ children, className = "" }: { children: ReactNode; className?: string }) {
  return (
    <div className={`rounded-lg border border-edge bg-ink-raised p-6 ${className}`}>
      {children}
    </div>
  );
}

export function InPathTag({ path }: { path: "IN_PATH" | "ASYNC" }) {
  const inPath = path === "IN_PATH";
  return (
    <span
      className={`inline-flex items-center rounded px-1.5 py-0.5 font-mono text-2xs uppercase tracking-[0.1em] ${
        inPath ? "bg-accent-wash text-accent" : "bg-ink-high text-fg-dim"
      }`}
    >
      {inPath ? "in-path" : "async"}
    </span>
  );
}
