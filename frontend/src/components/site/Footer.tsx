import Link from "next/link";
import { GitHubIcon } from "@/components/landing/icons";
import { SITE } from "@/lib/site";

/**
 * Footer.
 *
 * The reference layout carries a longer link list — Use Cases, Roadmap, About,
 * Contribute, Privacy, Terms, Code of Conduct. Those pages do not exist yet, and
 * a dead link on a page whose entire argument is verifiability costs more than
 * an empty column slot. They go in when their destinations do; CONTRIBUTING.md
 * and CODE_OF_CONDUCT.md are already scheduled for phase 6, session 6.2.
 */

const COLUMNS = [
  {
    title: "Product",
    links: [
      { href: "/#how-it-works", label: "How It Works", external: false },
      { href: "/#architecture", label: "Control Layers", external: false },
      { href: "/#performance", label: "Performance", external: false },
      { href: "/#integrate", label: "Integration", external: false },
    ],
  },
  {
    title: "Resources",
    links: [
      { href: "/methodology", label: "Documentation", external: false },
      { href: SITE.githubUrl, label: "GitHub Repository", external: true },
      {
        href: `${SITE.githubUrl}/tree/main/docs/framework`,
        label: "Methodology Source",
        external: true,
      },
      { href: SITE.licenseUrl, label: SITE.license, external: true },
    ],
  },
  {
    title: "Contact",
    links: [{ href: `mailto:${SITE.contactEmail}`, label: "Get in Touch", external: true }],
  },
] as const;

export function Footer() {
  return (
    <footer className="border-t border-edge px-6 py-14">
      <div className="mx-auto w-full max-w-content">
        <div className="grid gap-10 sm:grid-cols-2 lg:grid-cols-[minmax(0,1.4fr)_repeat(3,minmax(0,1fr))]">
          <div className="max-w-sm">
            <Link
              href="/"
              className="flex items-center gap-2.5 text-base font-semibold tracking-tight text-fg"
            >
              <span aria-hidden className="h-2 w-2 rounded-full bg-accent" />
              <span>
                {SITE.name}
                <span aria-hidden className="text-accent">
                  .
                </span>
              </span>
            </Link>
            <p className="mt-4 text-sm leading-relaxed text-fg-muted">{SITE.fullName}</p>
            <p className="mt-2 text-sm leading-relaxed text-fg-muted">
              Open source. Built for a safer payments ecosystem.
            </p>
            <a
              href={SITE.githubUrl}
              rel="noreferrer noopener"
              aria-label="IPRF on GitHub"
              className="mt-5 inline-flex rounded-lg border border-edge-strong p-2 text-fg-muted transition-colors hover:border-accent-dim hover:text-fg"
            >
              <GitHubIcon className="h-4 w-4" />
            </a>
          </div>

          {COLUMNS.map((column) => (
            <div key={column.title}>
              <p className="font-mono text-2xs uppercase tracking-[0.16em] text-fg-dim">
                {column.title}
              </p>
              <ul className="mt-4 space-y-2.5">
                {column.links.map((link) => (
                  <li key={link.href}>
                    {link.external ? (
                      <a
                        href={link.href}
                        rel="noreferrer noopener"
                        className="text-sm text-fg-muted transition-colors hover:text-fg"
                      >
                        {link.label}
                      </a>
                    ) : (
                      <Link
                        href={link.href}
                        className="text-sm text-fg-muted transition-colors hover:text-fg"
                      >
                        {link.label}
                      </Link>
                    )}
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>

        <div className="mt-12 flex flex-col gap-3 border-t border-edge pt-6 sm:flex-row sm:items-center sm:justify-between">
          <p className="text-2xs text-fg-dim">
            © 2026 {SITE.name}. Open source software for a safer financial future.
          </p>
          <p className="text-2xs text-fg-dim">
            Everything demonstrated here runs on synthetic data and describes no real institution.
          </p>
        </div>
      </div>
    </footer>
  );
}
