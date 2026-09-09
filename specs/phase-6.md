# Phase 6 — Hardening, Benchmarks, Release v1.0.0, Deploy

**Window:** Sep 21–26, 2026 (3–4 sessions)
**Depends on:** Phase 5 gate met.
**Gate:** All 20 acceptance criteria from the original brief (§26) verified;
v1.0.0 tagged; landing live on Vercel; demo API live on VPS.

---

## Session 6.1 — Benchmarks

1. `benchmarks` module: JMH microbenchmarks for rule evaluation + a load harness
   (Gatling or a simple driver) against the evaluate endpoint.
2. Reproducible entry point: `./gradlew benchmark`. Output written to
   `benchmarks/results/` with environment fingerprint (CPU, JVM, container limits).
3. `docs/framework/latency-model.md` updated with MEASURED results, labeled with
   the exact command and environment that produced them. Never present numbers
   the reader cannot reproduce.
4. Timeout budgets asserted in tests: the in-path pipeline must complete within
   the documented budget on the reference scenario, or the test fails.

## Session 6.2 — Repository quality sweep

Per original brief §23:
1. Remove dead code, duplicated code, placeholder UI, fake metrics, core TODOs,
   hardcoded anything. Improve naming and module boundaries where cheap.
2. Add: `SECURITY.md` (reporting process, threat model pointer, dependency
   scanning policy), `CONTRIBUTING.md` (build, test, PR conventions),
   `CODE_OF_CONDUCT.md`, `.env.example` (complete), seed scripts documented.
3. CI extended: dependency vulnerability scan (OWASP dependency-check or Trivy),
   secret scan (gitleaks) — both blocking.
4. License headers where the ecosystem expects them; NOTICE file if needed.

## Session 6.3 — README + final docs

README with the 20 sections from the original brief §19, written for a senior
fintech engineer skimming in 3 minutes:
what/why-instant-fraud-is-different/framework overview/five layers/architecture
diagram (Mermaid)/example transaction/example decision/assessment methodology/
false-positive philosophy/resilience methodology/local install/Docker/tests/
benchmarks/demo/API examples/project structure/security/roadmap/license.

Roadmap section names v1.1 explicitly: scroll-driven landing journey, additional
typology detectors, assessment web wizard. Honest about limitations (synthetic
data only, simulated external registry, no auth in open core).

## Session 6.4 — Release + deploy

1. Tag v1.0.0 (semver from here on). GitHub Release with highlights + sample
   report PDF attached.
2. **Vercel:** static export of the frontend; project connected to the repo;
   production domain attached.
3. **VPS (Hostinger):** `deploy/` directory with production compose file —
   backend + Postgres + Redis + RabbitMQ on an ISOLATED Docker network, nginx
   server block + Let's Encrypt cert, aggressive rate limiting on
   `/api/v1/transactions/evaluate` and assessment endpoints, no volumes or
   credentials shared with the other SaaS products on the machine. Health checks
   wired to compose restart policy.
4. Smoke test checklist post-deploy: landing loads, simulator runs, demo API
   evaluates, dashboard populates, audit endpoint answers.
5. Domain: purchased before this session (candidates: iprf.dev, iprf.io,
   iprf-framework.org — verify availability). DNS: apex/www → Vercel,
   `api.` → VPS.

## Session 6.5 — Acceptance verification

Walk the original brief §26 list 1–20 and record each as PASS with evidence
(command output or screenshot path) in `docs/acceptance-v1.md`. Anything not
PASS is either fixed or explicitly moved to the roadmap with justification —
no silent gaps.

---

## Owner actions this week (Ronaldo)
- Purchase domain; point DNS.
- Ask Deborah Baxley for the supplemental note / public comment on the published
  spec (see specs/README.md — RFE evidence actions).
- LinkedIn announcement post (EN) on release day; capture the URL for the RFE.

## Phase 6 exit checklist
- [ ] `./gradlew benchmark` reproducible; results committed with environment info
- [ ] Vulnerability + secret scans blocking in CI, both green
- [ ] README complete (20 sections); acceptance doc complete
- [ ] v1.0.0 tagged and released
- [ ] Vercel live; VPS demo API live, isolated and rate-limited
- [ ] Evidence freeze inputs ready for Sep 28–Oct 5 window
---

## Deviation record — README, community files and Vercel deploy, Sep 9 2026

Work from this phase was pulled forward, before the Phase 5 gate closed and
before the sessions below ran in order. Recorded here for the same reason as
the record in `phase-5.md`: an out-of-order build with no written reason looks
arbitrary in hindsight.

**Session 6.3 (README) — done early.** The README now carries the §19 structure
in full. Reason: the repository is the destination the site sends technical
readers to, and it was still a stub announcing "Phase 1 of 6" long after phases
1 through 5 had shipped. A stale front page costs more than an out-of-order one.

**Session 6.2 (community files) — partially done early.** `SECURITY.md`,
`CONTRIBUTING.md` and `CODE_OF_CONDUCT.md` are written. The rest of 6.2 remains
outstanding: the dead-code and duplicated-code sweep, dependency vulnerability
scanning and secret scanning as blocking CI steps, licence headers, NOTICE.

**Session 6.4 (deploy) — frontend only, and only prepared.** `vercel.json` at
the repository root defines install, build and output so the frontend builds
with the repository root as the Vercel root directory. That is not cosmetic:
`frontend/scripts/sync-docs.mjs` reads `docs/framework`, which lives above
`frontend/`, and `frontend/content/` is gitignored — with the root directory
set to `frontend/`, the prebuild step cannot see the documents and the deploy
fails. `framework` is null because `next.config.mjs` sets `output: 'export'`;
declaring the Next.js preset while pointing `outputDirectory` at `out/` asks
Vercel for two contradictory things.

The demo API on the VPS is untouched and still owed.

**Not done, still owed from this phase.** Benchmarks (6.1) — the suite has not
been run and no figure anywhere claims a measurement. The remainder of the 6.2
quality sweep. The v1.0.0 tag. The 20 acceptance criteria from §26.

**Also owed, carried from `phase-5.md`.** The dashboard and transaction
explorer (session 5.3), and rebasing `latency-model.md` and
`false-positive-model.md` off the Banco Central sources onto FedNow and RTP
primary sources. Note that `backend/risk-engine/.../Rail.java` still declares a
`PIX` constant; removing rail names from the domain model was not in scope for
the landing work and would be a backend change.
