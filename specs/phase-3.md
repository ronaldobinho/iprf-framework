# Phase 3 — Assessment Engine (the centerpiece)

**Window:** Aug 31–Sep 5, 2026 (3–4 sessions)
**Depends on:** Phase 2 gate met.
**Gate:** One command produces a complete, exportable institutional assessment
report from a filled questionnaire + evidence inputs.

This phase is what converts "individualized consulting" into "a replicable
methodology any institution can self-execute" — the exact repositioning the
NIW RFE requires. Quality bar is highest here.

---

## Session 3.1 — Assessment domain + configuration schema

1. `assessment-engine` module. Twelve categories (from the framework spec):
   Fraud Prevention, Real-Time Decisioning, Network/Counterparty Risk, External
   Intelligence, Post-Settlement Analytics, False-Positive Management, Resilience,
   Recovery, Scalability, Auditability, Observability, Regulatory Readiness.
2. Per category, defined in YAML configuration (NOT code):
   - controls (each with an ID, question, evidence requirement)
   - maturity criteria per level 0–4 (Not Established / Ad Hoc / Defined /
     Measured / Optimized)
   - scoring rules and weights
   - finding templates with severity (CRITICAL/HIGH/MEDIUM/LOW)
   - recommendation templates
3. Scoring must be traceable: category score = f(control answers) with the formula
   visible in config and echoed in the report. No arbitrary numbers.
4. Versioned assessment model: reports record which model version scored them.

Deliverable: config schema validated at startup; malformed config fails fast with
a clear error.

## Session 3.2 — Assessment execution

1. Input format: a structured assessment file (YAML/JSON) an institution fills in —
   answers per control + evidence references. This file IS the product interface:
   document it thoroughly in `docs/framework/assessment-model.md` (update from
   Phase 1 version).
2. Execution: `POST /api/v1/assessments` (and a CLI entry point:
   `./gradlew runAssessment -Pinput=...`) → computes maturity per category,
   overall maturity level, findings, recommendations, severity ranking.
3. Cross-category logic where the methodology demands it (e.g., Real-Time
   Decisioning cannot exceed level 2 if Observability is level 0 — you cannot
   claim measured latency without measurement). Encode 3–5 such gating rules;
   document each with its rationale.
4. Publish `AssessmentCompleted` event.

Deliverable: the sample institution file produces a deterministic, reproducible
assessment result.

## Session 3.3 — Sample institution + report export

1. Create `samples/institution-meridian/` — a fictional mid-size US bank adopting
   FedNow, with a realistic, internally consistent filled assessment (some strong
   categories, some weak; overall LEVEL 2–3). This is the demo everyone will see:
   invest in realism.
2. Report export:
   - Markdown report (canonical)
   - PDF export (use the repo's chosen toolchain; keep it dependency-light)
   - Structure: executive summary, maturity radar/table, per-category detail
     (score, evidence gaps, findings, recommendations, severity), methodology
     appendix, model version, SYNTHETIC/SAMPLE watermark.
3. Report language: findings written as an auditor would — evidence-based,
   specific, no marketing tone.

Deliverable: `samples/institution-meridian/report.pdf` committed as a showcase
artifact (clearly watermarked SAMPLE).

## Session 3.4 — Tests + spec alignment

1. Golden-file tests: sample input → expected report (catches scoring regressions).
2. Property tests on scoring: monotonicity (better answers never lower a score),
   bounds, gating rules.
3. Reconcile `docs/framework/assessment-model.md` and `maturity-model.md` with the
   implemented reality — the docs and the engine must not diverge.

---

## Owner action this phase (Ronaldo, not Claude Code)
Start third-party outreach NOW: 1–2 practitioners from the Faster Payments Council
network / Baxley referrals invited to run the sample assessment when v1.0 lands.
Lead time is why this starts in early September, not October.

## Out of scope
Resilience module (Phase 4), frontend, benchmarks, deploy.

## Phase 3 exit checklist
- [ ] 12 categories fully config-driven; scoring traceable and documented
- [ ] Gating rules implemented with written rationale
- [ ] Meridian sample assessment produces committed MD + PDF report
- [ ] Golden-file + property tests green
- [ ] Docs and engine reconciled