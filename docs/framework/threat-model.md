# Threat Model

Fraud typologies specific to irrevocable instant payments, and which control
layer mitigates each. This document exists to make one thing checkable: **for
every typology, which layer is supposed to catch it, and what happens when that
layer is absent.**

---

## 1. What makes the instant-payment threat model distinct

Three attacker advantages exist on instant rails that do not exist, or are much
weaker, on deferred-settlement rails:

| Advantage | Consequence for the defender |
|---|---|
| **Finality** | No reversal right. Recovery depends on the receiving institution and on the funds still being present. |
| **Speed of layering** | Funds can traverse several accounts within minutes. By the time a report is filed, the trail is cold and the balance is zero. |
| **24/7 operation** | Attacks are timed for periods of minimum staffing — nights, weekends, holidays — when manual review capacity is lowest. |

A fourth is structural rather than technical, and it is the most important:
**the highest-volume attack requires no technical compromise at all.** In
authorised push payment fraud the customer is manipulated into issuing a genuine
instruction. Authentication succeeds because the authentication is genuine.

UK Finance recorded 248,070 APP fraud cases in 2025, worth £576.4m — up 19% year
on year, and 32% of all UK payment fraud losses. **66% of cases originated
online and 17% via telecommunications**: 83% of the attack happens somewhere the
institution cannot observe, before the customer ever opens the banking app.

---

## 2. Typologies

### T1 — Authorised Push Payment (APP) fraud

**Mechanism.** The attacker persuades the legitimate account holder to send a
payment. Variants differ in the story, not the structure: purchase scams
(goods never delivered), impersonation (bank, police, tax authority), investment
scams, romance scams, invoice redirection and CEO fraud against businesses,
advance-fee scams.

**What is technically normal about it.** Correct credentials, correct device
often, correct customer, genuine intent to send. Every authentication control
passes.

**Detectable signals.** Not in the authentication, but in the *shape* of the
payment relative to the payer's history: an unusually large amount, a
first-ever counterparty, an atypical hour, urgency reflected in velocity, a
channel the payer does not normally use. Individually weak; jointly meaningful.

| Layer | Contribution |
|---|---|
| **1 — Identity** | Weak. Nothing is wrong with the identity. Device/channel anomalies help only in the subset where the victim was moved to an unfamiliar channel. |
| **2 — Behavioral** | **Primary.** Deviation from the payer's own baseline is the main available signal. |
| **3 — Network** | **Strong when populated.** Destination accounts are frequently reused across victims; a receiver already flagged by Layer 5 is the single highest-value signal. |
| 4 — Enrichment | Shared registries and consortium reporting on known scam-receiving accounts. |
| 5 — Post-settlement | Detects the receiving account's fan-in pattern, which protects the *next* victim. |

**If Layer 3 is absent:** each victim is evaluated as if they were the first,
even when the institution has already settled twenty payments into the same
account that week. This is the most costly single gap in a typical instant-payment
control stack.

---

### T2 — Mule networks

**Mechanism.** Proceeds must be collected and moved. A receiving account
accumulates funds from multiple unrelated payers (**fan-in**), then disperses
them across further accounts (**fan-out**), typically within minutes, before
cashing out.

**Why the receiving institution now cares.** Under the UK reimbursement regime
in force since 7 October 2024, reimbursement cost is **split equally between
sending and receiving firms**. Hosting mule accounts carries a direct, measurable
cost. See [`false-positive-model.md`](false-positive-model.md).

**Detectable signals.** Invisible in any single transaction; unmistakable across
the set. Many unrelated senders to one recipient in a short window; a recently
opened account with immediate high inbound volume; inbound and outbound totals
that nearly match, with short dwell time.

| Layer | Contribution |
|---|---|
| 1 — Identity | Account age and verification tier on the receiving side. |
| 2 — Behavioral | Detects the fan-out phase from the mule account's own payer perspective. |
| **3 — Network** | Consumes the tier that Layer 5 assigned. |
| 4 — Enrichment | Externally reported mule accounts. |
| **5 — Post-settlement** | **Primary.** Fan-in, fan-out and dwell-time detection over settled history. |

