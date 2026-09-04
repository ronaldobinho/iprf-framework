"use client";

import { useEffect, useId, useRef, useState } from "react";

/**
 * Renders a Mermaid diagram, loaded on demand.
 *
 * Mermaid is a large dependency and only six of the eleven documents contain a
 * diagram, so it is imported dynamically inside the effect rather than bundled
 * into every page. The landing page draws its figure in markup instead and
 * never loads this at all.
 *
 * The source is shown as a code block until the render succeeds, so a reader
 * with JavaScript disabled — or a diagram that fails to parse — still gets the
 * content rather than an empty box.
 */
export function Mermaid({ chart }: { chart: string }) {
  const id = useId().replace(/[^a-zA-Z0-9]/g, "");
  const container = useRef<HTMLDivElement>(null);
  const [svg, setSvg] = useState<string | null>(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    let cancelled = false;

    (async () => {
      try {
        const mermaid = (await import("mermaid")).default;
        mermaid.initialize({
          startOnLoad: false,
          securityLevel: "strict",
          theme: "base",
          fontFamily: "var(--font-geist-sans), system-ui, sans-serif",
          themeVariables: {
            background: "#0D131C",
            primaryColor: "#141C28",
            primaryTextColor: "#E8EEF6",
            primaryBorderColor: "#2A3746",
            lineColor: "#65748A",
            secondaryColor: "#0E2439",
            tertiaryColor: "#0D131C",
            mainBkg: "#141C28",
            nodeBorder: "#2A3746",
            clusterBkg: "#0D131C",
            clusterBorder: "#1B2532",
            titleColor: "#E8EEF6",
            edgeLabelBackground: "#0D131C",
          },
        });
        const { svg: rendered } = await mermaid.render(`mmd-${id}`, chart);
        if (!cancelled) setSvg(rendered);
      } catch {
        if (!cancelled) setFailed(true);
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [chart, id]);

  if (svg) {
    return (
      <div
        ref={container}
        className="my-6 overflow-x-auto rounded-lg border border-edge bg-ink-raised p-5 [&_svg]:mx-auto [&_svg]:h-auto [&_svg]:max-w-full"
        // Mermaid output, rendered with securityLevel "strict" from repository
        // content that is not user-supplied.
        dangerouslySetInnerHTML={{ __html: svg }}
      />
    );
  }

  return (
    <figure className="my-6 overflow-x-auto rounded-lg border border-edge bg-ink-raised p-5">
      <pre className="overflow-x-auto font-mono text-xs leading-relaxed text-fg-muted">
        <code>{chart}</code>
      </pre>
      <figcaption className="mt-3 text-2xs text-fg-dim">
        {failed ? "Diagram source (could not be rendered)" : "Diagram source"}
      </figcaption>
    </figure>
  );
}
