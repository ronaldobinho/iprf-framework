# Security Policy

## Reporting a vulnerability

**Please do not open a public issue for a security vulnerability.**

Report it privately through one of these:

1. **GitHub private advisory** (preferred) — [open a draft security advisory](https://github.com/ronaldobinho/iprf-framework/security/advisories/new). This keeps the report and the discussion private until a fix is available.
2. **Email** — ronaldobinho@gmail.com, with `IPRF security` in the subject.

Please include enough for the issue to be reproduced: affected version or commit, the component, the steps, and what an attacker gains. A proof of concept helps; it is not required.

## What to expect

| Stage | Target |
|---|---|
| Acknowledgement of your report | 3 business days |
| Initial assessment and severity | 10 business days |
| Fix or documented mitigation | Depends on severity, communicated in the assessment |

This is a small open-source project, not a vendor with an on-call security team. These are honest targets rather than a contractual SLA, and you will be told if something slips.

Credit is given in the advisory and the release notes unless you would rather stay anonymous.

## Scope

**In scope** — anything in this repository: the decision engine and its rules, the event handlers, the audit trail, the assessment engine, the HTTP boundary, the frontend, the build and CI configuration, and the dependency surface.

Also in scope, and specifically welcome: **a way to make the decision engine produce a wrong or non-deterministic decision.** A fraud engine that can be induced to allow what it should decline is a security problem even when no memory is corrupted and no data leaks.

**Out of scope**

- The synthetic data itself. Everything in this repository is fabricated; there is no real account, customer or institution to expose.
- The seeded local demo profiles, which are demo data by design and disabled outside local profiles.
- Findings that require an attacker to already control the host or the configuration.
- Missing hardening on a local development compose file that is not intended for production.

## Deployment note

This is a **reference implementation**, not a production fraud service. If you adapt it, treat the following as your responsibility rather than assuming this repository handled them: authentication and authorisation, which the open core does not implement; rate limiting on public endpoints; network isolation; secret management beyond environment variables; and retention and residency for anything you store that is not synthetic.

## Related

The fraud typologies the framework defends against are documented openly in [`docs/framework/threat-model.md`](docs/framework/threat-model.md). That is deliberate — a control you cannot describe is a control you cannot assess — and it is a threat model for the payment rail, not a security threat model for this codebase.
