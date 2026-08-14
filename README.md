# IPRF — Instant Payment Fraud & Resilience Framework

IPRF is an open-source assessment framework and reference implementation for fraud
prevention and operational resilience in **irrevocable instant-payment systems**
(FedNow, Pix and similar rails). Because an instant payment settles in seconds and
cannot be recalled, fraud controls cannot rely on the days-long investigation windows
that card and ACH systems assume — every control must be classified up front as either
deterministic and in-path, or pre-computed and asynchronous. IPRF makes that
classification explicit across five control layers, pairs it with a maturity model an
institution can self-assess against, and ships a working decision engine that produces
explainable, reproducible, auditable decisions with no black-box scoring.

> Status: Phase 1 of 6 — in development. All data in this repository is
> **SYNTHETIC / DEMO DATA**. The full README lands with v1.0.0.

## Repository layout

| Path | Contents |
|---|---|
| `backend/` | Gradle multi-module Java 21 / Spring Boot modular monolith |
| `frontend/` | Next.js 14 app (landing, docs, dashboard) |
| `docs/framework/` | The methodology documents — the framework specification itself |
| `specs/` | Phase specs for development sessions |

## Quick start

```bash
cp .env.example .env      # local development values, synthetic data only
docker compose up -d      # PostgreSQL 16, Redis 7, RabbitMQ

cd backend && ./gradlew build
cd ../frontend && npm ci && npm run dev
```

## License

[Apache-2.0](LICENSE)