**This is the typology the framework's feedback loop is built for.** The
detection is expensive, cross-transactional, and impossible in-path — and its
output is a single pre-computed value that Layer 3 reads at negligible cost.
See [`fraud-control-layers.md`](fraud-control-layers.md#layer-5--post-settlement-analysis).

---

### T3 — Account takeover (ATO)

**Mechanism.** The attacker obtains control of a genuine account — credential
stuffing, phishing, malware, SIM swap to defeat SMS-based authentication,
social engineering of support staff — and then pushes funds out fast.

**Detectable signals.** Unrecognized device; channel change; credential or
contact-detail change shortly before a payment; payment behavior sharply
discontinuous with the account's history; rapid sequence of payments to new
destinations.

| Layer | Contribution |
|---|---|
| **1 — Identity** | **Primary.** Unknown device, channel anomaly, recent contact-detail change, posture flags. |
| **2 — Behavioral** | **Primary.** Post-takeover behavior is usually a sharp discontinuity — the attacker does not know the baseline they need to imitate. |
| 3 — Network | Destinations are often known mule accounts. |
| 4 — Enrichment | Device reputation, compromised-credential feeds. |
| 5 — Post-settlement | Fan-out burst detection. |

**Note on the timing dependency.** The most valuable ATO signal — "the contact
email changed 40 minutes ago" — is a *profile* fact. It must be pushed into the
account profile that Layer 1 reads, not queried at decision time. An institution
whose profile refresh lags by hours has a control that works in principle and
misses in practice.

---

### T4 — Synthetic identity

**Mechanism.** An account is opened using a fabricated identity, often combining
real and invented attributes. It is then operated normally to build a plausible
history before being used — for fraud directly, or as a durable mule.

**Detectable signals.** Weak at the transaction layer by construction; the
identity was designed to pass. The signals live at onboarding and in
longitudinal behavior: history that is thin or oddly uniform, activity
consistent with building a profile rather than living a life, correlation with
other accounts sharing device or contact attributes.

| Layer | Contribution |
|---|---|
| **1 — Identity** | Account age, verification depth, history sufficiency. |
| 2 — Behavioral | Weak — the baseline is genuine, just manufactured. |
| 3 — Network | Correlation with other synthetic accounts, once Layer 5 has established it. |
| **4 — Enrichment** | **Primary.** Identity verification services and cross-institution correlation. |
| 5 — Post-settlement | Cross-account correlation over time. |

**Honest limitation.** This typology is substantially an *onboarding* problem,
and a transaction-time framework mitigates it only partially. The assessment
model treats onboarding controls as a distinct category rather than pretending
transaction monitoring covers it.

---

### T5 — Structuring / threshold evasion

**Mechanism.** Splitting a transfer into several payments sized just below a
known reporting, review or control threshold.

**Detectable signals.** Sequences of similar amounts clustered just below a
round or regulatory boundary; a total that would have triggered a control had it
moved as one payment.

| Layer | Contribution |
|---|---|
| 1 — Identity | Minimal. |
| 2 — Behavioral | Velocity rules catch the burst; a single payment in the sequence looks unremarkable. |
| 3 — Network | Repeated payments to the same destination. |
| 4 — Enrichment | Regulatory threshold reference data. |
| **5 — Post-settlement** | **Primary.** The just-below-threshold sequence detector. |

**Why this typology is a good test of a control design.** Structuring exists
*because* controls have thresholds. Any absolute threshold you publish, enforce
consistently, and never vary becomes a target to sit underneath. This is the
strongest argument for baseline-relative rules over absolute limits: a threshold
derived from the payer's own history is not knowable by the attacker.

---

## 3. Coverage matrix

Primary mitigation in **bold**; supporting contribution marked `+`.

| Typology | L1 Identity | L2 Behavioral | L3 Network | L4 Enrichment | L5 Post-settlement |
|---|---|---|---|---|---|
| T1 APP fraud | + | **primary** | **primary** | + | + |
| T2 Mule networks | + | + | **primary** | + | **primary** |
| T3 Account takeover | **primary** | **primary** | + | + | + |
| T4 Synthetic identity | + | — | + | **primary** | + |
| T5 Structuring | — | + | + | + | **primary** |

### Reading the matrix

**No typology is mitigated by a single layer.** Any control stack built around
one layer has a corresponding blind spot: behavioral-only misses mule networks
(the mule's own behavior is consistent), identity-only misses APP fraud
(identity is genuine), network-only misses first-contact attacks (no prior
signal exists).

**Layers 3 and 5 carry the receiving side.** Three of five typologies have
Layer 5 as a primary mitigation, and Layer 5 acts only through Layer 3. An
institution that has implemented Layers 1–2 and stopped has built the half of
the framework that protects against attacks on its own customers, and none of
the half that addresses the accounts it hosts — which, since October 2024 in the
UK, is half its financial exposure.

**Layer 4 is primary for exactly one typology** and supporting for all others.
It is genuinely valuable and genuinely not urgent, which is why it is
asynchronous.

---

## 4. What this framework does not address

Stated so the gaps are not mistaken for coverage:

- **Onboarding and identity proofing.** Referenced by T4 and assessed as a
  category, but not implemented. Where synthetic identity is defeated.
- **Customer-side channel security.** Device malware, SIM swap execution,
  phishing site takedown.
- **Consumer education.** UK Finance data puts 83% of APP case origination
  outside the banking channel; education addresses that surface, and no
  transaction control substitutes for it.
- **Cross-institution intelligence sharing.** Modeled as an integration point
  (Layer 4) with a simulated registry. The framework specifies the interface;
  it does not provide a network.
- **Sanctions and AML screening.** Adjacent regulatory obligation with its own
  requirements, deliberately out of scope. Where it shares the payment path,
  it is subject to the same in-path constraints as anything else.

---

## 5. Sources

- UK Finance, *Annual Fraud Report 2026* (2025 data) — [press release](https://www.ukfinance.org.uk/news-and-insight/press-release/fraud-report-2026-press-release)
- UK Payment Systems Regulator, *APP fraud reimbursement protections* — [consumer information](https://www.psr.org.uk/information-for-consumers/app-fraud-reimbursement-protections/)
- Federal Reserve Financial Services, *FedNow Service Readiness Guide: Managing Fraud Risk* — [PDF](https://explore.fednow.org/resources/fraud-at-a-glance.pdf)
- Banco Central do Brasil, *Guia de implementação dos procedimentos de devolução (MED)* — [PDF](https://www.bcb.gov.br/content/estabilidadefinanceira/pix/Guia_MED.pdf)
