/**
 * The landing's copy and structure, in one place.
 *
 * The five layers are the framework's spine, and they are described in
 * docs/framework/fraud-control-layers.md. Keeping the page's version of them
 * as data rather than as markup means the timeline, the platform stack and the
 * per-layer checklists cannot drift out of step with each other — they all read
 * from this array.
 */

export type LayerPath = "IN_PATH" | "ASYNC";

export interface JourneyLayer {
  id: string;
  label: string;
  title: string;
  description: string;
  path: LayerPath;
  /** The budget or cadence shown next to the path badge. */
  budget: string;
  items: string[];
  icon: "identity" | "behavior" | "network" | "enrichment" | "analysis";
}

export const LAYERS: JourneyLayer[] = [
  {
    id: "layer-1",
    label: "Layer 1",
    title: "Identity & Account Posture",
    description:
      "We verify identity, account history, device, channel and account age to assess the trust level of the payer.",
    path: "IN_PATH",
    budget: "< 1 ms",
    items: [
      "Identity verification",
      "Account reputation",
      "Device and channel analysis",
      "Historical behavior",
    ],
    icon: "identity",
  },
  {
    id: "layer-2",
    label: "Layer 2",
    title: "Real-Time Behavioral Scoring",
    description:
      "We analyze amount, counterparty, timing, channel and velocity using deterministic rules with strict latency budgets.",
    path: "IN_PATH",
    budget: "< 5 ms",
    items: [
      "Amount and pattern analysis",
      "Velocity checks",
      "Behavioral risk scoring",
      "Real-time decision rules",
    ],
    icon: "behavior",
  },
  {
    id: "layer-3",
    label: "Layer 3",
    title: "Counterparty & Network Signals",
    description:
      "We evaluate counterparty risk and network intelligence using pre-computed risk state — never live database lookups.",
    path: "IN_PATH",
    budget: "< 5 ms",
    items: [
      "Counterparty risk score",
      "Network relationship analysis",
      "Known fraud patterns",
      "Pre-computed risk state",
    ],
    icon: "network",
  },
  {
    id: "layer-4",
    label: "Layer 4",
    title: "External Enrichment",
    description:
      "We asynchronously enrich the transaction with external intelligence, sanctions data and contextual signals.",
    path: "ASYNC",
    budget: "Background",
    items: [
      "External data sources",
      "Sanctions and watchlists",
      "Fraud intelligence providers",
      "Context enrichment",
    ],
    icon: "enrichment",
  },
  {
    id: "layer-5",
    label: "Layer 5",
    title: "Post-Settlement Analysis",
    description:
      "We analyze completed transactions to detect new patterns and feed future decisions.",
    path: "ASYNC",
    budget: "Continuous",
    items: [
      "Pattern detection",
      "Typology analysis",
      "Feedback to risk models",
      "Continuous improvement",
    ],
    icon: "analysis",
  },
];

/**
 * Timeline stops, including the two endpoints that are not layers. The journey
 * progress is divided evenly across these, so the sphere reaches a stop exactly
 * when that stop's block is centred.
 */
export const JOURNEY_STOPS = ["start", ...LAYERS.map((l) => l.id), "decision"] as const;

/**
 * Hero credentials. Deliberately not adoption claims: each of these is a
 * property of the repository that a reader can verify by cloning it.
 */
export const HERO_CREDENTIALS = [
  "Open Source",
  "Modular & Extensible",
  "Reference Implementation",
] as const;

export const HERO_PILLARS = [
  "Real-time decisions",
  "Low latency",
  "Higher trust",
  "Greater resilience",
] as const;

/**
 * Performance figures.
 *
 * `TARGET` means a design budget the framework is built to, not a measurement:
 * the benchmark suite has not been run, so no number here is a result. `FACT`
 * means a property of the implementation that is true today and checkable in
 * the source. Nothing on this page may carry a badge implying a measurement
 * until backend/benchmarks produces one.
 */
export type MetricBadge = "TARGET" | "FACT" | "OPEN CORE";

export interface Metric {
  value: string;
  label: string;
  badge: MetricBadge;
  icon: "latency" | "layers" | "deterministic" | "audit";
}

export const METRICS: Metric[] = [
  {
    value: "< 10 ms",
    label: "In-path latency budget (p95)",
    badge: "TARGET",
    icon: "latency",
  },
  {
    value: "5 layers",
    label: "Three in-path, two asynchronous",
    badge: "FACT",
    icon: "layers",
  },
  {
    value: "Deterministic",
    label: "Rules and thresholds, no black box",
    badge: "FACT",
    icon: "deterministic",
  },
  {
    value: "100%",
    label: "Decisions written to an audit trail",
    badge: "OPEN CORE",
    icon: "audit",
  },
];

/** Only destinations that exist. A dead link on a credibility page is a cost. */
export const NAV_LINKS = [
  { href: "/#how-it-works", label: "How It Works" },
  { href: "/#architecture", label: "Architecture" },
  { href: "/#performance", label: "Performance" },
  { href: "/methodology", label: "Documentation" },
] as const;
