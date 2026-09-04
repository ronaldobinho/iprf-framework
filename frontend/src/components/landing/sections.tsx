import Link from "next/link";
import { Card, Section, SyntheticBadge } from "@/components/ui";
import { SITE, mailtoAssessment } from "@/lib/site";
import type { FrameworkDoc } from "@/lib/docs";

/* ------------------------------------------------------------------ hero */

export function Hero() {
  return (
    <section className="px-6 pb-16 pt-20 sm:pb-20 sm:pt-28">
      <div className="mx-auto w-full max-w-content">
        <p className="font-mono text-2xs uppercase tracking-[0.18em] text-accent">
          Open source &middot; {SITE.license} &middot; FedNow, Pix and similar rails
        </p>
        <h1 className="mt-5 max-w-3xl text-balance text-4xl font-semibold leading-[1.08] tracking-tight sm:text-5xl lg:text-6xl">
          {SITE.tagline}
        </h1>
        <p className="mt-6 max-w-2xl text-pretty text-lg leading-relaxed text-fg-muted">
          A payment that settles in seconds and cannot be recalled does not give you the
          investigation window every legacy fraud control was built around. IPRF is an
          assessment framework and a working reference engine for deciding, before the
          transaction arrives, what can be evaluated in-path and what cannot.
        </p>

        <div className="mt-9 flex flex-wrap items-center gap-3">
          <a
            href="#demo"
            className="rounded bg-accent px-5 py-2.5 text-sm font-medium text-ink transition-colors hover:bg-accent-strong"
          >
            Run a demo evaluation
          </a>
          <Link
            href="/methodology"
            className="rounded border border-edge-strong px-5 py-2.5 text-sm font-medium text-fg transition-colors hover:border-accent-dim"
          >
            Read the methodology
          </Link>
          <a
            href={SITE.githubUrl}
            rel="noreferrer noopener"
            className="px-2 py-2.5 text-sm text-fg-muted transition-colors hover:text-fg"
          >
            Source on GitHub
          </a>
        </div>

        <dl className="mt-14 grid gap-x-8 gap-y-6 border-t border-edge pt-8 sm:grid-cols-3">
          <Stat
            value="40 s"
            label="Pix settlement ceiling"
            note="A payment not settled on the SPI primary channel within the limit is rejected."
          />
          <Stat
            value="£576.4m"
            label="UK APP fraud losses, 2025"
            note="Up 19% year on year across 248,070 cases; 32% of all UK payment fraud losses."
          />
          <Stat
            value="83%"
            label="of APP cases start off-channel"
            note="66% online and 17% via telecoms — before the customer opens the banking app."
          />
        </dl>
      </div>
    </section>
  );
}

function Stat({ value, label, note }: { value: string; label: string; note: string }) {
  return (
    <div>
      <dt className="text-xs uppercase tracking-wide text-fg-dim">{label}</dt>
      <dd>
        <p className="tnum mt-1 font-mono text-2xl text-fg">{value}</p>
        <p className="mt-1.5 text-xs leading-relaxed text-fg-muted">{note}</p>
      </dd>
    </div>
  );
}

/* --------------------------------------------------------------- problem */

