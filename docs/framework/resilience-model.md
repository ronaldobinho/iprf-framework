# Resilience Model

Fraud controls that are unavailable are not fraud controls. This document
defines how the framework assesses resilience: failure isolation, dependency
coupling, recovery time, and whether remediation after an incident is permanent.

The specific pattern where recovery time grows with business volume has its own
document — [`growth-coupling.md`](growth-coupling.md) — because it is the
framework's central resilience thesis.

---

## 1. Why resilience sits inside a fraud framework

On a 24/7 irrevocable rail, availability and fraud control are the same problem
viewed from two angles.

**A control that fails open stops being a control.** If the fraud service is
unavailable and payments continue, the institution is running unprotected during
exactly the window an attacker would choose to create.

**A control that fails closed stops being a payment system.** If the fraud
service is unavailable and payments halt, every legitimate customer is failed.
See [`false-positive-model.md`](false-positive-model.md) — a mass failure is a
mass false positive with a different root cause.

**Attacks and load arrive together.** A fraud attack is a volume event. If the
control's capacity degrades under exactly the conditions that trigger it, the
control's stated performance describes a state that never occurs when it
matters.

The framework's answer is the layer classification in
[`methodology.md`](methodology.md): the in-path layers depend only on
pre-computed state, so the payment path survives the loss of every asynchronous
component and degrades — explicitly, with recorded reason codes — rather than
failing.

---

## 2. The four resilience dimensions

### 2.1 Failure isolation

**Question:** when component X fails, what else stops working?

The design target is that no asynchronous component can take down the payment
path, and that each in-path layer can degrade independently.

| Component fails | Correct behavior |
|---|---|
| External enrichment (Layer 4) | No effect on decisions. State ages. |
| Post-settlement analysis (Layer 5) | No effect on decisions. Risk state stops being refreshed; Layer 3 eventually reports `NETWORK_STATE_STALE`. |
| Message broker | Layers 4–5 stop. Decisions unaffected. Events accumulate. |
| Risk state store (Redis) | Layer 3 degrades whole with `NETWORK_STATE_UNAVAILABLE`. Layers 1–2 continue. Decisions still produced. |
| Primary database | Decisions continue — nothing in-path reads it. Audit persistence buffers or the institution decides to stop. **This is the one deliberate stop-the-line choice**, and it must be an explicit policy, not an emergent behavior. |

The assessment asks institutions to produce this table for their own system. The
common finding is that nobody has ever written it down, and that the answers
differ depending on which engineer you ask.

### 2.2 Dependency coupling

**Question:** what must be available for the system to *start*, and in what
order?

Runtime coupling and startup coupling are different problems and are assessed
separately. A system can be well-isolated at runtime and still require every
dependency to be present, in sequence, to boot — which means a full restart
during an incident is the slowest and most fragile operation the system
performs.

Assessed as an inventory:

| Attribute | Why it matters |
|---|---|
| Count of startup dependencies | Each is a failure point in the recovery path |
| Synchronous vs asynchronous initialization | Synchronous init serializes recovery |
| Sequential chains | Chain length sets a floor on recovery time |
| Database round trips during initialization | The usual source of volume-coupled startup |
| What is loaded eagerly vs lazily | Eager loading of everything is the default that creates the problem |

### 2.3 Recovery time

**Question:** how long from "down" to "correctly serving traffic", and does that
number depend on how much business the institution does?

Recovery time is measured as a **series, not a single number**. One measurement
cannot distinguish a fixed cost from one that scales — and that distinction is
the entire subject of [`growth-coupling.md`](growth-coupling.md).

The maturity model reflects this directly: Recovery cannot exceed level 2 unless
recovery time has been measured at multiple scale points. See
[`maturity-model.md`](maturity-model.md).

Two things must be separated when measuring:

| Phase | Definition |
|---|---|
| **Time to process start** | Infrastructure: container scheduled, image pulled, process launched |
| **Time to correct service** | Application: state loaded, caches warm, dependencies verified, ready to decide correctly |

