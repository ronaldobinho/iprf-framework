import { Button, Card, DemoDataNote, Eyebrow, ProvenanceBadge, Section } from "@/components/ui";
import { SITE } from "@/lib/site";
import { METRICS } from "./content";
import { ArrowRightIcon, BookIcon, GitHubIcon, MetricIcon } from "./icons";

/**
 * The invitation to scroll.
 *
 * It states what the scroll will do rather than just pointing downward, because
 * the journey below is the page's argument, not an ornament — a reader who
 * skips it has skipped the framework.
 */
export function ScrollInvitation() {
  return (
    <div className="relative flex flex-col items-center gap-5 px-6 py-16 sm:py-20">
      <Eyebrow className="text-center text-fg-dim">Scroll to follow a transaction</Eyebrow>
      <span
        aria-hidden
        className="flex h-9 w-[22px] items-start justify-center rounded-full border border-edge-strong pt-2"
      >
        <span className="h-1.5 w-1 rounded-full bg-accent" />
      </span>
    </div>
  );
}

export function JourneyIntro() {
  return (
    <Section id="how-it-works" className="pb-8 pt-4 sm:pb-10 sm:pt-6">
      <div className="grid gap-8 lg:grid-cols-[minmax(0,1.1fr)_minmax(0,1fr)] lg:items-end lg:gap-16">
        <h2 className="text-balance text-3xl font-semibold leading-[1.12] tracking-tight sm:text-4xl">
          <span className="block text-fg">Follow a transaction</span>
          <span className="block text-fg">through multiple layers</span>
          <span className="block text-accent">of protection.</span>
        </h2>
        <p className="max-w-prose text-pretty text-sm leading-relaxed text-fg-muted sm:text-base">
          From identity to post-settlement analysis, IPRF combines deterministic rules,
          pre-computed risk intelligence and asynchronous enrichment across multiple layers —
          without slowing down legitimate payments.
        </p>
      </div>
    </Section>
  );
}

/**
 * Performance.
 *
 * Every figure carries a provenance badge, and none of them claims a
 * measurement: the benchmark suite in backend/benchmarks has not been run, so
 * there is nothing here to report as a result. A latency budget is a target, a
 * layer count is a fact, and both say which they are.
 */
export function Performance() {
  return (
    <Section id="performance" bordered className="bg-ink-raised/30">
      <div
        aria-hidden
        className="tech-grid mask-fade-y pointer-events-none absolute inset-0 opacity-50"
      />
      <div className="relative">
        <h2 className="mx-auto max-w-2xl text-balance text-center text-2xl font-semibold tracking-tight sm:text-3xl">
          Built for performance. Trusted for what matters.
        </h2>

        <ul className="mt-12 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {METRICS.map((metric) => (
            <li key={metric.label}>
              <Card className="flex h-full flex-col items-start gap-4">
                <MetricIcon name={metric.icon} className="h-5 w-5 text-accent" />
                <div className="flex-1">
                  <p className="tnum text-2xl font-semibold tracking-tight text-fg">
                    {metric.value}
                  </p>
                  <p className="mt-1.5 text-sm leading-relaxed text-fg-muted">{metric.label}</p>
                </div>
                <ProvenanceBadge badge={metric.badge} />
              </Card>
            </li>
          ))}
        </ul>

        <p className="mt-8 text-center text-2xs leading-relaxed text-fg-dim">
          <span className="font-mono uppercase tracking-[0.14em]">Target</span> is a design
          budget the framework is built to.{" "}
          <span className="font-mono uppercase tracking-[0.14em]">Fact</span> is a property of
          the source you can verify by cloning it. No figure on this page is a benchmark result —
          the suite has not been run.
        </p>
      </div>
    </Section>
  );
}

/**
 * Integration.
 *
 * The pitch sits over the photograph, as in the reference. The contract sits
 * directly beneath it, because an engineer evaluating this will want the shape
 * of the request before they want the invitation.
 */