export function Problem() {
  return (
    <Section
      id="how-it-works"
      eyebrow="Why this is a different problem"
      title="Fraud controls assume there is time. On an instant rail, there is not."
      lede={
        <>
          Card and ACH controls were designed around reversibility: the system is allowed to be
          wrong on Tuesday because it can be corrected on Friday. Irrevocable instant payments
          remove that assumption, and the Federal Reserve says so in its own readiness guidance
          for participants.
        </>
      }
    >
      <figure className="max-w-prose border-l-2 border-accent-dim pl-5">
        <blockquote className="text-pretty text-base leading-relaxed text-fg">
          Systems designed to combat fraud involving payments that are cleared and settled in
          batches on predictable cycles may need updates to address fraud involving payments
          that clear and settle immediately.
        </blockquote>
        <figcaption className="mt-3 text-xs text-fg-dim">
          Federal Reserve Financial Services,{" "}
          <a
            href="https://explore.fednow.org/resources/fraud-at-a-glance.pdf"
            className="text-fg-muted underline decoration-edge-strong underline-offset-2 hover:text-accent"
            rel="noreferrer noopener"
          >
            FedNow Service Readiness Guide: Managing Fraud Risk
          </a>
        </figcaption>
      </figure>

      <div className="mt-12 grid gap-4 md:grid-cols-3">
        <Card>
          <h3 className="text-sm font-medium text-fg">The attack moved</h3>
          <p className="mt-2 text-sm leading-relaxed text-fg-muted">
            When stealing credentials stops paying, attackers stop attacking the credential and
            start attacking the customer. In authorised push payment fraud the victim issues the
            instruction themselves — correctly authenticated, genuinely intended.
            Authentication controls cannot see it, because nothing about the authentication is
            wrong.
          </p>
        </Card>
        <Card>
          <h3 className="text-sm font-medium text-fg">The receiving side now pays</h3>
          <p className="mt-2 text-sm leading-relaxed text-fg-muted">
            Since 7 October 2024 the UK requires reimbursement of APP scam victims up to
            £85,000, with the cost split equally between the sending and receiving firms. An
            institution is now financially liable for the mule accounts it hosts, which turns
            counterparty controls from a courtesy into a line item.
          </p>
        </Card>
        <Card>
          <h3 className="text-sm font-medium text-fg">The rails already split the path</h3>
          <p className="mt-2 text-sm leading-relaxed text-fg-muted">
            Pix grants up to 30 minutes to authorize a fraud-suspected payment during business
            hours, and 60 outside them. FedNow offers &ldquo;accept without posting&rdquo;. Both
            rails built an escape hatch from the real-time path — which is why this framework
            has three outcomes, not two.
          </p>
        </Card>
      </div>
    </Section>
  );
}

/* --------------------------------------------------------- what it refuses */

const REFUSALS = [
  {
    title: "No black-box scoring",
    body: "Every decision carries the rules that fired, their versions, their individual contributions and a readable explanation. A model that cannot explain a declined payment cannot be defended to the customer, the risk committee, or a supervisor. Machine learning is an extension point that produces features for explainable rules, never an unexplainable verdict.",
  },
  {
    title: "No thresholds in code",
    body: "Rule weights and decision boundaries live in configuration. Changing the amount at which a payment becomes suspicious is a risk decision, not a software release — and an institution that can only respond to an attack as fast as it can ship cannot respond.",
  },
  {
    title: "No live queries in-path",
    body: "The in-path layers read pre-computed state only. This is enforced by a build-time architecture test, not by convention: the rule is exactly the kind that erodes under deadline, so it is guarded by something that does not have one.",
  },
  {
    title: "No unlabelled metrics",
    body: "Every rate published here is computed against synthetic data with ground-truth labels, and says so. A detection rate quoted without its false-positive rate is not a result, it is a selected statistic.",
  },
];

export function Refusals() {
  return (
    <Section
      eyebrow="Constraints, not omissions"
      title="What this framework refuses to do"
      lede="These are the decisions that make the rest defensible. Each one costs something, and each one is deliberate."
    >
      <div className="grid gap-4 sm:grid-cols-2">
        {REFUSALS.map((item) => (
          <Card key={item.title}>
            <h3 className="text-sm font-medium text-fg">{item.title}</h3>
            <p className="mt-2 text-sm leading-relaxed text-fg-muted">{item.body}</p>
          </Card>
        ))}
      </div>
    </Section>
  );
}

/* ----------------------------------------------------- reproducible result */

