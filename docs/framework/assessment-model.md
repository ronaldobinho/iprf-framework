# Assessment Model

How an institution assesses itself against this framework. This document defines
the twelve categories, the structure of a control, what counts as evidence, and
how findings and recommendations are produced.

The companion document [`maturity-model.md`](maturity-model.md) defines the
levels a category is scored against.

> **(B) Methodology.** This document specifies the assessment approach. The
> engine that executes it is built in Phase 3; this specification is written
> first and the implementation conforms to it, not the reverse.

---

## 1. What an assessment is for

An assessment answers one question: **where would this institution's
instant-payment fraud and resilience posture actually fail, and what should it
fix first?**

Three properties are required for that answer to be worth anything:

**Evidence-based.** Every control is scored against artifacts that exist —
configuration, measurements, runbooks, incident records — not against opinions
about intent. "We monitor latency" is not evidence. A dashboard showing p99 per
layer is.

**Traceable.** Every score decomposes into the control answers that produced it,
via a formula that is visible in configuration and echoed in the report. No
number appears that a reader cannot reconstruct.

**Self-executable.** An institution can run this without engaging anyone. That
constraint is what makes it a methodology rather than a consulting engagement,
and it disciplines the design: anything requiring an assessor's judgment call
has to be reduced to a question the institution can answer about itself.

---

## 2. The twelve categories

Grouped by what they protect.

### Fraud control effectiveness

| # | Category | Assesses |
|---|---|---|
| 1 | **Fraud Prevention** | Overall control coverage across the typologies in [`threat-model.md`](threat-model.md); whether prevention is designed or accumulated |
| 2 | **Real-Time Decisioning** | The in-path pipeline: determinism, latency budgets, degradation behavior, configurability of thresholds |
| 3 | **Network / Counterparty Risk** | Layer 3 capability: pre-computed counterparty state, risk tiers, staleness handling |
| 4 | **External Intelligence** | Layer 4: enrichment sources, integration discipline, isolation from the payment path |
| 5 | **Post-Settlement Analytics** | Layer 5: typology detectors, the feedback loop into pre-computed state |
| 6 | **False-Positive Management** | Whether FP is measured at all, label provenance, whether the detection/FP tradeoff is made explicitly |

### Operational robustness

| # | Category | Assesses |
|---|---|---|
| 7 | **Resilience** | Failure isolation, dependency coupling, degradation behavior under partial outage |
| 8 | **Recovery** | Recovery-time measurement, startup behavior, permanence of remediation after incidents |
| 9 | **Scalability** | Whether cost grows with volume in ways that threaten the latency budget or recovery time |

### Governance

| # | Category | Assesses |
|---|---|---|
| 10 | **Auditability** | Decision reconstruction: are rule versions, risk-state versions and inputs persisted immutably |
| 11 | **Observability** | Whether the system can be measured: latency percentiles, decision distribution, per-rule attribution |
| 12 | **Regulatory Readiness** | Alignment with applicable rail requirements — settlement timing, reimbursement obligations, fraud reporting duties |

### Why these twelve

Categories 1–5 map to the five control layers. Category 6 exists because the
layers can all be present and the institution can still be failing customers
invisibly. Categories 7–9 exist because a fraud control that is unavailable is
not a fraud control. Categories 10–12 exist because a decision that cannot be
explained, measured, or defended to a supervisor is a liability regardless of
its accuracy.

---

## 3. Structure of a control

Each category contains controls. A control is the atomic unit of assessment.

```yaml
- id: RTD-04
  category: real-time-decisioning
  question: >
    Are fraud decision thresholds and rule weights changeable without
    deploying code?
  rationale: >
    Threshold changes are risk decisions with a response time requirement
    measured in hours. Coupling them to a release cycle means the institution
    cannot respond to an active attack faster than it can ship software.
  evidence_requirement:
    - Configuration file or rule store containing the thresholds
    - Change record showing a threshold modified without a deployment
  answer_type: maturity_scale     # or: boolean | scale_0_4 | not_applicable
  weight: 1.5
  applies_when:
    layer_in_scope: [1, 2, 3]
```

| Field | Purpose |
|---|---|
| `id` | Stable identifier. Referenced by findings; never reused after retirement. |
| `question` | Answerable by the institution about itself, without interpretation |
| `rationale` | Why this control matters. Present so a reader can disagree with the framework on the merits. |
| `evidence_requirement` | The artifacts that substantiate the answer |
| `answer_type` | Determines the scoring contribution |
| `weight` | Relative contribution within the category |
| `applies_when` | Scoping — controls that do not apply are excluded, not scored zero |

### Not-applicable is not zero

A control that does not apply to an institution's scope is **removed from the
denominator**. An institution that does not operate on the receiving side is not
penalized for lacking receiving-side controls.

