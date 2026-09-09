import type { Config } from "tailwindcss";

/**
 * Design language: dark fintech infrastructure. Near-black charcoal with a
 * green undertone, one neon green accent, and controlled green / amber / red
 * reserved for decision outcomes.
 *
 * This replaces the electric-blue accent that specs/phase-5.md fixed for v1.
 * The deviation and its reason are recorded in that spec — see the "Deviation
 * record" section there.
 *
 * On the green collision: the accent and ALLOW now share a hue family. That is
 * survivable here only because the three outcomes appear together in a single
 * card, where the trio reads as a set and each verdict carries its own icon.
 * If a lone outcome pill ever returns to a data-dense surface, it must be
 * distinguished by more than hue again.
 */
const config: Config = {
  content: ["./src/**/*.{js,ts,jsx,tsx,mdx}"],
  theme: {
    extend: {
      colors: {
        // Surfaces, darkest to lightest. Charcoal, not pure black: pure black
        // kills the sense of depth the layered platforms depend on.
        ink: {
          DEFAULT: "#06090B", // page
          raised: "#0B1114", // cards
          high: "#111A1D", // inputs, hovered rows
        },
        edge: {
          DEFAULT: "#182326", // hairlines
          strong: "#25343A", // emphasised borders
        },
        // Text. `dim` is for large or secondary text only — at small sizes it
        // sits near the 4.5:1 floor against `ink`.
        fg: {
          DEFAULT: "#E7F0EC",
          muted: "#93A9A2",
          dim: "#647B74",
        },
        // The single accent. Used as an accent — never as a fill.
        accent: {
          DEFAULT: "#20E07C",
          strong: "#5CF0A6",
          dim: "#0F7A46",
          wash: "#06231A",
        },
        // Outcomes only.
        allow: { DEFAULT: "#20E07C", wash: "#06231A" },
        review: { DEFAULT: "#E0A72B", wash: "#2B2008" },
        decline: { DEFAULT: "#F2564D", wash: "#33110F" },
      },
      fontFamily: {
        sans: ["var(--font-geist-sans)", "system-ui", "sans-serif"],
        mono: ["var(--font-geist-mono)", "ui-monospace", "monospace"],
      },
      fontSize: {
        "2xs": ["0.6875rem", { lineHeight: "1rem", letterSpacing: "0.04em" }],
      },
      maxWidth: { content: "72rem", prose: "46rem" },
    },
  },
  plugins: [],
};

export default config;
