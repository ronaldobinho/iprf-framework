/**
 * Copies docs/framework/*.md into frontend/content/framework/.
 *
 * The methodology pages render those documents directly — specs/phase-5.md
 * forbids duplicating the content into React, so they stay the single source of
 * truth. Copying rather than reading across the directory boundary keeps the
 * build working regardless of which directory the host treats as the project
 * root, which is the usual way this breaks on a deploy.
 *
 * The copy is gitignored. Editing it does nothing; edit docs/framework.
 */
import { readdirSync, readFileSync, writeFileSync, mkdirSync, rmSync } from "node:fs";
import { dirname, resolve, join } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const SOURCE = resolve(here, "../../docs/framework");
const TARGET = resolve(here, "../content/framework");

let files;
try {
  files = readdirSync(SOURCE).filter((name) => name.endsWith(".md"));
} catch (error) {
  console.error(
    `\n  sync-docs: could not read ${SOURCE}\n  ${error.message}\n` +
      "  The methodology pages render docs/framework — the build cannot proceed without it.\n",
  );
  process.exit(1);
}

if (files.length === 0) {
  console.error("\n  sync-docs: docs/framework contains no .md files\n");
  process.exit(1);
}

rmSync(TARGET, { recursive: true, force: true });
mkdirSync(TARGET, { recursive: true });
for (const name of files) {
  writeFileSync(join(TARGET, name), readFileSync(join(SOURCE, name), "utf8"), "utf8");
}
console.log(`sync-docs: copied ${files.length} document(s) to content/framework`);
