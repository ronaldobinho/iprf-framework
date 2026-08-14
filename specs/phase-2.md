# Phase 2 — Full Pipeline: Layers 3–5, Events, Audit, Metrics

**Window:** Aug 24–29, 2026 (3–4 sessions)
**Depends on:** Phase 1 gate met (Layers 1–2 in-path engine, ArchUnit guard, docs).
**Gate:** Sync and async paths visibly separated in code; duplicate-event and
stale-state tests green; FP/latency metrics computed from real pipeline runs.

---

## Session 2.1 — Layer 3: Counterparty & Network Signals

1. `risk-state` module: `RiskStateStore` backed by Redis, holding pre-computed
   counterparty risk — receiving-account history summary, network behavior flags,
   previously reported typologies, risk tier.
2. Population happens OUTSIDE the transaction path: a startup loader (from synthetic
   seed data) + async updaters (Phase 2.3 feedback loop writes here).
3. `network-risk` module evaluates in-path using ONLY the store. Extend the ArchUnit
   guard: `network-risk` may not import JPA/repository classes either.
4. Risk-state versioning: every stored entry carries a version/timestamp; decisions
   record which state version they read (needed for audit and stale-state tests).
5. Stale-state policy: if counterparty state is missing or older than a configurable
   TTL, the layer degrades to a neutral contribution with an explicit reason code
   (`NETWORK_STATE_STALE`) — never a blocking lookup, never a silent zero.

Deliverable: transactions to known-risky synthetic counterparties shift score/decision;
stale-state scenario produces the degraded-but-explained path.

## Session 2.2 — Event model + Layer 4 (async enrichment)

1. Event catalog (RabbitMQ, JSON payloads, versioned schema):
   `TransactionReceived`, `RiskEvaluationCompleted`, `TransactionApproved`,
   `TransactionReviewed`, `TransactionDeclined`, `ExternalRiskUpdated`,
   `TransactionSettled`, `FraudPatternDetected`, `AssessmentCompleted`.
2. Idempotency: every consumer keyed by event ID; duplicate delivery is a no-op
   with a log line, not a second side effect. Idempotency store in Postgres.
3. `external-enrichment` module: consumes decision events, calls a SIMULATED
   external fraud registry (an in-repo stub with configurable latency/failure rate
   — no real third-party dependency), and publishes `ExternalRiskUpdated`, which
   updates the Redis risk state for FUTURE decisions.
4. Structural guarantee: the evaluate endpoint never awaits enrichment. Prove it
   with a test where the enrichment stub is configured to hang — decision latency
   must be unaffected.

Deliverable: enrichment demonstrably async; a transaction evaluated after
enrichment of its counterparty gets a different score than one evaluated before.

## Session 2.3 — Layer 5: Post-settlement analysis + feedback loop

1. `post-settlement` module consumes `TransactionSettled`, runs pattern detection
   over settled history: velocity bursts, fan-out (one payer → many new receivers),
   fan-in (mule pattern: many payers → one receiver), amount structuring
   (just-below-threshold sequences).
2. On detection: publish `FraudPatternDetected` with typology label; update the
   counterparty's Redis risk state (closing the loop into Layer 3).
3. Demonstration scenario in the synthetic generator: a mule-pattern sequence that
   is individually ALLOW but flips the receiver's risk tier, causing a later
   transaction to the same receiver to go REVIEW. This is the framework's core
   story — make it a named, runnable scenario.

Deliverable: the mule scenario runs end-to-end and is asserted in an integration test.

## Session 2.4 — Immutable audit trail + metrics

1. `audit` module: append-only table (no UPDATE/DELETE grants in schema migration)
   recording per decision: transaction ID, framework version, rules executed +
   rule versions, risk factors, decision, timestamp, latency, risk-state versions
   read, correlation ID. Written transactionally with the decision.
2. `GET /api/v1/audit/{transactionId}` reconstructs the full explanation after the fact.
3. Metrics module (Micrometer + Prometheus endpoint):
   - decision counters by outcome; approval/review/decline rates
   - detection rate and FP rate computed against synthetic ground-truth labels
     (the generator tags each transaction fraudulent/legitimate — this is what
     makes FP measurement honest rather than invented)
   - latency histograms: decision p50/p95/p99, per-layer, per-rule
4. Health/readiness/liveness endpoints; readiness requires Redis + Postgres + broker.

Deliverable: after a 1,000-transaction synthetic run, a single command (or endpoint)
prints the confusion-matrix-derived rates and latency percentiles.

---

## Out of scope
Assessment engine (Phase 3), resilience module (Phase 4), any frontend, benchmarks.

## Phase 2 exit checklist
- [ ] ArchUnit guards cover risk-engine AND network-risk
- [ ] Duplicate-event, hanging-enrichment, stale-state tests green
- [ ] Mule feedback-loop scenario asserted end-to-end
- [ ] Audit endpoint reconstructs any decision
- [ ] FP/detection rates computed from labeled synthetic ground truth
- [ ] CI green; no secrets; all data synthetic