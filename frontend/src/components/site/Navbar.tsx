"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { NAV_LINKS } from "@/components/landing/content";
import { ArrowRightIcon, GitHubIcon } from "@/components/landing/icons";
import { SITE } from "@/lib/site";

/**
 * The navbar is transparent over the hero and picks up a translucent dark
 * background once the page has moved. The state flips on an IntersectionObserver
 * watching a zero-height sentinel rather than on a scroll listener, so there is
 * no handler running on the scroll thread for a purely cosmetic change.
 */
export function Navbar() {
  const [scrolled, setScrolled] = useState(false);
  const [open, setOpen] = useState(false);

  useEffect(() => {
    const sentinel = document.getElementById("nav-sentinel");
    if (!sentinel) return;
    const observer = new IntersectionObserver(
      ([entry]) => setScrolled(!entry.isIntersecting),
      { threshold: 0 },
    );
    observer.observe(sentinel);
    return () => observer.disconnect();
  }, []);

  return (
    <header
      className={`sticky top-0 z-50 transition-colors duration-300 ${
        scrolled ? "border-b border-edge bg-ink/80 backdrop-blur-md" : "border-b border-transparent"
      }`}
    >
      <div className="mx-auto flex h-16 w-full max-w-content items-center justify-between gap-6 px-6">
        <Link
          href="/"
          className="flex items-center gap-2.5 text-base font-semibold tracking-tight text-fg"
        >
          <span aria-hidden className="h-2 w-2 rounded-full bg-accent shadow-[0_0_10px_2px_rgba(32,224,124,0.6)]" />
          <span>
            {SITE.name}
            <span aria-hidden className="text-accent">
              .
            </span>
          </span>
        </Link>

        <nav className="hidden items-center gap-7 md:flex" aria-label="Primary">
          {NAV_LINKS.map((item) => (
            <Link
              key={item.href}
              href={item.href}
              className="text-sm text-fg-muted transition-colors hover:text-fg"
            >
              {item.label}
            </Link>
          ))}
        </nav>

        <div className="flex items-center gap-3">
          <a
            href={SITE.githubUrl}
            rel="noreferrer noopener"
            aria-label="IPRF on GitHub"
            className="rounded-lg p-2 text-fg-muted transition-colors hover:text-fg"
          >
            <GitHubIcon className="h-5 w-5" />
          </a>
          <a
            href={`mailto:${SITE.contactEmail}`}
            className="hidden items-center gap-2 rounded-lg bg-accent px-4 py-2 text-sm font-semibold text-ink transition-colors hover:bg-accent-strong sm:inline-flex"
          >
            Get in Touch
            <ArrowRightIcon className="h-3.5 w-3.5" />
          </a>
          <button
            type="button"
            onClick={() => setOpen((value) => !value)}
            aria-expanded={open}
            aria-controls="mobile-nav"
            className="rounded-lg border border-edge-strong p-2 text-fg-muted transition-colors hover:text-fg md:hidden"
          >
            <span className="sr-only">{open ? "Close menu" : "Open menu"}</span>
            <svg viewBox="0 0 24 24" className="h-4 w-4" fill="none" stroke="currentColor" strokeWidth={1.8} strokeLinecap="round" aria-hidden>
              {open ? (
                <>
                  <path d="m6 6 12 12" />
                  <path d="M18 6 6 18" />
                </>
              ) : (
                <>
                  <path d="M4 7h16" />
                  <path d="M4 12h16" />
                  <path d="M4 17h16" />
                </>
              )}
            </svg>
          </button>
        </div>
      </div>

      <div
        id="mobile-nav"
        hidden={!open}
        className="border-t border-edge bg-ink/95 backdrop-blur-md md:hidden"
      >
        <nav className="mx-auto flex w-full max-w-content flex-col px-6 py-3" aria-label="Primary, mobile">
          {NAV_LINKS.map((item) => (
            <Link
              key={item.href}
              href={item.href}
              onClick={() => setOpen(false)}
              className="border-b border-edge py-3 text-sm text-fg-muted last:border-0 hover:text-fg"
            >
              {item.label}
            </Link>
          ))}
          <a
            href={`mailto:${SITE.contactEmail}`}
            className="mt-3 inline-flex items-center justify-center gap-2 rounded-lg bg-accent px-4 py-2.5 text-sm font-semibold text-ink sm:hidden"
          >
            Get in Touch
            <ArrowRightIcon className="h-3.5 w-3.5" />
          </a>
        </nav>
      </div>
    </header>
  );
}
