import type { MetadataRoute } from "next";
import { getAllDocs } from "@/lib/docs";
import { SITE } from "@/lib/site";

export const dynamic = "force-static";

export default function sitemap(): MetadataRoute.Sitemap {
  const docs = getAllDocs().map((doc) => ({
    url: `${SITE.url}/methodology/${doc.slug}/`,
    changeFrequency: "monthly" as const,
    priority: 0.6,
  }));

  return [
    { url: `${SITE.url}/`, changeFrequency: "monthly", priority: 1 },
    { url: `${SITE.url}/methodology/`, changeFrequency: "monthly", priority: 0.8 },
    ...docs,
  ];
}
