# IPRF frontend

Next.js 14 (App Router, TypeScript, Tailwind). Currently a scaffold — the landing
page, documentation renderer, dashboard and client-side simulator are built in
Phase 5.

```bash
nvm use          # Node version pinned in .nvmrc
npm ci
npm run dev      # http://localhost:3000
npm run build    # production build
npm run lint
```

The landing page and simulator are designed to run on Vercel with **no backend
dependency**: the simulator is a TypeScript port of the deterministic rule set,
with thresholds generated from the backend's `application-rules.yml` at build
time so the two implementations cannot silently diverge.
