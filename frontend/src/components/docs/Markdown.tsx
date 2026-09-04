import Link from "next/link";
import type { ComponentPropsWithoutRef, ReactNode } from "react";
import ReactMarkdown from "react-markdown";
import rehypeSlug from "rehype-slug";
import remarkGfm from "remark-gfm";
import { Mermaid } from "./Mermaid";

/**
 * Renders a methodology document.
 *
 * A Server Component, so the output is static HTML in the export — the pages
 * are readable with JavaScript disabled, which the Phase 5 gate requires. Only
 * the Mermaid blocks hydrate, and only on the pages that contain one.
 *
 * The documents are written to be read in a repository, so their inter-document
 * links are relative filenames. Those are rewritten to routes here rather than
 * edited in the source, which stays the single source of truth.
 */

function flatten(node: ReactNode): string {
  if (typeof node === "string" || typeof node === "number") return String(node);
  if (Array.isArray(node)) return node.map(flatten).join("");
  if (node && typeof node === "object" && "props" in node) {
    return flatten((node as { props: { children?: ReactNode } }).props.children);
  }
  return "";
}

export function Markdown({ source }: { source: string }) {
  return (
    <div className="text-[0.9375rem] leading-relaxed text-fg-muted">
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        rehypePlugins={[rehypeSlug]}
        components={{
          h1: () => null, // The page renders the title in its own header.
          h2: ({ children, ...props }) => (
            <h2
              {...props}
              className="mt-12 scroll-mt-20 border-t border-edge pt-8 text-xl font-semibold tracking-tight text-fg first:mt-0 first:border-0 first:pt-0"
            >
              {children}
            </h2>
          ),
          h3: ({ children, ...props }) => (
            <h3 {...props} className="mt-8 scroll-mt-20 text-base font-semibold text-fg">
              {children}
            </h3>
          ),
          h4: ({ children, ...props }) => (
            <h4 {...props} className="mt-6 text-sm font-semibold text-fg">
              {children}
            </h4>
          ),
          p: ({ children }) => <p className="mt-4 text-pretty">{children}</p>,
          ul: ({ children }) => (
            <ul className="mt-4 list-disc space-y-2 pl-5 marker:text-fg-dim">{children}</ul>
          ),
          ol: ({ children }) => (
            <ol className="mt-4 list-decimal space-y-2 pl-5 marker:text-fg-dim">{children}</ol>
          ),
          li: ({ children }) => <li className="pl-1">{children}</li>,
          strong: ({ children }) => (
            <strong className="font-semibold text-fg">{children}</strong>
          ),
          em: ({ children }) => <em className="text-fg">{children}</em>,
          hr: () => <hr className="my-10 border-edge" />,
          blockquote: ({ children }) => (
            <blockquote className="my-6 rounded-r border-l-2 border-accent-dim bg-ink-raised px-5 py-4 [&>p]:mt-0 [&>p+p]:mt-3">
              {children}
            </blockquote>
          ),
          a: ({ href, children, ...props }) => {
            const target = href ?? "";
            // Inter-document links are written as relative .md filenames.
            const internal = /^[a-z0-9-]+\.md(#.*)?$/i.test(target);
            if (internal) {
              const [file, hash] = target.split("#");
              const slug = file.replace(/\.md$/i, "");
              return (
                <Link
                  href={`/methodology/${slug}${hash ? `#${hash}` : ""}`}
                  className="text-accent underline decoration-accent-dim underline-offset-2 hover:text-accent-strong"
                >
                  {children}
                </Link>
              );
            }
            const external = /^https?:/i.test(target);
            return (
              <a
                {...props}
                href={target}
                {...(external ? { rel: "noreferrer noopener" } : {})}
                className="text-accent underline decoration-accent-dim underline-offset-2 hover:text-accent-strong"
              >
                {children}
              </a>
            );
          },
          table: ({ children }) => (
            <div className="my-6 overflow-x-auto rounded-lg border border-edge">
              <table className="w-full min-w-[34rem] text-sm">{children}</table>
            </div>
          ),
          thead: ({ children }) => <thead className="bg-ink-high">{children}</thead>,
          th: ({ children }) => (
            <th className="border-b border-edge px-4 py-2.5 text-left font-medium text-fg">
              {children}
            </th>
          ),
          td: ({ children }) => (
            <td className="border-b border-edge px-4 py-2.5 align-top last:border-0">
              {children}
            </td>
          ),
          tr: ({ children }) => <tr className="last:[&>td]:border-0">{children}</tr>,
          pre: ({ children }) => <>{children}</>,
          code: ({ className, children, ...props }: ComponentPropsWithoutRef<"code">) => {
            const language = /language-(\w+)/.exec(className ?? "")?.[1];

            if (language === "mermaid") {
              return <Mermaid chart={flatten(children).trim()} />;
            }
            if (language) {
              return (
                <pre className="my-6 overflow-x-auto rounded-lg border border-edge bg-ink-raised p-5">
                  <code className="font-mono text-xs leading-relaxed text-fg-muted">
                    {children}
                  </code>
                </pre>
              );
            }
            return (
              <code
                {...props}
                className="rounded border border-edge bg-ink-high px-1.5 py-0.5 font-mono text-[0.85em] text-fg"
              >
                {children}
              </code>
            );
          },
        }}
      >
        {source}
      </ReactMarkdown>
    </div>
  );
}
