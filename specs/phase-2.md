# Phase 2 — Full Pipeline: Layers 3–5, Events, Audit, Metrics

**Window:** Aug 24–29, 2026 (3–4 sessions)
**Depends on:** Phase 1 gate met (Layers 1–2 in-path engine, ArchUnit guard, docs).
**Gate:** Sync and async paths visibly separated in code; duplicate-event and
stale-state tests green; FP/latency metrics computed from real pipeline runs.

---

## Carried over from Phase 1

Written at the close of Phase 1, per the process in `README.md`. These are facts
about the code as it now stands — read them before starting 2.1.

**Where things live.** The brief's module list has no `core-domain`, so the
shared domain types live in `risk-engine` under `io.iprf.domain`, and
`transaction-api` depends on `risk-engine`. `AuditRecord` is already defined
there as a plain record with no persistence annotations, precisely so the
`audit` module can map it to storage without dragging JPA onto the payment path.

**State interfaces already exist.** `io.iprf.state.AccountProfileStore` and
`VelocityCounterStore` are read-only interfaces with in-memory implementations.
Session 2.1 adds `RiskStateStore` alongside them and swaps the backing to Redis;
the interfaces are deliberately write-free so an in-path caller cannot populate
what it reads.

**`LayerResult.stateVersion` is already on the wire** — declared, serialized in
the API response, and currently always null because no layer reads pre-computed
counterparty state yet. Layer 3 populates it; `AuditRecord.stateVersionsRead`
already collects it.

**Reason codes are a published vocabulary.** `NETWORK_STATE_ABSENT`,
`NETWORK_STATE_STALE` and `NETWORK_STATE_UNAVAILABLE` are already defined in the
`ReasonCode` enum. Add codes, never rename them — historical audit records and
the Phase 5 TypeScript parity test both depend on stability.

**The ArchUnit guard is in place** at
`risk-engine/src/test/java/io/iprf/architecture/InPathArchitectureTest.java`,
with four rules covering `io.iprf.engine..`, `io.iprf.state..` and
`io.iprf.domain..` against JPA, Spring Data, JDBC, Hibernate, Hikari and HTTP
clients. Session 2.1 extends the package list to cover `network-risk`.

**`Clock` is a Spring bean** (`EngineConfig.iprfClock`). Every timestamped
component takes it by constructor. Do not call `Instant.now()` directly — stale-state
and TTL tests depend on pinning time.

**Metrics have a starting point.** `io.iprf.synthetic.EvaluationStatistics`
already computes the confusion matrix, detection and false-positive rates, the
decision distribution and latency percentiles from generator-assigned labels.
Session 2.4's Micrometer work should expose the same definitions rather than
inventing parallel ones — in particular, a `REVIEW` on a legitimate payment
counts as a false positive.

**Build note.** Gradle 9 no longer puts the JUnit Platform launcher on the test
runtime classpath implicitly; the root build declares it for every module. Any
new module inherits that automatically.

### A measurable target for Layer 3

The Phase 1 scenario run (`./gradlew runScenario`, seed 20260814, 200 profiles,
1,000 transactions) produced:

| | |
|---|---|
| ALLOW / REVIEW / DECLINE | 891 / 47 / 62 |
| Detection rate | 78.75% |
| False positive rate | 5.00% (hard: 0.65%) |
| Precision | 57.80% |
| Latency p50 / p95 / p99 | 0.045 / 0.121 / 0.223 ms |

Nearly all 17 false negatives are the `FRAUD_SUBTLE` scenario — fraud shaped to
look ordinary, where only the destination is wrong. That is precisely the case
Layer 3 exists to catch, and the generator already reuses mule destinations
across those transactions.

**Phase 2 should therefore raise the detection rate without a proportional rise
in the false-positive rate, and the run above is the baseline to compare
against.** If Layer 3 lands and detection does not move, either the feedback
loop is not reaching Layer 3 or the risk tiers are not being populated — and
that is a finding worth having, not a number to quietly retune the thresholds
around.

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