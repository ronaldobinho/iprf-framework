import Link from "next/link";

export default function NotFound() {
  return (
    <div className="px-6 py-32">
      <div className="mx-auto w-full max-w-prose">
        <p className="font-mono text-2xs uppercase tracking-[0.18em] text-accent">404</p>
        <h1 className="mt-4 text-3xl font-semibold tracking-tight">Nothing here</h1>
        <p className="mt-4 text-base leading-relaxed text-fg-muted">
          That page does not exist. The framework specification is the most likely thing you
          were looking for.
        </p>
        <div className="mt-8 flex gap-3">
          <Link
            href="/"
            className="rounded bg-accent px-5 py-2.5 text-sm font-medium text-ink hover:bg-accent-strong"
          >
            Home
          </Link>
          <Link
            href="/methodology"
            className="rounded border border-edge-strong px-5 py-2.5 text-sm hover:border-accent-dim"
          >
            Methodology
          </Link>
        </div>
      </div>
    </div>
  );
}
