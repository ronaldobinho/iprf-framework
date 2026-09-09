import Link from "next/link";
import type { AnchorHTMLAttributes, ReactNode } from "react";
import type { LayerPath, MetricBadge } from "@/components/landing/content";

/** The three outcomes the decision engine can return. */
export type Decision = "ALLOW" | "REVIEW" | "DECLINE";

export function Section({
  id,
  className = "",
  children,
  bordered = false,
}: {
  id?: string;
  className?: string;
  children: ReactNode;
  bordered?: boolean;
}) {
  return (
    <section
      id={id}
      className={`relative scroll-mt-20 px-6 py-20 sm:py-28 ${bordered ? "border-t border-edge" : ""} ${className}`}
    >
      <div className="mx-auto w-full max-w-content">{children}</div>
    </section>
  );
}

export function Eyebrow({ children, className = "" }: { children: ReactNode; className?: string }) {
  return (
    <p
      className={`font-mono text-2xs uppercase tracking-[0.22em] text-fg-muted ${className}`}
    >
      {children}
    </p>
  );
}

type ButtonProps = {
  href: string;
  children: ReactNode;
  variant?: "primary" | "secondary";
  external?: boolean;
  className?: string;
} & Omit<AnchorHTMLAttributes<HTMLAnchorElement>, "href" | "children" | "className">;

/**
 * One button component with two skins. The primary is the only solid green
 * surface on the page — that scarcity is what makes it read as the action.
 */
export function Button({
  href,
  children,
  variant = "primary",
  external = false,
  className = "",
  ...rest
}: ButtonProps) {
  const base =
    "inline-flex items-center justify-center gap-2 rounded-lg px-5 py-3 text-sm font-semibold tracking-tight transition-colors";
  const skin =
    variant === "primary"
      ? "bg-accent text-ink hover:bg-accent-strong"
      : "border border-edge-strong bg-ink-raised/70 text-fg hover:border-accent-dim hover:bg-ink-high";

  const classes = `${base} ${skin} ${className}`;

  if (external) {
    return (
      <a href={href} rel="noreferrer noopener" className={classes} {...rest}>
        {children}
      </a>
    );
  }
  return (
    <Link href={href} className={classes} {...rest}>
      {children}
    </Link>
  );
}

/**
 * Provenance badges.
 *
 * These exist because the project's honesty rules require every figure to say
 * what kind of claim it is. TARGET is a design budget, not a measurement. FACT
 * is a property of the source that a reader can check by cloning it. Nothing
 * may claim a measurement until backend/benchmarks produces one.
 */
const BADGE_STYLE: Record<MetricBadge, string> = {
  TARGET: "border-edge-strong bg-ink-high text-fg-muted",
  FACT: "border-edge-strong bg-ink-high text-fg-muted",
  "OPEN CORE": "border-accent-dim/60 bg-accent-wash text-accent",
};

export function ProvenanceBadge({ badge }: { badge: MetricBadge }) {
  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded border px-2 py-1 font-mono text-2xs uppercase tracking-[0.12em] ${BADGE_STYLE[badge]}`}
    >
      <span
        aria-hidden
        className={`h-1.5 w-1.5 rounded-full ${badge === "OPEN CORE" ? "bg-accent" : "bg-fg-dim"}`}
      />
      {badge}
    </span>
  );
}

export function DemoDataNote({ className = "" }: { className?: string }) {
  return (
    <p className={`font-mono text-2xs uppercase tracking-[0.14em] text-fg-dim ${className}`}>
      Synthetic / demo data — illustrative payload, not a recorded transaction
    </p>
  );
}

export function PathBadge({ path }: { path: LayerPath }) {
  const inPath = path === "IN_PATH";
  return (
    <span
      className={`inline-flex items-center rounded px-2 py-1 font-mono text-2xs font-medium uppercase tracking-[0.12em] ${
        inPath
          ? "bg-accent-wash text-accent ring-1 ring-inset ring-accent-dim/50"
          : "bg-ink-high text-fg-dim ring-1 ring-inset ring-edge-strong"
      }`}
    >
      {inPath ? "in-path" : "async"}
    </span>
  );
}

export function Card({ children, className = "" }: { children: ReactNode; className?: string }) {
  return (
    <div
      className={`rounded-xl border border-edge bg-ink-raised/70 p-6 backdrop-blur-sm ${className}`}
    >
      {children}
    </div>
  );
}