export function ReproducibleResult() {
  return (
    <Section
      eyebrow="Reproducible output, not a claim"
      title="What the feedback loop is worth, measured"
      lede={
        <>
          The repository ships a seeded scenario that evaluates 1,000 synthetic transactions
          twice. Between the passes, settled payments are analysed by Layer 5, which writes what
          it finds into pre-computed state for Layer 3 to read. Nothing tells the engine which
          accounts are mules — the generator&rsquo;s labels only score the result.
        </>
      }
    >
      <div className="overflow-hidden rounded-lg border border-edge bg-ink-raised">
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-edge bg-ink-high px-5 py-3">
          <code className="font-mono text-xs text-fg-muted">./gradlew runScenario</code>
          <SyntheticBadge />
        </div>
        <div className="overflow-x-auto">
          <table className="w-full min-w-[36rem] text-sm">
            <thead>
              <tr className="border-b border-edge text-left">
                <th scope="col" className="px-5 py-3 font-medium text-fg-dim">
                  Metric
                </th>
                <th scope="col" className="px-5 py-3 text-right font-medium text-fg-dim">
                  Before Layer 5
                </th>
                <th scope="col" className="px-5 py-3 text-right font-medium text-fg-dim">
                  After
                </th>
                <th scope="col" className="px-5 py-3 text-right font-medium text-fg-dim">
                  Change
                </th>
              </tr>
            </thead>
            <tbody className="tnum font-mono">
              <Row label="Detection rate" before="72.97%" after="97.30%" delta="+24.32 pp" good />
              <Row label="False positive rate" before="3.13%" after="3.13%" delta="0.00 pp" />
              <Row label="Hard false positive rate" before="0.22%" after="0.22%" delta="0.00 pp" />
              <Row label="Precision" before="65.06%" after="71.29%" delta="+6.23 pp" good />
            </tbody>
          </table>
        </div>
        <p className="border-t border-edge px-5 py-4 text-xs leading-relaxed text-fg-dim">
          A detection rate that rises while the false-positive rate holds is the loop working. A
          rise in both would mean the detectors are simply flagging more counterparties, which
          is a different and far less useful result. These figures measure this rule set against
          a generated dataset with generator-assigned labels; they are reproducible from the
          seed in the repository and are <strong className="text-fg-muted">not</strong>{" "}
          production performance claims. Fraud prevalence in the generator is far above
          real-world rates so a run of this size yields meaningful counts.
        </p>
      </div>
    </Section>
  );
}

function Row({
  label,
  before,
  after,
  delta,
  good = false,
}: {
  label: string;
  before: string;
  after: string;
  delta: string;
  good?: boolean;
}) {
  return (
    <tr className="border-b border-edge last:border-0">
      <th scope="row" className="px-5 py-3 text-left font-sans font-normal text-fg-muted">
        {label}
      </th>
      <td className="px-5 py-3 text-right text-fg-muted">{before}</td>
      <td className="px-5 py-3 text-right text-fg">{after}</td>
      <td className={`px-5 py-3 text-right ${good ? "text-allow" : "text-fg-dim"}`}>{delta}</td>
    </tr>
  );
}

/* ------------------------------------------------------------- assessment */

const CATEGORIES = [
  ["Fraud Prevention", "Control coverage across the typologies"],
  ["Real-Time Decisioning", "Determinism, budgets, degradation, configurability"],
  ["Network / Counterparty Risk", "Pre-computed counterparty state and tiers"],
  ["External Intelligence", "Enrichment sources, isolation from the payment path"],
  ["Post-Settlement Analytics", "Typology detectors and the feedback loop"],
  ["False-Positive Management", "Whether FP is measured at all, and how labels are obtained"],
  ["Resilience", "Failure isolation and behaviour under partial outage"],
  ["Recovery", "Recovery-time measurement and permanence of remediation"],
  ["Scalability", "Whether cost grows with volume in ways that threaten the budget"],
  ["Auditability", "Whether a past decision can be reconstructed exactly"],
  ["Observability", "Latency percentiles, decision distribution, per-rule attribution"],
  ["Regulatory Readiness", "Settlement timing, reimbursement duties, fraud reporting"],
];

const LEVELS = [
  ["0", "Not Established", "The capability is absent."],
  ["1", "Ad Hoc", "It happens, driven by individuals rather than by design."],
  ["2", "Defined", "Documented, repeatable, owned. Not yet quantified."],
  ["3", "Measured", "Instrumented. Performance is known, not believed."],
  ["4", "Optimized", "Measurements drive a deliberate improvement loop."],
];

