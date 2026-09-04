/**
 * Reads the methodology documents at build time.
 *
 * The documents in docs/framework are the framework specification itself and
 * remain the single source of truth — specs/phase-5.md forbids duplicating
 * their content into React. A prebuild step copies them into content/framework
 * so the build does not depend on which directory the host treats as the
 * project root.
 */
import { readFileSync, readdirSync } from "node:fs";
import { join } from "node:path";

const CONTENT_DIR = join(process.cwd(), "content", "framework");

export interface FrameworkDoc {
  slug: string;
  title: string;
  /** First paragraph after the H1, used as the card summary. */
  summary: string;
  body: string;
  hasDiagrams: boolean;
}

/**
 * Reading order, not alphabetical order. A reader arriving at the methodology
 * index should be able to start at the top and have each document make sense.
 */
const READING_ORDER = [
  "methodology",
  "fraud-control-layers",
  "latency-model",
  "false-positive-model",
  "threat-model",
  "architecture",
  "assessment-model",
  "maturity-model",
  "resilience-model",
  "growth-coupling",
  "terminology",
];

function parse(slug: string, raw: string): FrameworkDoc {
  const lines = raw.split("\n");
  const headingIndex = lines.findIndex((line) => line.startsWith("# "));
  const title = headingIndex >= 0 ? lines[headingIndex].replace(/^#\s+/, "").trim() : slug;

  // First non-empty, non-heading, non-blockquote, non-table line after the H1.
  let summary = "";
  for (const line of lines.slice(headingIndex + 1)) {
    const trimmed = line.trim();
    if (!trimmed) continue;
    if (/^[#>|*\-`]/.test(trimmed)) continue;
    summary = trimmed.replace(/\*\*/g, "").replace(/\[([^\]]+)\]\([^)]+\)/g, "$1");
    break;
  }

  return {
    slug,
    title,
    summary,
    body: raw,
    hasDiagrams: raw.includes("```mermaid"),
  };
}

export function getAllDocs(): FrameworkDoc[] {
  const files = readdirSync(CONTENT_DIR).filter((name) => name.endsWith(".md"));
  const docs = files.map((name) => parse(name.replace(/\.md$/, ""), readFileSync(join(CONTENT_DIR, name), "utf8")));

  return docs.sort((a, b) => {
    const left = READING_ORDER.indexOf(a.slug);
    const right = READING_ORDER.indexOf(b.slug);
    // Anything not in the list sorts last rather than disappearing, so a new
    // document is visible even before someone places it.
    return (left === -1 ? 999 : left) - (right === -1 ? 999 : right);
  });
}

export function getDoc(slug: string): FrameworkDoc | undefined {
  return getAllDocs().find((doc) => doc.slug === slug);
}
