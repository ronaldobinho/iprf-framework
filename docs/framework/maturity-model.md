# Maturity Model

Five levels, 0 through 4, applied per category. This document defines what each
level means, how scores are computed, and why the scoring rules live in
configuration rather than in code.

Used together with [`assessment-model.md`](assessment-model.md), which defines
the categories and controls being scored.

---

## 1. The levels

| Level | Name | Defining question | The state it describes |
|---|---|---|---|
| **0** | Not Established | Does it exist? | The capability is absent. No control, no process, no owner. |
| **1** | Ad Hoc | Does it happen? | It happens, driven by individuals rather than by design. Inconsistent, undocumented, dependent on who is on shift. |
| **2** | Defined | Is it written down? | Documented, repeatable, owned. A new engineer can find how it works. Not yet quantified. |
| **3** | Measured | Do you have the numbers? | Instrumented and quantified. Performance is known, not believed. Deviation is detected. |
| **4** | Optimized | Do the numbers drive change? | Measurements feed a deliberate improvement loop. Thresholds are tuned against observed outcomes, and the effect of tuning is measured. |

### The two transitions that carry all the weight

**1 → 2 (Ad Hoc to Defined)** is where a capability stops depending on
individuals. Below level 2, the institution's control posture is a property of
its staffing.

**2 → 3 (Defined to Measured)** is where claims become checkable. This is the
transition most self-assessments overstate, and it is the one the framework
guards hardest — via the evidence tiers in
[`assessment-model.md`](assessment-model.md#4-evidence-requirements). Level 3
requires **E3 evidence: ongoing measurement**. A documented process with no
measurement is level 2 no matter how good the document is.

### Level 4 is deliberately hard

Level 4 is not "level 3, done well". It requires a demonstrated **loop**:
measurements are used to change configuration, the change's effect is measured,
and that cycle is routine rather than exceptional.

For a fraud control, level 4 means the institution can answer: *when we last
tightened a threshold, what happened to our detection rate, our false-positive
rate, and our review volume?* An institution that cannot answer that question
about a change it actually made is at level 3.

Most categories in most institutions should be at 2 or 3. A self-assessment
returning level 4 across the board has almost certainly mis-scored, and the
report says so.

---

## 2. Level descriptors by category

Full criteria for all twelve categories live in configuration. These four are
reproduced here because they carry the framework's core theses.

### Real-Time Decisioning

| Level | Criteria |
|---|---|
| 0 | No real-time fraud decisioning on the instant-payment path. |
| 1 | Some rules exist in the payment path. Behavior under latency pressure is not defined. Thresholds change by code deployment. |
| 2 | The in-path pipeline is documented. Latency budgets are stated. Degradation behavior is specified per layer. Thresholds are externalized to configuration. |
| 3 | Latency is measured per layer at p50/p95/p99. Timeout and degraded-evaluation rates are tracked. Decisions carry rule versions. Conformance to the budget is verified, not assumed. |
| 4 | Budgets and thresholds are tuned against measured decision quality. Regression in latency or decision distribution is detected automatically and acted on. |

### False-Positive Management

| Level | Criteria |
|---|---|
| 0 | False positives are not measured. Success is reported as fraud prevented. |
| 1 | False positives are known anecdotally — via complaints, support volume, escalations. |
| 2 | A definition exists: what counts as a false positive, including whether `REVIEW` on a legitimate payment counts. Label provenance is documented. |
| 3 | FP rate and detection rate are computed from the same run against labeled data, and reported together. Review rate is tracked separately. |
| 4 | The detection/FP tradeoff is managed as an explicit, owned decision with a stated target operating point, and threshold changes are evaluated against both rates before adoption. |

### Recovery

| Level | Criteria |
|---|---|
| 0 | Recovery time is unknown. |
| 1 | Recovery time is known approximately, from incident experience. |
| 2 | Recovery procedures are documented. Recovery time has been measured at least once. |
| 3 | Recovery time is measured at multiple scale points, so a fixed cost can be distinguished from one that grows with volume. Startup dependency order is documented. |
| 4 | Recovery time is tracked over time as volume grows, with a defined threshold that triggers remediation before it becomes an incident. |

### Auditability

| Level | Criteria |
|---|---|
| 0 | Decisions are not persisted, or persisted without their inputs. |
| 1 | Decisions are logged. Reconstruction is possible in principle, laborious in practice. |
| 2 | Decisions persist to a defined store with inputs, risk factors and outcome. |
| 3 | Every decision persists rule versions, risk-state versions read, correlation ID and latency, immutably. Any past decision can be reconstructed exactly. |
| 4 | Reconstruction is routinely exercised — dispute handling and false-positive investigation run against the audit trail as a normal workflow, not a forensic exercise. |

---

## 3. Scoring

### Category score

For each category, over its applicable controls:

```
category_score = Σ(control_level × control_weight) / Σ(control_weight)
```

Producing a continuous value in `[0, 4]`. The **reported category level** is the
floor of that value, after gates are applied:

```
category_level = min(floor(category_score), evidence_cap, gate_cap)
```

Three caps, each able only to lower the result:

| Cap | Source |
|---|---|
| `evidence_cap` | Highest level supportable by the evidence tier provided — see [`assessment-model.md`](assessment-model.md#4-evidence-requirements) |
| `gate_cap` | Cross-category gating rules |
| `floor()` | Partial progress toward a level is not that level |

**Floor, not round.** A category at 2.9 is level 2. Rounding up would let an
institution report a level it does not hold on the strength of being close, and
the whole point of a maturity level is that it is a threshold.

### Overall maturity

```
overall_score = Σ(category_score × category_weight) / Σ(category_weight)
overall_level = floor(overall_score)
```

Subject to one additional rule:

> **The overall level cannot exceed the minimum category level plus one.**

An institution with eleven categories at level 4 and Auditability at level 0
does not have a level-3 posture. It has an unauditable one. Averaging without
this rule lets a single disqualifying gap disappear into a strong mean — which
is precisely the failure mode a maturity model is supposed to prevent.

---

## 4. Why scoring lives in configuration

Every weight, level criterion, cap and gate is defined in YAML, validated at
startup, and versioned. None of it is in code.

**Because the numbers are contestable.** An institution may reasonably weight
Regulatory Readiness differently under a different supervisor. The framework's
structure — categories, levels, evidence tiers, the capping mechanism — is the
contribution. The specific weights are a defensible default, not a finding about
the universe.

**Because the model must be diffable.** An assessment records the model version
that scored it. When the model changes, the change is a reviewable configuration
diff, and a re-score against a new version is distinguishable from a genuine
change in posture. An institution comparing this year to last year needs to know
which of the two moved.

**Because opaque scoring invites gaming and forbids disagreement.** A reader who
disagrees with a score should be able to locate the exact line that produced it
and argue with that line. A score that cannot be traced to a rule cannot be
argued with, only accepted or ignored.

---

## 5. Reading a result honestly

**A level is a floor, not a summary.** Level 2 means every level-2 criterion is
met; it says nothing about how far past them the institution is.

**Category shape matters more than the overall number.** A profile of
`[4,4,4,4,4,0,...]` and one of `[2,2,2,2,2,2,...]` can produce similar averages
and describe entirely different institutions. Reports lead with the per-category
table; the overall level is reported second, deliberately.

**Progress is not linear in effort.** 0 → 2 is usually documentation and
ownership. 2 → 3 usually requires building measurement infrastructure that does
not exist. Recommendations carry an `effort` field separate from `severity` so
this asymmetry stays visible.

**The model does not predict losses.** It measures posture. A well-scoring
institution can still be successfully attacked, and the report says so.