Institutions routinely report the first and experience the second. For a fraud
system the gap is significant, because a service that accepts traffic before its
pre-computed state is loaded is a service making decisions on empty state — a
silent failure mode strictly worse than being down, since it produces confident
wrong answers rather than visible unavailability.

**A readiness probe that passes before risk state is loaded is a resilience
defect, and the assessment treats it as one.**

### 2.4 Incident recurrence

**Question:** when the same root cause produces a second incident, what does
that say about the first remediation?

Recurrence is the framework's proxy for whether remediation is permanent or
cosmetic. The distinction:

| Remediation type | Characteristic | Recurrence |
|---|---|---|
| **Mitigating** | Restores service. Restart, failover, scale-up, manual intervention. | Root cause intact — recurs |
| **Permanent** | Removes the possibility. Design change, dependency removal, structural fix. | Cannot recur in the same form |

Mitigating actions are necessary during an incident. The failure is stopping
there — closing the incident when service is restored, with a follow-up ticket
that is never prioritized because the system is working again.

Assessed by asking for the institution's own incident record:

- Distinct root causes over the period
- How many produced more than one incident
- For recurring causes, what the first remediation actually changed
- Median time from incident to permanent remediation

An institution whose incidents cluster on a small number of recurring root
causes has a remediation-discipline problem, not a reliability problem, and the
finding says so — because they require entirely different fixes.

---

## 3. Degradation must be designed, not discovered

The recurring theme across all four dimensions.

Every layer in this framework has a **specified** behavior when its inputs are
unavailable, and that behavior is asserted in tests:

| Principle | Consequence |
|---|---|
| Degradation is explicit | A degraded layer emits a reason code recorded on the decision |
| Degradation is neutral, never favorable | A failed layer never silently becomes `ALLOW` — see [`latency-model.md`](latency-model.md#4-timeout-behavior) |
| Degradation is visible | Degraded-evaluation rate is a tracked metric, alertable |
| Degradation is tested | Dependency-failure and stale-state scenarios are explicit test cases |

The alternative — discovering degradation behavior during an incident — means
learning what the system does at the moment you can least afford to be surprised.

---

## 4. What the assessment produces

Resilience findings map to three categories in
[`assessment-model.md`](assessment-model.md): **Resilience** (7), **Recovery**
(8), **Scalability** (9).

Typical findings and their severity:

| Finding | Severity | Why |
|---|---|---|
| An asynchronous component can block the payment path | CRITICAL | Violates the framework's core principle; converts a fraud control into an availability incident |
| Readiness probe passes before pre-computed state is loaded | CRITICAL | Produces confident decisions on empty state |
| Recovery time measured at one scale point only | HIGH | Cannot distinguish fixed cost from volume-coupled cost |
| Recovery time grows linearly with business volume | HIGH | See [`growth-coupling.md`](growth-coupling.md) |
| Degradation behavior undocumented for one or more layers | HIGH | Behavior under failure is unknown until failure |
| Same root cause produced multiple incidents | MEDIUM | Remediation discipline gap |
| Startup dependency order undocumented | MEDIUM | Recovery depends on individual knowledge |

---

## 5. Origin and framing

> **(C) Historical case.** This resilience model derives from the author's
> professional engineering experience — specifically a recovery-time case at B3,
> the Brazilian financial market infrastructure operator, in which a critical
> system's startup time was reduced substantially by removing sequential
> per-instrument validation, introducing parallel processing, and batching
> database round trips.
>
> That work **predates this repository and was not performed by it**. This
> repository contains an analyzer that detects the same pattern in measurements
> you provide, and a simulator that models the same class of remediation against
> synthetic data. It has not reproduced the original result and does not claim
> to.

The case is documented in full, with its framing, in
[`growth-coupling.md`](growth-coupling.md).
