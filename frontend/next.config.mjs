/** @type {import('next').NextConfig} */
const nextConfig = {
  // Static export. The landing and the simulator must run with no backend at
  // all — that is what makes the public demo real rather than a screenshot,
  // and it is a requirement of the project brief.
  output: 'export',

  // The image optimizer needs a server, which a static export does not have.
  images: { unoptimized: true },

  // Emit /methodology/index.html rather than /methodology.html, so any static
  // host serves the routes without rewrite rules.
  trailingSlash: true,

  eslint: { ignoreDuringBuilds: false },
  typescript: { ignoreBuildErrors: false },
};

export default nextConfig;