export function Integration() {
  return (
    <Section id="integrate" bordered className="overflow-hidden !py-0">
      <div className="relative -mx-6 px-6">
        {/* The photograph bleeds from the right and is masked back to near-solid
            on the left, so the copy always sits on a readable ground.

            public/integrate-bg.png is currently a 224x179 crop taken from the
            reference image, which is all the photographic pixels that image
            contained. At this size it is upscaled heavily, so it is blurred a
            touch deliberately and pushed well back behind the gradients — it
            reads as atmosphere rather than as a photograph. Dropping a real
            high-resolution file at that path is the only change needed. */}
        {/* Pulled out to the viewport width: inside the content container the
            band stops at the container edge and leaves a visible seam. */}
        <div
          aria-hidden
          className="pointer-events-none absolute inset-y-0 left-1/2 w-screen -translate-x-1/2 overflow-hidden"
        >
          <div className="absolute inset-y-0 right-0 w-full overflow-hidden sm:w-[64%]">
            <div
              className="absolute inset-0 scale-110 bg-cover bg-no-repeat opacity-90 blur-[2px] [background-position:64%_center]"
              style={{ backgroundImage: "url(/integrate-bg.png)" }}
            />
          </div>
          <div className="absolute inset-0 bg-gradient-to-r from-ink via-ink/85 to-ink/30" />
          <div className="absolute inset-0 bg-gradient-to-t from-ink via-transparent to-ink/75" />
        </div>

        <div className="relative grid gap-10 py-20 sm:py-24 lg:grid-cols-[minmax(0,1fr)_auto] lg:items-center">
          <div className="max-w-xl">
            <Eyebrow>Open source. Real impact.</Eyebrow>
            <h2 className="mt-5 text-balance text-3xl font-semibold tracking-tight sm:text-4xl">
              Integrate. Adapt. Scale.
            </h2>
            <p className="mt-5 max-w-md text-pretty text-sm leading-relaxed text-fg-muted sm:text-base">
              Get the code, explore the documentation and see how IPRF can fit into your payment
              infrastructure.
            </p>
            <div className="mt-8 flex flex-wrap items-center gap-3">
              <Button href={SITE.githubUrl} external>
                <GitHubIcon className="h-4 w-4" />
                Explore on GitHub
                <ArrowRightIcon className="h-4 w-4" />
              </Button>
              <Button href="/methodology" variant="secondary">
                <BookIcon className="h-4 w-4" />
                Read the Documentation
              </Button>
            </div>
          </div>

          <p
            aria-hidden
            className="hidden font-mono text-2xs uppercase leading-[2] tracking-[0.2em] text-fg-muted lg:block"
          >
            Safer
            <br />
            payments
            <br />
            stronger
            <br />
            societies
          </p>
        </div>
      </div>

      <div className="relative border-t border-edge py-16 sm:py-20">
        <div className="grid gap-6 lg:grid-cols-2 lg:gap-8">
          <ApiPanel
            title="Request"
            method="POST"
            path="/api/v1/transactions/evaluate"
            body={`{
  "transactionId": "txn_123",
  "amount": 125.00,
  "currency": "USD",
  "from": "user_123",
  "to": "user_987"
}`}
          />
          <ApiPanel
            title="Response"
            status="200 OK"
            body={`{
  "decision": "ALLOW",
  "riskScore": 12,
  "latencyMs": 8
}`}
          />
        </div>
        <DemoDataNote className="mt-6" />
      </div>
    </Section>
  );
}

function ApiPanel({
  title,
  method,
  path,
  status,
  body,
}: {
  title: string;
  method?: string;
  path?: string;
  status?: string;
  body: string;
}) {
  return (
    <div className="overflow-hidden rounded-xl border border-edge bg-ink-raised/80 backdrop-blur-sm">
      <div className="flex items-center gap-3 border-b border-edge px-4 py-3">
        <span className="font-mono text-2xs uppercase tracking-[0.16em] text-fg-dim">
          {title}
        </span>
        {method ? (
          <span className="rounded bg-accent-wash px-1.5 py-0.5 font-mono text-2xs font-semibold text-accent">
            {method}
          </span>
        ) : null}
        {path ? <span className="truncate font-mono text-2xs text-fg-muted">{path}</span> : null}
        {status ? (
          <span className="font-mono text-2xs text-accent">{status}</span>
        ) : null}
      </div>
      <pre className="overflow-x-auto px-4 py-4 font-mono text-xs leading-relaxed text-fg-muted">
        <code>{body}</code>
      </pre>
    </div>
  );
}

export function FinalCta() {
  return (
    <Section bordered className="text-center">
      <div
        aria-hidden
        className="pointer-events-none absolute inset-x-0 top-0 h-64 bg-[radial-gradient(ellipse_50rem_18rem_at_50%_0%,rgba(32,224,124,0.12),transparent_70%)]"
      />
      <div className="relative mx-auto max-w-2xl">
        <h2 className="text-balance text-3xl font-semibold tracking-tight sm:text-4xl">
          <span className="text-fg">Safer payments. </span>
          <span className="text-accent">Stronger systems.</span>
        </h2>
        <p className="mx-auto mt-5 max-w-lg text-pretty text-sm leading-relaxed text-fg-muted sm:text-base">
          IPRF is open source and free to adopt, adapt and audit. If you are assessing fraud
          controls on an instant-payment rail, start with the methodology.
        </p>
        <div className="mt-9 flex flex-wrap items-center justify-center gap-3">
          <Button href={`mailto:${SITE.contactEmail}`} external>
            Get in Touch
            <ArrowRightIcon className="h-4 w-4" />
          </Button>
          <Button href={SITE.githubUrl} variant="secondary" external>
            <GitHubIcon className="h-4 w-4" />
            Explore GitHub
          </Button>
        </div>
      </div>
    </Section>
  );
}
