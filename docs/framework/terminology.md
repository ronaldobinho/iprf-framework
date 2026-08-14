# Terminology

Terms as this framework uses them. Where an industry definition is contested or
varies by jurisdiction, the definition adopted here is stated explicitly along
with the reason.

---

## Core framework terms

**IPRF** — Instant Payment Fraud & Resilience Framework. Always this spelling.

**In-path (SYNC)** — Evaluated during payment authorization, inside a bounded
latency budget, reading pre-computed state only. Layers 1–3. See
[`fraud-control-layers.md`](fraud-control-layers.md#classification-rules) for the
four conditions a layer must satisfy to qualify.

**Async (off-path)** — Evaluated outside the payment path. Has no latency budget
because nothing waits for it. Layers 4–5. Feeds future decisions by updating
pre-computed state.

**Pre-computed state** — Risk data materialized *before* a transaction arrives,
held in memory or Redis, versioned and timestamped. The mechanism that makes
in-path evaluation possible without live queries.

**`RiskStateStore`** — The component holding pre-computed risk state. Written by
async layers, read by in-path layers. Never queried against the primary
transactional database.

**Layer** — One of the five control layers. Each has a fixed path classification
that does not vary by deployment or configuration.

**`LayerResult`** — A layer's output: its contribution to the composite score,
the rules that fired with reason codes and individual contributions, and
measured latency.

**Reason code** — A stable, enumerated identifier for why a rule fired or a layer
degraded (`AMOUNT_DEVIATION_HIGH`, `NETWORK_STATE_STALE`). Stable because
downstream consumers, the TypeScript simulator, and historical audit records all
depend on the vocabulary not shifting.

**Composite risk score** — A value in `0.0–1.0` combining all layer
contributions. Always decomposable: the per-rule contributions that produced it
are recorded with it.

**Decision** — One of `ALLOW`, `REVIEW`, `DECLINE`.

**Degradation** — Defined behavior when a layer's inputs are missing, stale, or
unavailable: an explicit reason code and a *neutral* contribution. Never a silent
zero, never a favorable default, never a blocking retry.

**Feedback loop** — Layer 5 detects a pattern → writes to pre-computed state →
Layer 3 reads it in-path on a later transaction. The mechanism by which
expensive analysis influences real-time decisions without being on the real-time
path.

**Growth coupling** — Recovery time that is a function of business volume:
`startupTime ≈ base + N × cost` with `cost > 0`. See
[`growth-coupling.md`](growth-coupling.md).

**Framework version** — The version of the rule set and decision logic that
produced a decision. Recorded on every decision and every audit record.

---

## Decision outcomes

| Term | Definition | Rail analogue |
|---|---|---|
| **`ALLOW`** | Proceed on the real-time path | Normal settlement |
| **`REVIEW`** | Hold for asynchronous assessment within the window the rail permits | Pix fraud-suspicion window (30/60 min); FedNow "accept without posting" |
| **`DECLINE`** | Reject the payment | Rejection |

---

## Measurement terms

**Ground truth** — Whether a transaction was actually fraudulent. In this
repository, assigned by the synthetic generator at generation time. In
production, the hardest problem in fraud measurement: labels arrive late,
incomplete, and biased by the control itself.

**False positive (FP)** — A legitimate payment that was flagged. **This framework
counts `REVIEW` on a legitimate payment as a false positive**, not only
`DECLINE` — a legitimate payment that did not complete on the real-time path is
a degraded outcome regardless of which non-allow bucket it landed in. Counting
only declines would let an institution improve the metric by routing everything
to review.

**Hard false positive** — A legitimate payment that was **declined**. Reported
alongside the FP rate, never instead of it.

**False negative (FN)** — A fraudulent payment that was allowed.

**False positive rate** — `FP / (FP + TN)`. Share of legitimate payments flagged.

**Detection rate** (recall, TPR) — `TP / (TP + FN)`. Share of fraudulent payments
caught.

**Precision** — `TP / (TP + FP)`. Share of flagged payments that were actually
fraudulent.

**Approval / review / decline rate** — Share of all transactions receiving each
decision. **Operational metrics, not accuracy metrics.** They require no labels
and must never be presented as evidence of effectiveness.

**p50 / p95 / p99** — Latency percentiles. Averages are not reported anywhere in
this framework: an average hides the tail that causes rejected settlements. These
specific percentiles match those the Pix regulation uses for participant service-level
assessment.

**SYNTHETIC / DEMO DATA** — Mandatory label on every number this repository
produces. No figure here derives from real transactions or a real institution.

---

## Fraud typology terms

**APP fraud (Authorised Push Payment fraud)** — The account holder is
manipulated into authorizing a payment themselves. Authentication succeeds
because it is genuine. The dominant instant-payment fraud category.

**Account takeover (ATO)** — An attacker gains control of a genuine account and
initiates payments. Distinguished from APP fraud by *who* issues the
instruction.

**Mule account** — An account used to receive and move fraud proceeds, whether
knowingly or through recruitment.

**Fan-in** — Many unrelated payers sending to one receiver in a short window. The
signature of a mule account collecting proceeds.

**Fan-out** — One payer sending to many new receivers in a short window. Either
dispersal of stolen funds or a mule distribution layer.

**Structuring** — Splitting a transfer into multiple payments sized just below a
known threshold, to evade a control.

**Synthetic identity** — A fabricated identity, often combining real and invented
attributes, used to open an account. Substantially an onboarding problem rather
than a transaction-monitoring one.

**Layering** — Moving funds through multiple accounts to obscure their origin.
Instant rails compress this from days to minutes.

---

## Assessment terms

**Category** — One of the twelve assessment areas. See
[`assessment-model.md`](assessment-model.md#2-the-twelve-categories).

**Control** — The atomic unit of assessment: a question about the institution,
with an evidence requirement, rationale, and weight.

**Evidence tier** — E0 None (assertion) / E1 Documented / E2 Configured /
E3 Measured. **Caps** the maturity level a category can reach.

**Maturity level** — 0 Not Established / 1 Ad Hoc / 2 Defined / 3 Measured /
4 Optimized. See [`maturity-model.md`](maturity-model.md).

**Gate** — A cross-category rule that caps one category's level based on another's
deficiency. Gates only ever lower a score.

**Finding** — A documented control gap, with severity, evidence gap, impact and
recommendation.

**Severity** — CRITICAL / HIGH / MEDIUM / LOW. Describes **consequence only** —
effort and urgency are separate fields so they cannot be traded against each
other silently.

**Model version** — The version of the assessment configuration that scored an
assessment. Recorded on every result so a re-score under a new model is
distinguishable from a genuine change in posture.

---

## Rail and regulatory terms

**Instant payment** — A payment that settles in seconds, 24/7/365, and is
irrevocable once settled.

**Irrevocability** — Settled funds cannot be unilaterally recalled. Recovery
depends on the receiving institution's cooperation and on the funds still being
present.

**FedNow** — The Federal Reserve's instant payment service (United States).

**Pix** — Brazil's instant payment system, operated by the Banco Central do
Brasil.

**SPI** *(Sistema de Pagamentos Instantâneos)* — The settlement system
underlying Pix. Maximum end-to-end settlement on the primary message channel is
**40 seconds**; transactions exceeding it are rejected.

**DICT** — The Pix directory service mapping aliases (*chaves*) to accounts.
Published SLA: key query P99 of 1 second.

**MED** *(Mecanismo Especial de Devolução)* — Pix's special return mechanism for
suspected fraud. An investigation process with a defined analysis window, **not**
a reversal right.

**Faster Payments** — The UK's instant payment service, the longest-running
major instant rail and therefore the best public dataset on instant-payment
fraud evolution.

**Mandatory reimbursement (UK)** — Since 7 October 2024, UK PSPs must reimburse
APP scam victims up to £85,000, with the cost **split equally between sending and
receiving firms**. The rule that gives receiving-side controls a direct cost
basis.

**"Accept without posting"** — A FedNow status a receiving institution can return
to indicate further information is required before accepting a payment. The
rail-level analogue of `REVIEW`.

---

## Claim markers

Used throughout this documentation to keep categories of claim distinct.

| Marker | Meaning |
|---|---|
| **(A) Implemented here** | Working code in this repository, verifiable by running it |
| **(B) Methodology** | Assessment approach derived from experience; not a software feature |
| **(C) Historical case** | Professional experience predating this repository; never a repository result |
| **(D) Roadmap** | Not built; stated as intent |
