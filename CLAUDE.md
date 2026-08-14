# IPRF — Instant Payment Fraud & Resilience Framework

You are a senior staff engineer building a production-quality, open-source reference
implementation of an assessment framework for fraud prevention and resilience in
irrevocable instant-payment systems (FedNow, Pix and similar rails).

This file contains only durable principles and conventions. Detailed work for the
current phase lives in `/specs/phase-N.md` — load ONLY the spec for the phase you
are executing. Do not load all specs at once.

## Project identity

- Name: **IPRF** (always this spelling — never "IRPF").
- Greenfield repository. There is no legacy code to preserve.
- The open core must be independently useful: an institution clones it, understands
  the methodology, runs a sample assessment and simulations without any commercial
  service. A SaaS layer may exist later; never implement billing or tenancy in v1.

## The one non-negotiable architectural principle

> Decide before the transaction arrives what can be evaluated in-path, and what
> must be pre-computed or evaluated asynchronously.

- **SYNC / IN-PATH** (Layers 1–2): deterministic, bounded latency, pre-computed
  state only. NO live database queries during transaction authorization. Reject
  any rule design that violates this.
- **ASYNC** (Layers 4–5): enrichment, external intelligence, heavy analytics,
  post-settlement analysis. Feeds FUTURE decisions; never blocks the payment path.
- Layer 3 (counterparty/network) reads pre-computed risk state (Redis), never
  synchronous lookups against the primary database.

## Five control layers

1. Identity & Account Posture — account age, verification, device, channel, history
2. Real-Time Behavioral Scoring — amount, counterparty, timing, channel, velocity (IN-PATH, strict latency budget)
3. Counterparty & Network Signals — pre-computed risk state
4. External Enrichment — async by default, feeds future decisions
5. Post-Settlement Analysis — pattern detection, typologies, feedback loop

## Stack (do not deviate without a documented reason)

- Backend: Java 21, Spring Boot 3.x, Gradle multi-module, **modular monolith**
  (no microservices), event-driven internally (RabbitMQ), PostgreSQL 16, Redis 7.
- Frontend: Next.js 14 App Router, TypeScript, Tailwind. Static-exportable landing.
- Client-side simulator: TypeScript port of the deterministic rule set (runs on
  Vercel with no backend dependency).
- Infra: Docker Compose for local; production = Vercel (frontend) + Docker on
  Hostinger VPS behind nginx + Let's Encrypt (backend/demo API).
- CI: GitHub Actions — build, test, dependency scan on every push.

## Repository layout

```
backend/            Gradle modules: transaction-api, risk-engine, risk-state,
                    network-risk, external-enrichment, post-settlement,
                    assessment-engine, audit, benchmarks
frontend/           Next.js app (landing, docs, dashboard) + simulator/
docs/framework/     The 11 methodology documents (the framework spec itself)
specs/              Phase specs for Claude Code sessions
```

## Honesty rules (these protect the project's core purpose)

- Never fabricate credentials, employers, clients, adoption, or production metrics.
- All demo numbers are labeled **SYNTHETIC / DEMO DATA**.
- Benchmark results are documented as reproducible output, never as claims.
- The B3 recovery-time case study (~30min startup, ~80% reduction via parallel
  processing, batch validation, removal of sequential dependencies) is presented
  strictly as the founder's historical professional experience — never as a result
  achieved by this repository.
- Distinguish clearly: (A) implemented here, (B) methodology from experience,
  (C) historical case study, (D) roadmap.

## Decision engine contract

`POST /api/v1/transactions/evaluate` → decision ∈ {ALLOW, REVIEW, DECLINE},
riskScore, latencyMs, riskFactors[], layerResults{}, explanation, frameworkVersion.
Deterministic rules, configurable thresholds (config files, not code), explainable,
reproducible, auditable. No AI black box. ML is an extension point only.

## Quality conventions

- Every decision persisted to an immutable audit trail: transaction ID, framework
  version, rules executed + versions, risk factors, decision, timestamp, latency,
  state version, correlation ID.
- Idempotency on all event handlers. Explicit tests: duplicate events, timeouts,
  dependency failure, stale risk state, false-positive scenarios.
- False positives are a first-class concern: a legitimate payment declined is a
  FAILED payment. Track FP rate, detection rate, decline/review/approval rates,
  p50/p95/p99 latency.
- Secrets via environment variables only. Synthetic data only. PII minimization.
- Prefer: simple > complex, explicit > magical, measurable > impressive,
  explainable > black-box, modular > distributed-for-no-reason, working > speculative.

## Session protocol

- Load this file + the single relevant `/specs/phase-N.md`.
- End every session with build green, tests green, commit pushed.
- Never end a session on a broken build.
- Do not start work from a later phase while the current phase gate is unmet.

## Security note for production deploy

The VPS also hosts unrelated production SaaS products. The IPRF demo API must run
in an isolated Docker network, with aggressive rate limiting on public endpoints,
synthetic data only, and no shared volumes or credentials with other services.
