import type { Metadata, Viewport } from "next";
import localFont from "next/font/local";
import "./globals.css";
import { Footer } from "@/components/site/Footer";
import { Navbar } from "@/components/site/Navbar";
import { SITE } from "@/lib/site";

const geistSans = localFont({
  src: "./fonts/GeistVF.woff",
  variable: "--font-geist-sans",
  weight: "100 900",
  display: "swap",
});

const geistMono = localFont({
  src: "./fonts/GeistMonoVF.woff",
  variable: "--font-geist-mono",
  weight: "100 900",
  display: "swap",
});

export const metadata: Metadata = {
  metadataBase: new URL(SITE.url),
  title: {
    default: `${SITE.name} — ${SITE.fullName}`,
    template: `%s — ${SITE.name}`,
  },
  description: SITE.description,
  applicationName: SITE.name,
  keywords: [
    "instant payments",
    "real-time payments",
    "FedNow",
    "RTP",
    "fraud prevention",
    "authorised push payment fraud",
    "APP fraud",
    "payment resilience",
    "fraud decisioning",
    "payment infrastructure",
  ],
  openGraph: {
    type: "website",
    siteName: SITE.name,
    title: `${SITE.name} — ${SITE.fullName}`,
    description: SITE.description,
    url: SITE.url,
  },
  twitter: { card: "summary_large_image" },
  robots: { index: true, follow: true },
};

export const viewport: Viewport = {
  themeColor: "#06090B",
  colorScheme: "dark",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en" className={`${geistSans.variable} ${geistMono.variable}`}>
      <body className="min-h-screen antialiased">
        <a
          href="#main"
          className="sr-only focus:not-sr-only focus:absolute focus:left-4 focus:top-4 focus:z-[60] focus:rounded focus:bg-accent focus:px-4 focus:py-2 focus:font-semibold focus:text-ink"
        >
          Skip to content
        </a>

        {/* Watched by the navbar to decide when it stops being transparent. */}
        <div id="nav-sentinel" aria-hidden className="absolute top-0 h-px w-full" />

        <Navbar />
        <main id="main">{children}</main>
        <Footer />
      </body>
    </html>
  );
}
