"use client";

import { forwardRef, useEffect, useRef } from "react";
import { Card, PathBadge } from "@/components/ui";
import { LAYERS } from "./content";
import { CheckIcon, ClockIcon, CrossIcon, LayerIcon } from "./icons";

/**
 * The transaction journey.
 *
 * A single rAF, scheduled only when the user actually scrolls and only while
 * the section is near the viewport, writes two custom properties onto the
 * section: --journey-progress (0 → 1) and --rail-height (px). Everything that
 * moves — the sphere, the rail fill, the platform lift, the markers — is CSS
 * derived from those, so a scroll frame costs two style writes and no layout.
 *
 * Stop positions are measured from the DOM rather than assumed to be evenly
 * spaced, because the blocks are not the same height. That is what makes the
 * sphere arrive at a marker exactly as its block reaches the middle of the
 * screen, instead of drifting past it.
 */

const STOP_COUNT = LAYERS.length + 2; // start + layers + decision

/** Distance from a stop's top edge to the centre of its marker, in pixels. */
const MARKER_OFFSET = 12;

export function Journey() {
  const sectionRef = useRef<HTMLDivElement | null>(null);
  const railRef = useRef<HTMLDivElement | null>(null);
  const stopRefs = useRef<Array<HTMLElement | null>>([]);

  useEffect(() => {
    const section = sectionRef.current;
    const rail = railRef.current;
    if (!section || !rail) return;

    let railTop = 0;
    let railHeight = 0;
    let fractions: number[] = [];
    let lastProgress = -1;
    let lastIndex = -1;
    let ticking = false;
    let attached = false;

    function measure() {
      const rect = rail!.getBoundingClientRect();
      railTop = rect.top + window.scrollY;
      railHeight = rect.height;
      section!.style.setProperty("--rail-height", `${railHeight}px`);

      // Measured against the stop's marker, not the middle of its block. The
      // blocks are tall and unequal, so using their centres would light a layer
      // long after the sphere had visibly passed its dot on the rail.
      fractions = stopRefs.current.map((node) => {
        if (!node || railHeight === 0) return 0;
        const nodeRect = node.getBoundingClientRect();
        const marker = nodeRect.top + MARKER_OFFSET + window.scrollY;
        return Math.min(1, Math.max(0, (marker - railTop) / railHeight));
      });
    }

    function apply() {
      ticking = false;
      if (railHeight === 0) return;

      const viewportAnchor = window.scrollY + window.innerHeight * 0.5;
      const progress = Math.min(1, Math.max(0, (viewportAnchor - railTop) / railHeight));

      // Sub-pixel changes are invisible and still cost a style invalidation.
      if (Math.abs(progress - lastProgress) > 0.0004) {
        section!.style.setProperty("--journey-progress", progress.toFixed(4));
        lastProgress = progress;
      }

      let index = 0;
      for (let i = 1; i < fractions.length; i += 1) {
        if (progress >= fractions[i]) index = i;
      }

      // Attributes are written only when the active stop actually changes, not
      // on every frame.
      if (index !== lastIndex) {
        lastIndex = index;
        stopRefs.current.forEach((node, i) => {
          if (!node) return;
          node.dataset.state = i === index ? "active" : i < index ? "done" : "pending";
        });
      }
    }

    function schedule() {
      if (ticking) return;
      ticking = true;
      requestAnimationFrame(apply);
    }

    function attach() {
      if (attached) return;
      attached = true;
      window.addEventListener("scroll", schedule, { passive: true });
    }

    function detach() {
      if (!attached) return;
      attached = false;
      window.removeEventListener("scroll", schedule);
    }

    function remeasure() {
      measure();
      lastProgress = -1;
      lastIndex = -1;
      schedule();
    }

    remeasure();

    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          attach();
          schedule();
        } else {
          detach();
        }
      },
      { rootMargin: "25% 0px" },
    );
    observer.observe(section);

    const resizeObserver = new ResizeObserver(remeasure);
    resizeObserver.observe(section);

    // Fonts landing after hydration change block heights, which moves every
    // stop. Without this the markers sit slightly off until the first resize.
    if (document.fonts?.ready) {
      document.fonts.ready.then(remeasure).catch(() => {});
    }

    return () => {
      detach();
      observer.disconnect();
      resizeObserver.disconnect();
    };
  }, []);

  const registerStop = (index: number) => (node: HTMLElement | null) => {
    stopRefs.current[index] = node;
  };

  return (
    <div ref={sectionRef} className="journey relative scroll-mt-20 px-6 pb-24" id="architecture">
      <div className="relative mx-auto w-full max-w-content">
        {/* The rail. Absolutely positioned so its height is driven by the
            content beside it rather than the other way round. */}
        <div
          ref={railRef}
          aria-hidden
          className="pointer-events-none absolute bottom-6 left-5 top-6 w-px sm:left-6"
        >
          <div className="absolute inset-0 bg-edge-strong" />
          <div className="journey-rail-fill absolute inset-0 bg-gradient-to-b from-accent/70 to-accent" />

          {/* The transaction itself. */}
          <div className="journey-sphere absolute left-1/2 top-0 h-0 w-0">
            <div className="journey-sphere-trail absolute -top-24 left-1/2 h-24 w-px -translate-x-1/2" />
            <div className="journey-sphere-orbit absolute left-1/2 top-1/2 h-9 w-9 -translate-x-1/2 -translate-y-1/2 rounded-full border border-dashed border-accent/30" />
            <div className="journey-sphere-core absolute left-1/2 top-1/2 h-[18px] w-[18px] -translate-x-1/2 -translate-y-1/2 rounded-full" />
          </div>
        </div>

        <ol className="relative space-y-20 pl-12 sm:space-y-24 sm:pl-20">
          <Stop
            ref={registerStop(0)}
            marker="pulse"
            eyebrow="Transaction start"
            eyebrowClassName="text-accent"
          >
            <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_minmax(0,20rem)] lg:items-start lg:gap-10">
              <p className="max-w-md text-pretty text-sm leading-relaxed text-fg-muted">
                A payment is initiated and received by IPRF in milliseconds.
              </p>
              <Card className="p-4 font-mono text-2xs leading-relaxed text-fg-muted">
                <pre className="overflow-x-auto">
                  <code>{`{
  "amount": 125.00,
  "currency": "USD",
  "from": "user_123",
  "to": "user_987"
}`}</code>
                </pre>
              </Card>
            </div>
          </Stop>

          {LAYERS.map((layer, index) => (
            <Stop
              key={layer.id}
              ref={registerStop(index + 1)}
              id={layer.id}
              marker="dot"
              eyebrow={layer.label}
            >
              <div className="grid gap-8 lg:grid-cols-[minmax(0,1fr)_auto_minmax(0,17rem)] lg:items-center lg:gap-10">
                <div className="max-w-sm">
                  <h3 className="text-lg font-semibold tracking-tight text-fg sm:text-xl">
                    {layer.title}
                  </h3>
                  <p className="mt-3 text-pretty text-sm leading-relaxed text-fg-muted">
                    {layer.description}
                  </p>
                  <div className="mt-5 flex flex-wrap items-center gap-2">
                    <PathBadge path={layer.path} />
                    <span className="tnum rounded bg-ink-high px-2 py-1 font-mono text-2xs uppercase tracking-[0.12em] text-fg-muted ring-1 ring-inset ring-edge">
                      {layer.budget}
                    </span>
                  </div>
                </div>

                <LayerPlatform layer={layer} />

                <Card className="p-5">
                  <ul className="space-y-3">
                    {layer.items.map((item) => (
                      <li key={item} className="flex items-start gap-2.5 text-sm text-fg-muted">
                        <CheckIcon
                          className={`mt-0.5 h-3.5 w-3.5 shrink-0 ${
                            layer.path === "IN_PATH" ? "text-accent" : "text-fg-dim"
                          }`}
                        />
                        <span>{item}</span>
                      </li>
                    ))}
                  </ul>
                </Card>
              </div>
            </Stop>
          ))}

          <Stop
            ref={registerStop(STOP_COUNT - 1)}
            marker="pulse"
            eyebrow="Decision"
            eyebrowClassName="text-accent"
          >
            <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_minmax(0,22rem)] lg:items-center lg:gap-10">
              <p className="max-w-md text-pretty text-sm leading-relaxed text-fg-muted">
                The transaction is allowed, sent for review, or declined — with a clear,
                explainable reason.
              </p>
              <DecisionCard />
            </div>
          </Stop>
        </ol>
      </div>
    </div>
  );
}

