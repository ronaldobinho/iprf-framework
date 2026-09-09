/**
 * Site-wide constants.
 *
 * Everything a reader could act on lives here, so changing a contact address or
 * adding a profile link is one edit rather than a search across components.
 */

export const SITE = {
  name: "IPRF",
  fullName: "Instant Payment Fraud & Resilience Framework",
  tagline: "Faster payments, safely by design.",
  description:
    "An open-source assessment framework and reference implementation for fraud prevention and resilience in irrevocable instant-payment systems.",

  /**
   * Canonical origin. Used for the sitemap, robots, and Open Graph URLs, so it
   * must be the address the site actually answers on — a value pointing at an
   * unregistered domain silently poisons all three.
   */
  url: "https://iprf-payments.vercel.app",

  githubUrl: "https://github.com/ronaldobinho/iprf-framework",
  issuesUrl: "https://github.com/ronaldobinho/iprf-framework/issues",
  contactEmail: "ronaldobinho@gmail.com",
  linkedInUrl: "https://www.linkedin.com/in/ronaldocarvalho/",

  license: "Apache-2.0",
  licenseUrl: "https://www.apache.org/licenses/LICENSE-2.0",
  frameworkVersion: "0.1.0-SNAPSHOT",
} as const;
