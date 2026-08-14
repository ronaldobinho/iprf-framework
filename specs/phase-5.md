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