/**
 * Site-wide constants.
 *
 * Everything a reader could act on lives here, so changing a contact address or
 * adding a profile link is one edit rather than a search across components.
 */

export const SITE = {
  name: "IPRF",
  fullName: "Instant Payment Fraud & Resilience Framework",
  tagline: "Secure every instant payment.",
  description:
    "An open-source assessment framework and reference implementation for fraud prevention and resilience in irrevocable instant-payment systems.",

  /** Set once a domain is registered; used for canonical URLs and the sitemap. */
  url: "https://iprf.dev",

  githubUrl: "https://github.com/ronaldobinho/iprf-framework",
  contactEmail: "ronaldobinho@gmail.com",

  /**
   * Optional. The contact section renders the link only when this is set, so
   * an empty string is a supported state rather than a broken anchor.
   */
  linkedInUrl: "",

  license: "Apache-2.0",
  licenseUrl: "https://www.apache.org/licenses/LICENSE-2.0",
  frameworkVersion: "0.1.0-SNAPSHOT",
} as const;

export function mailtoAssessment(): string {
  const subject = encodeURIComponent("IPRF assessment enquiry");
  const body = encodeURIComponent(
    [
      "Institution:",
      "Rail(s) in scope (FedNow / Pix / Faster Payments / other):",
      "Role on the rail (sending / receiving / both):",
      "What prompted this:",
      "",
    ].join("\n"),
  );
  return `mailto:${SITE.contactEmail}?subject=${subject}&body=${body}`;
}
