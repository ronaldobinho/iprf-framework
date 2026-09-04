import type { Metadata, Viewport } from "next";
import localFont from "next/font/local";
import Link from "next/link";
import "./globals.css";
import { SITE } from "@/lib/site";

const geistSans = localFont({
  src: "./fonts/GeistVF.woff",
  variable: "--font-geist-sans",
  weight: "100 900",
  display: "swap",
});

const geistMono = localFont({
  src: "./fonts/GeistMonoVF.woff",
  variable: "--font-geist-mono",
  weight: "100 900",
  display: "swap",
});

export const metadata: Metadata = {
  metadataBase: new URL(SITE.url),
  title: {
    default: `${SITE.name} — ${SITE.fullName}`,
    template: `%s — ${SITE.name}`,
  },
  description: SITE.description,
  applicationName: SITE.name,
  keywords: [
    "instant payments",
    "FedNow",
    "Pix",
    "fraud prevention",
    "authorised push payment fraud",
    "APP fraud",
    "payment resilience",
    "fraud decisioning",
  ],
  openGraph: {
    type: "website",
    siteName: SITE.name,
    title: `${SITE.name} — ${SITE.fullName}`,
    description: SITE.description,
    url: SITE.url,
  },
  twitter: { card: "summary_large_image" },
  robots: { index: true, follow: true },
};

export const viewport: Viewport = {
  themeColor: "#070A0F",
  colorScheme: "dark",
};

const NAV = [
  { href: "/#how-it-works", label: "How it works" },
  { href: "/#demo", label: "Live demo" },
  { href: "/methodology", label: "Methodology" },
  { href: "/#assessment", label: "Assessment" },
];

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en" className={`${geistSans.variable} ${geistMono.variable}`}>
      <body className="min-h-screen antialiased">
        <a
          href="#main"
          className="sr-only focus:not-sr-only focus:absolute focus:left-4 focus:top-4 focus:z-50 focus:rounded focus:bg-accent focus:px-4 focus:py-2 focus:text-ink"
        >
          Skip to content
        </a>

        <header className="sticky top-0 z-40 border-b border-edge bg-ink/85 backdrop-blur">
          <div className="mx-auto flex h-14 w-full max-w-content items-center justify-between gap-6 px-6">
            <Link href="/" className="flex items-baseline gap-2.5 font-semibold tracking-tight">
              <span className="text-fg">{SITE.name}</span>
              <span className="hidden font-mono text-2xs uppercase tracking-[0.16em] text-fg-dim sm:inline">
                Instant Payment Fraud &amp; Resilience
              </span>
            </Link>
            <nav className="flex items-center gap-5" aria-label="Primary">
              {NAV.map((item) => (
                <Link
                  key={item.href}
                  href={item.href}
                  className="hidden text-sm text-fg-muted transition-colors hover:text-fg md:inline"
                >
                  {item.label}
                </Link>
              ))}
              <a
                href={SITE.githubUrl}
                className="rounded border border-edge-strong px-3 py-1.5 text-sm text-fg-muted transition-colors hover:border-accent-dim hover:text-fg"
                rel="noreferrer noopener"
              >
                GitHub
              </a>
            </nav>
          </div>
        </header>

        <main id="main">{children}</main>

        <footer className="border-t border-edge px-6 py-12">
          <div className="mx-auto flex w-full max-w-content flex-col gap-6 sm:flex-row sm:items-start sm:justify-between">
            <div className="max-w-md">
              <p className="font-semibold tracking-tight">{SITE.name}</p>
              <p className="mt-2 text-sm leading-relaxed text-fg-muted">
                Open-source under {SITE.license}. Everything demonstrated on this site runs
                against synthetic data and describes no real institution.
              </p>
            </div>
            <div className="flex flex-col gap-2 text-sm">
              <Link href="/methodology" className="text-fg-muted hover:text-fg">
                Methodology
              </Link>
              <a href={SITE.githubUrl} rel="noreferrer noopener" className="text-fg-muted hover:text-fg">
                Source
              </a>
              <a href={SITE.licenseUrl} rel="noreferrer noopener" className="text-fg-muted hover:text-fg">
                {SITE.license}
              </a>
              <a href={`mailto:${SITE.contactEmail}`} className="text-fg-muted hover:text-fg">
                Contact
              </a>
            </div>
          </div>
        </footer>
      </body>
    </html>
  );
}
