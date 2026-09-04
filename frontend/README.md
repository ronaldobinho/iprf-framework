# IPRF frontend

Next.js 14 (App Router, TypeScript, Tailwind), exported as a fully static site.

The landing page and the decision simulator run with **no backend at all**. That
is deliberate: the public demonstration has to be real rather than a screenshot,
and it must not depend on a service being up.

```bash
npm ci
npm run dev      # http://localhost:3000
npm run build    # static export to out/
npm test         # simulator unit and boundary tests
npm run lint
```

## Generated files

Two directories are produced at build time and are **git-ignored**. Editing them
does nothing; they are overwritten on every `dev`, `build`, `lint` and `test`.

| Path | Generated from | By |
|---|---|---|
| `src/simulator/generated/rules.ts` | `backend/risk-engine/src/main/resources/application-rules.yml` | `scripts/generate-rules.mjs` |
| `content/framework/` | `docs/framework/*.md` | `scripts/sync-docs.mjs` |

The rule constants are generated rather than copied so the browser simulator and
the Java engine cannot drift apart: change a threshold in the YAML and the
simulator changes with it, or the build fails. The methodology documents are
copied rather than read across the directory boundary so the build works
regardless of which directory a host treats as the project root.

## The simulator

`src/simulator/` is a faithful port of the in-path engine — Layers 1 to 3, score
composition, decision policy and explanation building.

Amounts use `src/simulator/decimal.ts`, a minimal `BigDecimal` equivalent over
`BigInt`, rather than JavaScript numbers. Both amount rules compare with `>=`, so
exact equality at a threshold *fires* the rule; that boundary is precisely where
a float port would diverge from the engine. The boundary cases are asserted in
`src/simulator/__tests__/pipeline.test.ts`.

## Deploying

The output in `out/` is plain static files. On Vercel, set the project root to
`frontend` — the build scripts reach up to `../backend` and `../docs`, so the
whole repository must be present in the build context.

Set `SITE.url` in `src/lib/site.ts` once a domain is registered; it is used for
canonical URLs and the sitemap.