/* -------------------------------------------------------------------------- */

interface StopProps {
  id?: string;
  marker: "dot" | "pulse";
  eyebrow: string;
  eyebrowClassName?: string;
  children: React.ReactNode;
}

const Stop = forwardRef<HTMLLIElement, StopProps>(function Stop(
  { id, marker, eyebrow, eyebrowClassName = "text-fg-dim", children },
  ref,
) {
  return (
    <li id={id} ref={ref} data-state="pending" className="layer-node relative scroll-mt-28">
      <span
        aria-hidden
        className={`layer-marker absolute top-1.5 -translate-x-1/2 rounded-full bg-edge-strong ${
          marker === "pulse"
            ? "-left-7 h-[18px] w-[18px] ring-1 ring-edge-strong sm:-left-14"
            : "-left-7 h-2.5 w-2.5 ring-4 ring-ink sm:-left-14"
        }`}
      />
      <p
        className={`font-mono text-2xs font-medium uppercase tracking-[0.22em] ${eyebrowClassName}`}
      >
        {eyebrow}
      </p>
      <div className="mt-4">{children}</div>
    </li>
  );
});

/**
 * The stacked platform for one layer.
 *
 * Three plates in a shared 3D context, tilted into an isometric view. IN-PATH
 * layers are lit; ASYNC layers stay neutral, which is the whole point of the
 * distinction — the reader should be able to see the sync boundary without
 * reading a word.
 */
