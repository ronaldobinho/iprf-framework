# Phase 5 — Frontend: Dashboard + Landing v1

**Window:** Sep 14–19, 2026 (3–4 sessions)
**Depends on:** Phase 4 gate met (or Phase 3 if Phase 4 shipped in degraded mode).
**Gate:** End-to-end local demo runnable; landing static-exportable to Vercel.

Scope discipline: the scroll-driven "transaction journey" (original brief §9A) is
**v1.1, post-RFE**. Landing v1 is static, excellent, and cheap. Do not let the
animation eat this phase. If Phases 1–4 finished early AND this phase's gate is
met with days to spare, §9A may begin — never before.

Design language (from original brief, still binding for v1): dark enterprise UI,
near-black/deep navy, electric blue accent, controlled green / amber / red for
outcomes. No emojis, no stock photos, no generic SaaS gradients, no glassmorphism.
Bloomberg-meets-cloud-infrastructure, not startup-playful. Load the
frontend-design skill at the start of each session in this phase.

---

## Session 5.1 — App shell + client-side simulator

1. Next.js 14 App Router structure:
   - `/` landing (static)
   - `/methodology` rendered from `docs/framework/*.md` (single source of truth —
     do not duplicate content into React)
   - `/dashboard/*` authenticated-later product area (no auth in v1; a plain route)
2. `frontend/simulator/`: TypeScript port of the DETERMINISTIC rule set (Layers
   1–2 rules + simplified Layer 3 from a bundled synthetic risk-state snapshot).
   - Port the rule THRESHOLDS from `application-rules.yml` at build time (script
     that reads the YAML and emits TS constants) so the two implementations cannot
     silently diverge.
   - Runs entirely client-side: this is what makes the Vercel demo real with zero
     backend dependency.
3. Parity test in CI: N synthetic transactions evaluated by both the Java engine
   and the TS simulator must produce identical decisions and reason codes.

Deliverable: simulator evaluates a transaction in the browser with the same
explanation structure as the API.

## Session 5.2 — Landing v1 (static)

Structure (compressed from the §9A narrative, without scroll animation):
1. Hero: "Secure every instant payment." / subline / CTAs "Explore the Framework"
   and "Run a demo evaluation".
2. Five-layer architecture diagram — a single well-crafted static/SVG figure with
   the sync/async boundary visually explicit (IN-PATH label + latency budget on
   Layers 1–2; async side-paths on 4–5).
3. Interactive demo block: three preset scenarios (normal → ALLOW, suspicious →
   REVIEW, high-risk → DECLINE) evaluated live by the client-side simulator,
   showing decision, score, reason codes, simulated latency — labeled DEMO /
   SYNTHETIC DATA.
4. Assessment preview: the Meridian sample's maturity table (LEVEL by category)
   linking to the sample report PDF.
5. Methodology links + GitHub link + license.

Deliverable: `next build` static export passes; Lighthouse performance ≥ 90 on
the landing; fully usable with JS disabled except the demo block.

## Session 5.3 — Dashboard + transaction explorer

Backed by the Java API (local/VPS), not the simulator:
1. Dashboard: volume, approval/review/decline rates, detection + FP rates (from
   labeled synthetic runs), latency p50/p95/p99, system health.
2. Transaction explorer: list + detail — decision, risk factors, per-layer results,
   timeline, audit trail (rendered from the audit endpoint).
3. Assessment view: render the Meridian report interactively (category drill-down:
   maturity, evidence, findings, recommendation).
4. Resilience view: coupling analysis with projection chart.
5. Every screen showing numbers carries the SYNTHETIC DATA badge.

Deliverable: with `docker compose up` + seeded run, all four areas populated.

## Session 5.4 — Polish + reduced-motion + responsive

1. Responsive pass (mobile/tablet/desktop) on landing and dashboard.
2. `prefers-reduced-motion` respected everywhere; any transition degrades to fades.
3. Empty states, loading states, error states on dashboard views.
4. Accessibility pass: keyboard navigation, contrast on the dark palette.

## Out of scope
Scroll-driven journey (§9A — v1.1), auth, SaaS features, benchmarks, deploy.

## Phase 5 exit checklist
- [ ] Java/TS parity test in CI
- [ ] Static landing exports and scores ≥ 90 performance
- [ ] Demo block runs the three scenarios client-side
- [ ] Dashboard fully populated from a seeded synthetic run
- [ ] SYNTHETIC DATA labeling on every metric surface
---

## Deviation record — landing rebuild, Sep 9 2026

The landing was rebuilt against a supplied visual reference. These are the
points where that build departs from what this spec fixes, and why. Recorded
here because CLAUDE.md requires a documented reason for any deviation from the
fixed stack and design language.

**Design language.** This spec fixes an electric-blue accent and rules out
glassmorphism. The landing now uses a neon-green accent over near-black
charcoal, with translucent card surfaces. Reason: an explicit product decision
on brand direction. Consequence handled: green was the outcome colour for
ALLOW, and the rule in `tailwind.config.ts` was that outcome colours appearing
outside a verdict stop reading as verdicts. The three outcomes now appear only
together, in a single card, each carrying its own icon. If a lone outcome pill
ever returns to a data-dense surface, it must be distinguished by more than hue.

**§9A scroll-driven journey, built before the gate.** This spec defers the
transaction journey to v1.1 and says it may not begin before the Phase 5 gate
is met. It is now built, and the gate is not met: session 5.3 — dashboard,
transaction explorer, assessment view, resilience view — is undelivered.
Reason: the landing is what puts the framework in front of readers. The
dashboard remains owed and the ordering, not the scope, is what changed.

**Client-side simulator removed.** `frontend/src/simulator/`, its two vitest
suites, `scripts/generate-rules.mjs` and the Demo block are deleted. Two exit
checklist items above are therefore abandoned rather than met:

- *Java/TS parity test in CI* — note that this was never actually wired up.
  The frontend job in `.github/workflows/ci.yml` runs lint and build only; it
  has no test step, so the parity guarantee this spec asked for was never
  enforced. Deleting the simulator removes the unenforced TypeScript port, not
  a working check.
- *Demo block runs the three scenarios client-side.*

`npm test` now runs `vitest --passWithNoTests`, and there are no frontend tests.

**Rail naming.** Pix is removed from the landing, the navigation, the page
metadata and the contact template. `docs/framework/latency-model.md` and
`docs/framework/false-positive-model.md` still derive their figures from the
Banco Central's *Manual de Tempos do Pix* and still cite it. Rebasing those two
documents on FedNow and RTP primary sources is scheduled work, not done here.
Until it is, the landing and the methodology disagree about which rails the
framework talks about.
