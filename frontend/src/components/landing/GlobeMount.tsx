"use client";

import dynamic from "next/dynamic";

/**
 * Keeps the globe — its canvas logic and its 2,462-point coordinate table —
 * out of the initial bundle. The hero's text and calls to action must paint
 * without waiting for decoration to download, so the planet arrives in its own
 * chunk after hydration, against a placeholder sized identically to it.
 */
const Globe = dynamic(() => import("./Globe").then((m) => m.Globe), {
  ssr: false,
  loading: () => (
    <div className="relative aspect-square w-full" aria-hidden>
      <div className="globe-halo absolute inset-[-14%] rounded-full" />
      <div className="absolute inset-[8%] rounded-full border border-accent-dim/25 bg-ink-raised/40" />
    </div>
  ),
});

export function GlobeMount() {
  return <Globe />;
}
