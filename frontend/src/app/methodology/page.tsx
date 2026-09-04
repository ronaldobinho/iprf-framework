import type { Metadata } from "next";
import Link from "next/link";
import { getAllDocs } from "@/lib/docs";
import { SITE } from "@/lib/site";

export const metadata: Metadata = {
  title: "Methodology",
  description:
    "The IPRF framework specification: eleven documents covering the control layers, latency and false-positive models, threat model, architecture, and the assessment and maturity models.",
};

export default function MethodologyIndex() {
  const docs = getAllDocs();

  return (
    <div className="px-6 py-16 sm:py-20">
      <div className="mx-auto w-full max-w-content">
        <p className="font-mono text-2xs uppercase tracking-[0.18em] text-accent">
          Framework specification
        </p>
        <h1 className="mt-4 max-w-3xl text-balance text-3xl font-semibold tracking-tight sm:text-4xl">
          The methodology is the primary artifact. The code demonstrates it.
        </h1>
        <p className="mt-5 max-w-prose text-pretty text-base leading-relaxed text-fg-muted">
          These eleven documents are the framework itself, rendered directly from{" "}
          <code className="rounded border border-edge bg-ink-high px-1.5 py-0.5 font-mono text-[0.85em] text-fg">
            docs/framework
          </code>{" "}
          in the repository — they are not a summary written for a website. Claims are grounded
          in the public record and cited to primary sources.
        </p>

        <ol className="mt-12 grid gap-3 sm:grid-cols-2">
          {docs.map((doc, index) => (
            <li key={doc.slug}>
              <Link
                href={`/methodology/${doc.slug}`}
                className="flex h-full flex-col rounded-lg border border-edge bg-ink-raised p-5 transition-colors hover:border-accent-dim"
              >
                <div className="flex items-baseline gap-3">
                  <span className="tnum font-mono text-2xs text-fg-dim">
                    {String(index + 1).padStart(2, "0")}
                  </span>
                  <span className="flex-1 text-base font-medium text-fg">{doc.title}</span>
                  {doc.hasDiagrams ? (
                    <span className="font-mono text-2xs uppercase tracking-wide text-fg-dim">
                      diagrams
                    </span>
                  ) : null}
                </div>
                <p className="mt-2 text-sm leading-relaxed text-fg-muted">{doc.summary}</p>
              </Link>
            </li>
          ))}
        </ol>

        <p className="mt-10 text-sm text-fg-dim">
          Read them in the repository instead:{" "}
          <a
            href={`${SITE.githubUrl}/tree/main/docs/framework`}
            rel="noreferrer noopener"
            className="text-accent hover:text-accent-strong"
          >
            docs/framework
          </a>
        </p>
      </div>
    </div>
  );
}