function LayerPlatform({ layer }: { layer: (typeof LAYERS)[number] }) {
  const lit = layer.path === "IN_PATH";

  return (
    <div
      aria-hidden
      className="layer-visual relative mx-auto h-[124px] w-[190px] shrink-0 sm:h-[140px] sm:w-[220px]"
      style={{ perspective: "760px" }}
    >
      <div
        className="absolute inset-0"
        style={{ transformStyle: "preserve-3d", transform: "rotateX(58deg) rotateZ(45deg)" }}
      >
        {[0, 1, 2].map((depth) => (
          <div
            key={depth}
            className={`layer-plate absolute left-1/2 top-1/2 h-[104px] w-[104px] -translate-x-1/2 -translate-y-1/2 rounded-[14px] border ${
              lit
                ? depth === 0
                  ? "layer-plate-lit border-accent/50 bg-accent/[0.14]"
                  : "border-accent/25 bg-accent/[0.06]"
                : depth === 0
                  ? "border-edge-strong bg-fg/[0.06]"
                  : "border-edge bg-fg/[0.025]"
            }`}
            style={{
              transform: `translateZ(${(2 - depth) * 16}px)`,
              opacity: 1 - depth * 0.26,
            }}
          />
        ))}
      </div>

      <div className="absolute inset-0 flex items-center justify-center">
        <span
          className={`flex h-14 w-14 items-center justify-center rounded-full border backdrop-blur-sm ${
            lit
              ? "border-accent/45 bg-accent/15 text-accent"
              : "border-edge-strong bg-ink-high/80 text-fg-muted"
          }`}
        >
          <LayerIcon name={layer.icon} className="h-6 w-6" />
        </span>
      </div>
    </div>
  );
}

/**
 * The three outcomes, shown together.
 *
 * They share a card precisely so the trio reads as a set. Each carries its own
 * icon, which is what keeps them distinguishable now that the brand accent and
 * ALLOW occupy the same hue.
 */
function DecisionCard() {
  return (
    <Card className="border-accent-dim/40 bg-accent-wash/40 p-5">
      <ul className="flex flex-wrap items-center gap-x-5 gap-y-3">
        <li className="flex items-center gap-2 font-mono text-sm font-semibold uppercase tracking-[0.1em] text-allow">
          <CheckIcon className="h-4 w-4" />
          Allow
        </li>
        <li className="flex items-center gap-2 font-mono text-sm font-semibold uppercase tracking-[0.1em] text-review">
          <ClockIcon className="h-4 w-4" />
          Review
        </li>
        <li className="flex items-center gap-2 font-mono text-sm font-semibold uppercase tracking-[0.1em] text-decline">
          <CrossIcon className="h-4 w-4" />
          Decline
        </li>
      </ul>
      <p className="mt-4 border-t border-edge pt-4 text-sm text-fg-muted">
        Fast. Explainable. Auditable.
      </p>
    </Card>
  );
}
