/**
 * Inline SVG icons.
 *
 * Inline rather than an icon package: the landing needs about a dozen glyphs,
 * and a dependency for that would ship a few thousand it does not. Every icon
 * here is decorative — the surrounding text carries the meaning — so they are
 * all aria-hidden and the components that use them supply the label.
 */
import type { SVGProps } from "react";
import type { JourneyLayer } from "./content";

type IconProps = SVGProps<SVGSVGElement>;

function Base({ children, ...props }: IconProps & { children: React.ReactNode }) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.6}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden
      focusable="false"
      {...props}
    >
      {children}
    </svg>
  );
}

export function IdentityIcon(props: IconProps) {
  return (
    <Base {...props}>
      <circle cx="12" cy="8.5" r="3.5" />
      <path d="M5 19.5c0-3.6 3.1-5.5 7-5.5s7 1.9 7 5.5" />
    </Base>
  );
}

export function BehaviorIcon(props: IconProps) {
  return (
    <Base {...props}>
      <path d="M5 19V11" />
      <path d="M10 19V6" />
      <path d="M15 19v-5" />
      <path d="M20 19V9" />
    </Base>
  );
}

export function NetworkIcon(props: IconProps) {
  return (
    <Base {...props}>
      <circle cx="6" cy="7" r="2.2" />
      <circle cx="18" cy="7" r="2.2" />
      <circle cx="12" cy="17.5" r="2.2" />
      <path d="M8.2 7h7.6" />
      <path d="M7.1 8.9 10.9 15.6" />
      <path d="M16.9 8.9 13.1 15.6" />
    </Base>
  );
}

export function EnrichmentIcon(props: IconProps) {
  return (
    <Base {...props}>
      <ellipse cx="12" cy="6.5" rx="6.5" ry="2.8" />
      <path d="M5.5 6.5v11c0 1.5 2.9 2.8 6.5 2.8s6.5-1.3 6.5-2.8v-11" />
      <path d="M5.5 12c0 1.5 2.9 2.8 6.5 2.8s6.5-1.3 6.5-2.8" />
    </Base>
  );
}

export function AnalysisIcon(props: IconProps) {
  return (
    <Base {...props}>
      <circle cx="11" cy="11" r="6" />
      <path d="m15.5 15.5 4 4" />
    </Base>
  );
}

const LAYER_ICONS: Record<JourneyLayer["icon"], (props: IconProps) => JSX.Element> = {
  identity: IdentityIcon,
  behavior: BehaviorIcon,
  network: NetworkIcon,
  enrichment: EnrichmentIcon,
  analysis: AnalysisIcon,
};

export function LayerIcon({ name, ...props }: IconProps & { name: JourneyLayer["icon"] }) {
  const Component = LAYER_ICONS[name];
  return <Component {...props} />;
}

export function LatencyIcon(props: IconProps) {
  return (
    <Base {...props}>
      <path d="M13 2 4.5 13.5H11l-1 8.5 8.5-11.5H12l1-8.5Z" />
    </Base>
  );
}

export function LayersIcon(props: IconProps) {
  return (
    <Base {...props}>
      <path d="m12 3 8.5 4.5L12 12 3.5 7.5 12 3Z" />
      <path d="m3.5 12.5 8.5 4.5 8.5-4.5" />
      <path d="m3.5 17 8.5 4.5 8.5-4.5" />
    </Base>
  );
}

export function DeterministicIcon(props: IconProps) {
  return (
    <Base {...props}>
      <path d="M12 3 4.5 6.2v5.4c0 4.5 3.1 8 7.5 9.4 4.4-1.4 7.5-4.9 7.5-9.4V6.2L12 3Z" />
      <path d="m9 12 2.2 2.2L15.2 10" />
    </Base>
  );
}

export function AuditIcon(props: IconProps) {
  return (
    <Base {...props}>
      <path d="M6 3.5h8.5L19 8v12.5H6V3.5Z" />
      <path d="M14 3.5V8h5" />
      <path d="M9 12.5h6" />
      <path d="M9 16h4" />
    </Base>
  );
}

const METRIC_ICONS = {
  latency: LatencyIcon,
  layers: LayersIcon,
  deterministic: DeterministicIcon,
  audit: AuditIcon,
} as const;

export function MetricIcon({
  name,
  ...props
}: IconProps & { name: keyof typeof METRIC_ICONS }) {
  const Component = METRIC_ICONS[name];
  return <Component {...props} />;
}

export function GitHubIcon(props: IconProps) {
  return (
    <svg viewBox="0 0 24 24" fill="currentColor" aria-hidden focusable="false" {...props}>
      <path d="M12 1.5A10.5 10.5 0 0 0 8.68 22a.79.79 0 0 0 .57-.19.75.75 0 0 0 .2-.55v-1.93c-2.92.63-3.54-1.4-3.54-1.4a2.79 2.79 0 0 0-1.17-1.54c-.95-.65.07-.64.07-.64a2.21 2.21 0 0 1 1.61 1.08 2.24 2.24 0 0 0 3.06.87 2.24 2.24 0 0 1 .67-1.41c-2.33-.26-4.78-1.17-4.78-5.19a4.06 4.06 0 0 1 1.08-2.82 3.77 3.77 0 0 1 .1-2.78s.88-.28 2.89 1.08a9.94 9.94 0 0 1 5.26 0c2-1.36 2.88-1.08 2.88-1.08a3.77 3.77 0 0 1 .11 2.78 4.05 4.05 0 0 1 1.08 2.82c0 4-2.46 4.92-4.8 5.18a2.51 2.51 0 0 1 .72 1.95v2.89a.75.75 0 0 0 .21.55.79.79 0 0 0 .57.19A10.5 10.5 0 0 0 12 1.5Z" />
    </svg>
  );
}

export function BookIcon(props: IconProps) {
  return (
    <Base {...props}>
      <path d="M4 4.5h5.5A2.5 2.5 0 0 1 12 7v13a2 2 0 0 0-2-2H4V4.5Z" />
      <path d="M20 4.5h-5.5A2.5 2.5 0 0 0 12 7v13a2 2 0 0 1 2-2h6V4.5Z" />
    </Base>
  );
}

export function ArrowRightIcon(props: IconProps) {
  return (
    <Base {...props}>
      <path d="M4.5 12h15" />
      <path d="m13.5 6 6 6-6 6" />
    </Base>
  );
}

export function CheckIcon(props: IconProps) {
  return (
    <Base {...props}>
      <path d="m5 12.5 4.5 4.5L19 7" />
    </Base>
  );
}

export function ClockIcon(props: IconProps) {
  return (
    <Base {...props}>
      <circle cx="12" cy="12" r="8.5" />
      <path d="M12 7v5.2l3.2 2" />
    </Base>
  );
}

export function CrossIcon(props: IconProps) {
  return (
    <Base {...props}>
      <path d="m6.5 6.5 11 11" />
      <path d="m17.5 6.5-11 11" />
    </Base>
  );
}

export function SquareIcon(props: IconProps) {
  return (
    <Base {...props}>
      <rect x="4.5" y="4.5" width="15" height="15" rx="3" />
    </Base>
  );
}