export function Assessment() {
  return (
    <Section
      id="assessment"
      eyebrow="The engagement"
      title="A maturity assessment of your instant-payment posture"
      lede={
        <>
          Twelve categories, scored 0 to 4 against evidence rather than intent, with findings
          ranked by severity and a recommendation for each. The output is a report you can act
          on and argue with — every score decomposes into the control answers that produced it,
          via a formula that is visible in configuration.
        </>
      }
    >
      <div className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_minmax(0,20rem)]">
        <Card>
          <h3 className="text-sm font-medium text-fg">The twelve categories</h3>
          <ul className="mt-4 grid gap-x-6 gap-y-3 sm:grid-cols-2">
            {CATEGORIES.map(([name, detail], index) => (
              <li key={name} className="flex gap-3">
                <span className="tnum mt-0.5 font-mono text-2xs text-fg-dim">
                  {String(index + 1).padStart(2, "0")}
                </span>
                <span>
                  <span className="block text-sm text-fg">{name}</span>
                  <span className="block text-xs leading-relaxed text-fg-muted">{detail}</span>
                </span>
              </li>
            ))}
          </ul>
        </Card>

        <Card>
          <h3 className="text-sm font-medium text-fg">Maturity levels</h3>
          <ol className="mt-4 space-y-3">
            {LEVELS.map(([level, name, detail]) => (
              <li key={level} className="flex gap-3">
                <span className="mt-0.5 font-mono text-2xs text-accent">{level}</span>
                <span>
                  <span className="block text-sm text-fg">{name}</span>
                  <span className="block text-xs leading-relaxed text-fg-muted">{detail}</span>
                </span>
              </li>
            ))}
          </ol>
          <p className="mt-4 border-t border-edge pt-4 text-xs leading-relaxed text-fg-dim">
            A level is capped by the evidence supporting it. An institution asserting excellence
            with no measurement scores as if the control were absent, because from the outside
            those two states are indistinguishable.
          </p>
        </Card>
      </div>

      <div className="mt-6 rounded-lg border border-edge bg-ink-raised p-6">
        <div className="flex flex-wrap items-center justify-between gap-5">
          <div className="max-w-xl">
            <h3 className="text-base font-medium text-fg">
              Run it yourself, or have it run with you
            </h3>
            <p className="mt-2 text-sm leading-relaxed text-fg-muted">
              The methodology and the engine are open source and self-executable — that is the
              point of them. If you would rather have the assessment carried out against your
              own stack, get in touch and describe what you are running.
            </p>
          </div>
          <a
            href={mailtoAssessment()}
            className="rounded bg-accent px-5 py-2.5 text-sm font-medium text-ink transition-colors hover:bg-accent-strong"
          >
            Enquire about an assessment
          </a>
        </div>
      </div>
    </Section>
  );
}

/* ------------------------------------------------------------ methodology */

export function MethodologyTeaser({ docs }: { docs: FrameworkDoc[] }) {
  return (
    <Section
      eyebrow="The specification"
      title="The methodology is the primary artifact"
      lede="The code demonstrates it; these documents are it. Claims are grounded in the public record and cited to primary sources — the Banco Central's Pix timing manual, the Federal Reserve's readiness guide, UK Finance and the Payment Systems Regulator."
    >
      <ul className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {docs.slice(0, 6).map((doc) => (
          <li key={doc.slug}>
            <Link
              href={`/methodology/${doc.slug}`}
              className="flex h-full flex-col rounded-lg border border-edge bg-ink-raised p-5 transition-colors hover:border-accent-dim"
            >
              <span className="text-sm font-medium text-fg">{doc.title}</span>
              <span className="mt-2 line-clamp-3 text-xs leading-relaxed text-fg-muted">
                {doc.summary}
              </span>
            </Link>
          </li>
        ))}
      </ul>
      <Link
        href="/methodology"
        className="mt-6 inline-block text-sm text-accent hover:text-accent-strong"
      >
        All {docs.length} documents &rarr;
      </Link>
    </Section>
  );
}

/* ---------------------------------------------------------------- contact */

export function Contact() {
  return (
    <Section eyebrow="Get in touch" title="Tell me what you are running">
      <div className="max-w-prose">
        <p className="text-pretty text-base leading-relaxed text-fg-muted">
          Which rail, whether you sit on the sending or receiving side, and what prompted you to
          look. If an assessment is not the right thing, I will say so.
        </p>
        <div className="mt-7 flex flex-wrap items-center gap-3">
          <a
            href={mailtoAssessment()}
            className="rounded bg-accent px-5 py-2.5 text-sm font-medium text-ink transition-colors hover:bg-accent-strong"
          >
            {SITE.contactEmail}
          </a>
          {SITE.linkedInUrl ? (
            <a
              href={SITE.linkedInUrl}
              rel="noreferrer noopener"
              className="rounded border border-edge-strong px-5 py-2.5 text-sm text-fg transition-colors hover:border-accent-dim"
            >
              LinkedIn
            </a>
          ) : null}
          <a
            href={SITE.githubUrl}
            rel="noreferrer noopener"
            className="rounded border border-edge-strong px-5 py-2.5 text-sm text-fg transition-colors hover:border-accent-dim"
          >
            Read the source first
          </a>
        </div>
      </div>
    </Section>
  );
}
