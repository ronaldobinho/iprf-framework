# IPRF — Phase Specs Index

One spec per phase. Claude Code sessions load CLAUDE.md + the single active spec.
Detailed specs for phases 2–6 are written at the END of the preceding phase,
incorporating what was learned. Scope summaries below are the commitments.

| Phase | Window 2026 | Scope | Gate |
|---|---|---|---|
| 1 | Aug 14–22 | 11 methodology docs + core engine (Layers 1–2, in-path) | Explainable decisions, ArchUnit guard, docs complete |
| 2 | Aug 24–29 | Layers 3–5, event model + idempotency, immutable audit trail, FP/latency metrics | Sync/async paths visibly separated; duplicate-event and stale-state tests green |
| 3 | Aug 31–Sep 5 | Assessment engine: 12 categories × maturity 0–4, config-driven scoring, exportable report (MD+PDF) | One command produces a complete institutional assessment |
| 4 | Sep 7–12 | Resilience + growth-coupling module; B3 case study (historical, disclaimed) | Linear coupling detected in synthetic sample with remediation recommendation |
| 5 | Sep 14–19 | Dashboard + transaction explorer; landing v1 (static, five-layer diagram, TS client-side simulator). Scroll-journey animation = v1.1, post-RFE | End-to-end local demo; landing static-exportable |
| 6 | Sep 21–26 | Benchmarks (`./gradlew benchmark`), full README (20 sections), SECURITY/CONTRIBUTING/CoC, seed scripts, v1.0.0 tag, deploy (Vercel + VPS) | All 20 acceptance criteria from original brief verified |

## RFE evidence actions (owner: Ronaldo, not Claude Code)

| When | Action |
|---|---|
| Phase 3 (early Sep) | Start third-party outreach: 1–2 practitioners (Faster Payments Council network / Baxley referrals) to run an assessment and comment publicly |
| Phase 6 / v1.0.0 | Ask Deborah Baxley for a short supplemental note (or public LinkedIn comment) confirming she reviewed the published IPRF specification — converts the informal "she saw the sketch and liked it" into written, citable evidence. Her existing EOL references the professional record and the technical article, not the framework; do not conflate them in the RFE narrative |
| Sep 28–Oct 5 | Freeze evidence: framework spec compiled as PDF exhibit, screenshots (dashboard, assessment report, diagrams), LinkedIn announcement (EN), release link |
| RFE narrative | Frame the repository as the FORMALIZATION of the methodology already described in the petition and derived from Pix experience 2019–2022 — pre-empting the "reactive evidence" reading |

## Deploy targets (decided)

- Frontend: Vercel (static export; simulator runs client-side, zero backend dependency)
- Backend/demo API: Docker on Hostinger VPS, nginx reverse proxy + Let's Encrypt,
  isolated Docker network, rate-limited public endpoints, synthetic data only
  (VPS shares hardware with production SaaS — see security note in CLAUDE.md)
- Domain: to be purchased before Phase 6 (check availability: iprf.dev, iprf.io,
  iprf-framework.org)
