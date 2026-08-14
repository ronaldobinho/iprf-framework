# Phase 1 — Framework Spec + Core Decision Engine

**Window:** Aug 14–22, 2026 (3–5 Claude Code sessions)
**Gate:** A bank engineer can understand the methodology from `/docs/framework`
alone, and the API evaluates a synthetic transaction with fully explainable output.

Order is deliberate: **documentation before code.** The methodology documents are
the framework's primary artifact; the code demonstrates them.

---

## Session 1.1 — Repository foundation

1. Rename repo `irpf-framework` → `iprf-framework` (GitHub redirects old URL).
2. Scaffold monorepo per CLAUDE.md layout. Gradle multi-module backend (Java 21,
   Spring Boot 3.x), empty Next.js 14 app, `docker-compose.yml` with PostgreSQL 16,
   Redis 7, RabbitMQ.
3. GitHub Actions: build + test on push. Must be green on the scaffold.
4. `LICENSE` (Apache-2.0), `.env.example`, `.gitignore`, root `README.md` stub
   (one paragraph: what IPRF is; full README is Phase 6).

Deliverable: `./gradlew build` green, `docker compose up` starts infra, CI green.

## Session 1.2 — The 11 methodology documents

Write `/docs/framework/` as technical reference material (not marketing copy):

| File | Content anchor |
|---|---|
| `methodology.md` | Why instant-payment fraud is different (irrevocability, seconds-not-days); the five layers; the sync/async principle as the core thesis |
| `fraud-control-layers.md` | Each layer: purpose, inputs, outputs, in-path vs async classification, failure modes |
| `latency-model.md` | Latency budgets per layer, timeout budgets, why in-path rules must be deterministic and pre-computed |
| `false-positive-model.md` | FP as failed payment; detection-vs-FP tradeoff; metrics definitions (FP rate, detection rate, decline/review/approval, p50/p95/p99) |
| `assessment-model.md` | The 12 assessment categories; controls, evidence requirements, findings, recommendations, severity |
| `maturity-model.md` | Levels 0–4 (Not Established → Optimized); scoring rules live in configuration, with rationale |
| `resilience-model.md` | Startup/recovery time, dependency coupling, sequential initialization, incident recurrence, permanent remediation |
| `growth-coupling.md` | The `startupTime ≈ base + N × cost` pattern; detection and remediation (parallelism, batching, dependency removal) |
| `architecture.md` | Reference architecture with Mermaid diagrams; modular monolith rationale; event catalog |
| `threat-model.md` | Fraud typologies for instant payments (APP fraud, mule networks, account takeover, synthetic identity); what each layer mitigates |
| `terminology.md` | Glossary |

Writing rules:
- Ground claims in the public record of instant-payment fraud (FedNow, Pix, UK
  Faster Payments APP-fraud experience) — cite sources where used.
- The founder-experience origin goes in ONE short "Origin of the methodology"
  section inside `methodology.md`, following the honesty rules in CLAUDE.md.
- Diagrams in Mermaid.

Deliverable: all 11 files complete and internally consistent.

## Session 1.3 — Domain model + API skeleton

1. Core domain: `Transaction`, `Decision` (ALLOW/REVIEW/DECLINE), `RiskFactor`,
   `LayerResult`, `RuleDefinition`, `RuleVersion`, `AuditRecord`.
2. `POST /api/v1/transactions/evaluate` per the contract in CLAUDE.md. Input
   validation (Bean Validation), correlation ID filter, structured JSON logging.
3. Latency measurement built into the pipeline from day one (per-decision and
   per-layer, recorded in `layerResults`).
4. OpenAPI spec generated (springdoc).

Deliverable: endpoint accepts a synthetic transaction and returns a stubbed but
schema-complete response with measured latency.

## Session 1.4 — Layers 1–2 (in-path engine)

1. **Layer 1 — Identity & Account Posture:** deterministic rules over pre-loaded
   account profiles (account age, verification status, device known/unknown,
   channel, historical behavior summary). Profiles seeded from synthetic dataset.
2. **Layer 2 — Behavioral Scoring:** deviation rules — amount vs payer baseline,
   new counterparty, unusual hour, velocity (txns per window), channel switch.
   Composite risk score 0.0–1.0 with per-rule contribution recorded.
3. Thresholds and rule weights in `application-rules.yml` (configuration, not code).
   Changing a threshold requires no recompilation.
4. Every rule execution produces a reason code + human-readable explanation.
5. In-path constraint enforced structurally: Layers 1–2 read ONLY from an in-memory
   /Redis-backed `RiskStateStore` populated at startup/async. Add an architecture
   test (ArchUnit) that fails the build if `risk-engine` imports a repository/JPA
   class.

Deliverable: synthetic transactions produce differentiated ALLOW/REVIEW/DECLINE
outcomes with explanations; ArchUnit guard green.

## Session 1.5 — Tests + synthetic data

1. Unit tests: every rule, threshold boundaries, score composition.
2. Synthetic dataset generator: N payer profiles with baselines + a transaction
   generator producing normal, suspicious and high-risk scenarios (seeded, reproducible).
3. API tests (MockMvc/RestAssured): contract, validation errors, latency field present.
4. Coverage focus: rule engine and decision pipeline (business-critical), not a
   global percentage.

Deliverable: `./gradlew test` green; a documented command evaluates 1,000 synthetic
transactions and prints the decision distribution.

---

## Out of scope for Phase 1 (do not start early)

Layers 3–5, events/RabbitMQ, assessment engine, resilience module, frontend,
benchmarks, deploy. These are Phases 2–6.

## Phase 1 exit checklist

- [ ] CI green on main
- [ ] 11 methodology docs complete
- [ ] Evaluate endpoint returns explainable decisions with measured latency
- [ ] ArchUnit in-path guard active
- [ ] Synthetic scenario run produces ALLOW/REVIEW/DECLINE distribution
- [ ] No secrets committed; all data synthetic
