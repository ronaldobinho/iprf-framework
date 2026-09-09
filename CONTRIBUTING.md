# Contributing to IPRF

Thanks for looking. This is a reference implementation of a methodology, which makes one kind of contribution unusually valuable: **telling us where the methodology is wrong.** If you assess fraud controls on an instant-payment rail and something in [`docs/framework`](docs/framework) is mistaken, contradicted by a primary source, or missing, an issue saying so is worth more than a pull request tidying code.

## Before you start

- **Small fix?** Open a pull request.
- **Anything larger, or any change to the methodology?** Open an issue first. The documents are cited to primary sources and changing them is a research task, not a wording task.
- **Security vulnerability?** Do not open an issue. See [`SECURITY.md`](SECURITY.md).

## Getting set up

Requirements: **JDK 21**, **Node 22**, **Docker**.

```bash
cp .env.example .env          # local values, synthetic data only
docker compose up -d          # PostgreSQL 16, Redis 7, RabbitMQ

cd backend && ./gradlew build         # compile + tests
./gradlew :transaction-api:bootRun    # http://localhost:8080

cd ../frontend && npm ci && npm run dev
```

## Before you open a pull request

```bash
cd backend  && ./gradlew build
cd frontend && npm run lint && npm run typecheck && npm run build
```

CI runs the same things. A red build will not be reviewed.

## The rules that are not negotiable

These exist because breaking them quietly is how this kind of system fails in production rather than in review.

**1. Nothing new goes in the authorization path that is not deterministic and bounded.**

Layers 1–3 run in-path. They may read pre-computed state; they may not perform a live database query, call an external service, or do anything whose duration depends on something outside the process. If a control needs a lookup, it belongs in Layer 4 or 5, feeding *future* decisions. An ArchUnit rule guards the boundary and will fail your build.

**2. Thresholds live in configuration, not in code.**

Rule constants belong in [`application-rules.yml`](backend/risk-engine/src/main/resources/application-rules.yml). A number hardcoded in a rule cannot be tuned, audited, or diffed.

**3. Decisions stay explainable.**

Every decision carries the rules that fired, their versions, and each one's contribution. A change that improves accuracy while making the outcome harder to explain is a change this project rejects. Machine learning is an extension point, never a dependency of the decision path.

**4. Event handlers are idempotent, and tested as such.**

Duplicate delivery is normal, not exceptional. So are timeouts, dependency failure and stale risk state. Tests for those cases are part of the change, not a follow-up.

**5. False positives count.**

A change that raises the detection rate while raising the false-positive rate has not obviously improved anything. Say what it does to both.

**6. No fabricated numbers, ever.**

No invented benchmark, adoption figure, client, employer or production metric — in code, documentation, commit messages or the site. Demo figures are labelled `SYNTHETIC / DEMO DATA`. A latency target is labelled a target. If a number cannot be reproduced by a reader, it does not go in.

## Commits

Conventional Commits, and the body carries the reasoning:

```
feat: add velocity rule for repeated new-counterparty transfers

Explain why the change is needed and what it costs. A reviewer six months
from now needs the reasoning far more than a restatement of the diff.
```

Prefixes in use: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `perf`.

## Documentation changes

The eleven documents in [`docs/framework`](docs/framework) are the framework. The website renders them directly — `frontend/content/` is a generated copy and editing it does nothing.

Claims are grounded in the public record and cited to primary sources: regulator publications, operator manuals, published rules. "Industry practice suggests" is not a citation. If you cannot cite it, write it as reasoning and mark it as such.

The distinction the project maintains everywhere: **(A)** implemented here, **(B)** methodology from experience, **(C)** historical case study, **(D)** roadmap. Do not blur them.

## Tests

```bash
cd backend && ./gradlew test
```

New rules need tests for the decision boundary, not only the happy path — the transaction just under the threshold and the one just over. New event handlers need a duplicate-delivery test.

The frontend has no test suite at present; `npm test` runs with `--passWithNoTests`.

## Licence

By contributing you agree your contribution is licensed under [Apache-2.0](LICENSE), the same as the project.
