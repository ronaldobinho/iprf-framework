import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { Markdown } from "@/components/docs/Markdown";
import { getAllDocs, getDoc } from "@/lib/docs";
import { SITE } from "@/lib/site";

interface Params {
  params: { slug: string };
}

export function generateStaticParams() {
  return getAllDocs().map((doc) => ({ slug: doc.slug }));
}

export function generateMetadata({ params }: Params): Metadata {
  const doc = getDoc(params.slug);
  if (!doc) return {};
  return {
    title: doc.title,
    description: doc.summary || SITE.description,
    openGraph: { title: `${doc.title} — ${SITE.name}`, description: doc.summary },
  };
}

export default function DocPage({ params }: Params) {
  const doc = getDoc(params.slug);
  if (!doc) notFound();

  const docs = getAllDocs();
  const index = docs.findIndex((entry) => entry.slug === doc.slug);
  const previous = index > 0 ? docs[index - 1] : undefined;
  const next = index < docs.length - 1 ? docs[index + 1] : undefined;

  return (
    <div className="px-6 py-12 sm:py-16">
      <div className="mx-auto w-full max-w-prose">
        <nav aria-label="Breadcrumb" className="text-sm">
          <Link href="/methodology" className="text-fg-dim transition-colors hover:text-accent">
            &larr; Methodology
          </Link>
        </nav>

        <header className="mt-6 border-b border-edge pb-8">
          <h1 className="text-balance text-3xl font-semibold tracking-tight text-fg">
            {doc.title}
          </h1>
          <p className="mt-4 font-mono text-2xs text-fg-dim">
            Rendered from{" "}
            <a
              href={`${SITE.githubUrl}/blob/main/docs/framework/${doc.slug}.md`}
              rel="noreferrer noopener"
              className="text-fg-muted hover:text-accent"
            >
              docs/framework/{doc.slug}.md
            </a>
          </p>
        </header>

        <article className="mt-8">
          <Markdown source={doc.body} />
        </article>

        <nav
          aria-label="Document navigation"
          className="mt-16 flex flex-col gap-3 border-t border-edge pt-8 sm:flex-row sm:justify-between"
        >
          {previous ? (
            <Link
              href={`/methodology/${previous.slug}`}
              className="group max-w-[18rem] text-sm text-fg-muted transition-colors hover:text-fg"
            >
              <span className="block text-2xs uppercase tracking-wide text-fg-dim">
                Previous
              </span>
              <span className="mt-1 block">&larr; {previous.title}</span>
            </Link>
          ) : (
            <span />
          )}
          {next ? (
            <Link
              href={`/methodology/${next.slug}`}
              className="group max-w-[18rem] text-sm text-fg-muted transition-colors hover:text-fg sm:text-right"
            >
              <span className="block text-2xs uppercase tracking-wide text-fg-dim">Next</span>
              <span className="mt-1 block">{next.title} &rarr;</span>
            </Link>
          ) : null}
        </nav>
      </div>
    </div>
  );
}