This is deliberately narrow. Scoping out a control requires a stated scope
condition, not a judgment that the control is unimportant. The distinction
between "this does not apply to us" and "we have not done this" is exactly the
distinction assessments most often lose.

---

## 4. Evidence requirements

An answer without evidence is an assertion. The model defines four evidence
tiers.

| Tier | Meaning | Example |
|---|---|---|
| **E0 — None** | Assertion only | "We have latency monitoring" |
| **E1 — Documented** | Written artifact exists | A design document describing the latency budget |
| **E2 — Configured** | Artifact exists in the running system | The threshold configuration file in production |
| **E3 — Measured** | Ongoing measurement demonstrates operation | A dashboard showing p99 per layer over 90 days |

**A category's maturity is capped by the evidence tier supporting it.** An
institution asserting excellence at E0 scores as if the control were absent,
because from the outside those two states are indistinguishable.

The tiers exist to make one failure mode impossible: a self-assessment that
scores well because the institution believes it is doing well.

---

## 5. Findings

Where a control is unmet or partially met, the assessment produces a finding.

```yaml
finding:
  id: F-RTD-04
  control: RTD-04
  severity: HIGH
  statement: >
    Fraud decision thresholds are compiled into application code. Changing a
    threshold requires a full release cycle.
  evidence_gap: >
    No configuration artifact was provided. Thresholds located in source.
  impact: >
    Response time to an active attack is bounded by the deployment pipeline,
    measured in days rather than hours.
  recommendation: >
    Externalize thresholds and rule weights to configuration loaded at startup
    and refreshable at runtime. Verify by changing a threshold in a non-production
    environment without rebuilding.
  effort: MEDIUM
```

### Severity

Severity describes **consequence**, not effort or urgency. Those are separate
fields precisely so they cannot be quietly traded against each other.

| Severity | Definition |
|---|---|
| **CRITICAL** | Can cause payment-path failure, undetected systematic fraud exposure, or breach of a rail requirement |
| **HIGH** | Materially degrades detection or false-positive performance, or prevents decision reconstruction |
| **MEDIUM** | Weakens a control's effectiveness or increases operational cost without direct exposure |
| **LOW** | Improvement opportunity; no material exposure |

### Findings are written as an auditor would write them

Specific, evidence-anchored, and free of marketing register. A finding names
what was observed, what evidence was missing, and what consequence follows. It
does not characterize the institution, speculate about causes, or recommend
products.

---

## 6. Cross-category gating

Some categories cannot honestly exceed a level while another category is
deficient. These are encoded as gating rules with written rationale — a small
number of them, because each one is a strong claim.

| Gate | Rationale |
|---|---|
| Real-Time Decisioning ≤ 2 if Observability = 0 | You cannot claim a latency budget you do not measure. Without measurement, conformance is belief. |
| False-Positive Management ≤ 1 if fraud labels have no stated provenance | A detection rate computed from unknown labels is not a measurement. |
| Post-Settlement Analytics ≤ 2 if Network/Counterparty Risk = 0 | Detection with no path back into decisioning is analysis, not control. The feedback loop is the mechanism. |
| Recovery ≤ 2 if recovery time is not measured at multiple scale points | A single recovery-time measurement cannot distinguish a fixed cost from one that grows with volume. See [`growth-coupling.md`](growth-coupling.md). |
| Auditability ≤ 1 if decisions do not persist rule versions | A decision that cannot be reproduced cannot be audited, regardless of what else is stored. |

Gates only ever **cap**; they never raise a score. Every gate applied is stated
explicitly in the report, with the rule and its rationale, so a capped score is
never mistaken for a directly assessed one.

---

## 7. The assessment input file

The input is a structured file the institution fills in. **This file is the
product interface** — it is what makes the methodology self-executable.

```yaml
assessment:
  model_version: 1.0.0
  institution:
    name: "Example Institution"
    scope:
      rails: [fednow]
      layer_in_scope: [1, 2, 3, 4, 5]
      role: [sending, receiving]
  responses:
    - control: RTD-04
      answer: 2
      evidence_tier: E1
      evidence_refs:
        - "design/fraud-thresholds.md#configuration"
      notes: >
        Thresholds documented but currently compiled into the decision service.
```

Executing it produces: per-category maturity, overall maturity, applied gates,
findings ranked by severity, recommendations, and the model version that scored
it. Given the same input and the same model version, the output is identical —
assessments are reproducible and diffable across time.

---

## 8. What an assessment is not

- **Not a certification.** There is no passing score and no issuing authority.
- **Not a benchmark against peers.** No peer dataset exists, and inventing one
  would violate the project's honesty rules.
- **Not a substitute for penetration testing, audit, or supervisory review.**
- **Not predictive.** It measures posture, not future loss. An institution
  scoring well can still be attacked successfully.
