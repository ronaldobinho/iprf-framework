import type { Config } from "tailwindcss";

/**
 * Design language is fixed by specs/phase-5.md and is not a matter of taste
 * here: dark enterprise, near-black over deep navy, one electric blue accent,
 * and controlled green / amber / red reserved exclusively for decision
 * outcomes. Bloomberg-meets-cloud-infrastructure, not startup-playful.
 *
 * The outcome colours are deliberately scarce. If they appear anywhere other
 * than ALLOW / REVIEW / DECLINE, they stop reading as a verdict.
 */
const config: Config = {
  content: ["./src/**/*.{js,ts,jsx,tsx,mdx}"],
  theme: {
    extend: {
      colors: {
        // Surfaces, darkest to lightest.
        ink: {
          DEFAULT: "#070A0F", // page
          raised: "#0D131C", // cards
          high: "#141C28", // inputs, hovered rows
        },
        edge: {
          DEFAULT: "#1B2532", // hairlines
          strong: "#2A3746", // emphasised borders
        },
        // Text. `dim` is for large or secondary text only — at small sizes it
        // sits near the 4.5:1 floor against `ink`.
        fg: {
          DEFAULT: "#E8EEF6",
          muted: "#94A2B5",
          dim: "#65748A",
        },
        // The single accent.
        accent: {
          DEFAULT: "#3B9EFF",
          strong: "#67B4FF",
          dim: "#1B5FA8",
          wash: "#0E2439",
        },
        // Outcomes only.
        allow: { DEFAULT: "#3FB950", wash: "#0C2912" },
        review: { DEFAULT: "#D6A020", wash: "#2B2008" },
        decline: { DEFAULT: "#F85149", wash: "#33110F" },
      },
      fontFamily: {
        sans: ["var(--font-geist-sans)", "system-ui", "sans-serif"],
        mono: ["var(--font-geist-mono)", "ui-monospace", "monospace"],
      },
      fontSize: {
        "2xs": ["0.6875rem", { lineHeight: "1rem", letterSpacing: "0.04em" }],
      },
      maxWidth: { content: "68rem", prose: "46rem" },
    },
  },
  plugins: [],
};

export default config;
