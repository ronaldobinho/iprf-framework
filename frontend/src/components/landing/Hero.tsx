import { Button, Eyebrow } from "@/components/ui";
import { SITE } from "@/lib/site";
import { HERO_CREDENTIALS, HERO_PILLARS } from "./content";
import { GlobeMount } from "./GlobeMount";
import { ArrowRightIcon, CheckIcon, GitHubIcon, LayersIcon } from "./icons";

const CREDENTIAL_ICONS = [CheckIcon, LayersIcon, CheckIcon] as const;

export function Hero() {
  return (
    <section className="relative overflow-hidden px-6 pb-16 pt-12 sm:pb-20 sm:pt-16">
      {/* Decorative ground: the technical grid, faded out before it reaches any
          edge so it never terminates on a visible line. */}
      <div
        aria-hidden
        className="tech-grid mask-fade-y pointer-events-none absolute inset-0 opacity-70"
      />

      <div className="relative mx-auto grid w-full max-w-content items-center gap-12 lg:grid-cols-[minmax(0,1fr)_minmax(0,1.05fr)] lg:gap-8">
        <div className="max-w-xl">
          <Eyebrow>Instant payments. Higher trust.</Eyebrow>

          <h1 className="mt-5 text-balance text-4xl font-semibold leading-[1.05] tracking-tight sm:text-5xl lg:text-[3.4rem]">
            <span className="block text-fg">Faster payments</span>
            <span className="block text-accent">safely, by design.</span>
          </h1>

          <p className="mt-6 max-w-lg text-pretty text-base leading-relaxed text-fg sm:text-lg">
            IPRF is an open-source framework for fraud prevention and operational resilience in
            instant payment systems.
          </p>

          <p className="mt-4 max-w-lg text-pretty text-sm leading-relaxed text-fg-muted sm:text-base">
            Modular, transparent, and built for low latency, IPRF helps financial institutions
            evaluate risk in real time — so legitimate payments flow, and fraud is stopped before
            it happens.
          </p>

          <div className="mt-9 flex flex-wrap items-center gap-3">
            <Button href="#how-it-works">
              Explore the Framework
              <ArrowRightIcon className="h-4 w-4" />
            </Button>
            <Button href={SITE.githubUrl} variant="secondary" external>
              <GitHubIcon className="h-4 w-4" />
              View on GitHub
            </Button>
          </div>

          <ul className="mt-10 flex flex-wrap items-center gap-x-7 gap-y-3">
            {HERO_CREDENTIALS.map((credential, index) => {
              const Icon = CREDENTIAL_ICONS[index];
              return (
                <li key={credential} className="flex items-center gap-2 text-sm text-fg-muted">
                  <Icon className="h-4 w-4 shrink-0 text-accent" />
                  {credential}
                </li>
              );
            })}
          </ul>
        </div>

        <div className="relative xl:pr-28">
          <div className="mx-auto w-full max-w-[34rem] lg:max-w-none">
            <GlobeMount />
          </div>

          <p
            className="pointer-events-none absolute right-0 top-1/2 hidden max-w-[7rem] -translate-y-1/2 font-mono text-2xs uppercase leading-[1.9] tracking-[0.18em] text-fg-dim xl:block"
            aria-hidden
          >
            A safer
            <br />
            payments
            <br />
            ecosystem
            <br />
            for a brighter
            <br />
            tomorrow
          </p>
        </div>
      </div>

      <div className="relative mx-auto mt-14 w-full max-w-content border-t border-edge pt-8">
        <ul className="grid grid-cols-2 gap-6 sm:grid-cols-4">
          {HERO_PILLARS.map((pillar) => (
            <li
              key={pillar}
              className="border-l border-accent-dim/40 pl-4 font-mono text-2xs uppercase leading-relaxed tracking-[0.16em] text-accent"
            >
              {pillar}
            </li>
          ))}
        </ul>
      </div>
    </section>
  );
}
