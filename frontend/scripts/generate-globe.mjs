/**
 * Bakes the hero globe's land point cloud into a TypeScript constant.
 *
 * Why a build script and not a runtime fetch: the landing is a static export
 * with a hard rule against unnecessary dependencies and third-party requests.
 * A coastline dataset would be either an npm dependency or a network call at
 * the exact moment the hero is trying to paint. Rasterising it once, here, and
 * committing the result costs the page nothing.
 *
 * The outlines below are deliberately coarse. At the rendered size each point
 * is a 1–2px dot on a sphere, so what matters is the macro-silhouette, not the
 * coastline. The Americas carry more detail than the rest because the hero
 * faces them.
 *
 * Run with: node scripts/generate-globe.mjs
 * The output is committed. Re-run only when the outlines change.
 */
import { writeFileSync, mkdirSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const OUT = resolve(here, "../src/components/landing/globe-data.ts");

/** Polygons as [longitude, latitude] rings. */
const LAND = {
  northAmerica: [
    [-168, 65.5], [-166, 60], [-161, 58.5], [-153, 59.5], [-148, 60.5], [-140, 59.5],
    [-135, 57], [-131, 53], [-127, 50], [-124, 46], [-124, 40], [-121, 35], [-117, 32.5],
    [-114.5, 30], [-112, 29], [-110, 24], [-106, 23], [-105, 20], [-100, 18], [-96, 16],
    [-92, 15], [-88, 15.5], [-84, 10], [-80, 9], [-77, 8], [-79, 9.5], [-83, 11],
    [-87, 16], [-90, 19], [-87, 21.5], [-90, 22], [-94, 19], [-97, 22], [-97, 26],
    [-94, 29.5], [-89, 29], [-85, 30], [-82, 25], [-80, 26], [-81, 31], [-79, 33],
    [-76, 35], [-75, 38], [-74, 40], [-70, 42], [-67, 45], [-65, 44], [-61, 46],
    [-64, 48], [-56, 51], [-56, 54], [-62, 58], [-66, 61], [-78, 63], [-80, 70],
    [-95, 70], [-100, 68], [-115, 69], [-125, 70], [-133, 69], [-141, 70],
    [-156, 71], [-163, 69],
  ],
  southAmerica: [
    [-81, -4], [-79, 0], [-77, 1], [-75, 6], [-72, 11], [-67, 11], [-62, 10], [-60, 8],
    [-55, 6], [-51, 4], [-50, 0], [-44, -2], [-38, -5], [-35, -6], [-35, -9], [-38, -13],
    [-39, -17], [-42, -22], [-48, -25], [-53, -33], [-57, -35], [-57, -39], [-62, -40],
    [-63, -42], [-65, -45], [-68, -50], [-69, -53], [-74, -53], [-75, -50], [-73, -45],
    [-73, -40], [-72, -33], [-71, -25], [-70, -18], [-76, -14], [-79, -8],
  ],
  greenland: [
    [-45, 60], [-43, 64], [-38, 66], [-32, 68], [-25, 71], [-21, 73], [-18, 76],
    [-22, 79], [-32, 82], [-45, 83], [-58, 82], [-65, 80], [-68, 76], [-62, 70],
    [-55, 66], [-50, 62],
  ],
  africa: [
    [-17, 15], [-16, 21], [-13, 28], [-10, 31], [-6, 36], [0, 37], [10, 37], [11, 34],
    [17, 31], [25, 32], [31, 31], [34, 31], [37, 22], [39, 15], [43, 11], [51, 12],
    [51, 6], [45, 3], [41, -2], [40, -10], [35, -18], [33, -26], [28, -33], [22, -34],
    [18, -34], [15, -27], [12, -18], [13, -12], [9, -1], [5, 4], [-4, 5], [-8, 4],
    [-13, 8],
  ],
  eurasia: [
    [-10, 36], [-9, 43], [-2, 43], [-2, 48], [0, 49], [3, 51], [5, 53], [8, 54],
    [10, 57], [8, 58], [5, 59], [7, 63], [12, 65], [15, 68], [21, 70], [28, 71],
    [33, 70], [40, 68], [45, 68], [55, 70], [60, 70], [69, 73], [75, 73], [80, 74],
    [90, 76], [100, 77], [110, 77], [120, 74], [130, 73], [140, 72], [150, 70],
    [160, 70], [170, 69], [179, 67], [179, 64], [170, 61], [163, 58], [160, 55],
    [155, 52], [143, 48], [140, 45], [135, 43], [130, 42], [127, 38], [122, 37],
    [120, 33], [122, 30], [118, 25], [110, 21], [108, 16], [106, 10], [103, 1],
    [98, 8], [95, 16], [92, 21], [88, 22], [80, 15], [78, 9], [73, 17], [70, 22],
    [64, 25], [57, 25], [50, 28], [48, 30], [44, 38], [36, 36], [30, 36], [26, 38],
    [24, 35], [21, 39], [19, 40], [16, 41], [13, 45], [18, 42], [16, 38], [12, 38],
    [12, 44], [8, 44], [4, 43], [0, 39], [-6, 36],
  ],
  australia: [
    [113, -22], [114, -26], [116, -32], [119, -34], [125, -33], [130, -32], [135, -35],
    [138, -35], [141, -38], [147, -38], [150, -37], [153, -31], [153, -25], [149, -21],
    [146, -18], [142, -11], [136, -12], [131, -12], [125, -14], [122, -17], [117, -21],
  ],
  india: [
    [70, 22], [72, 20], [73, 16], [75, 12], [77, 8], [80, 10], [80, 16], [83, 18],
    [87, 21], [89, 22], [86, 24], [80, 25], [75, 24], [70, 24],
  ],
};

/** Smaller land masses, same format. */
const ISLANDS = [
  // Britain
  [[-5, 50], [-3, 51], [1, 51], [1, 53], [-1, 55], [-2, 57], [-4, 58], [-6, 57], [-5, 54], [-5, 52]],
  // Ireland
  [[-10, 52], [-6, 52], [-6, 55], [-8, 55], [-10, 54]],
  // Iceland
  [[-24, 65], [-18, 66], [-14, 66], [-14, 64], [-19, 63], [-23, 64]],
  // Japan
  [[130, 32], [132, 34], [136, 34], [140, 36], [141, 40], [142, 43], [145, 44], [143, 42], [140, 38], [137, 36], [133, 34], [130, 31]],
  // Madagascar
  [[43, -12], [48, -14], [50, -18], [47, -25], [45, -23], [43, -18]],
  // New Zealand
  [[173, -35], [176, -38], [178, -38], [175, -41], [172, -41], [170, -44], [167, -46], [169, -46], [174, -41]],
  // Sumatra
  [[95, 5], [98, 2], [102, -2], [106, -6], [104, -6], [100, -1], [95, 3]],
  // Java
  [[105, -6], [112, -7], [115, -8], [110, -8], [106, -7]],
  // Borneo
  [[109, 2], [113, 3], [117, 5], [119, 1], [117, -3], [113, -3], [110, -1]],
  // Philippines
  [[120, 14], [122, 17], [124, 18], [123, 13], [126, 10], [125, 6], [122, 7], [120, 11]],
  // Sri Lanka
  [[80, 6], [82, 7], [81, 9], [80, 9]],
  // Cuba
  [[-85, 22], [-80, 23], [-75, 20], [-79, 21], [-83, 21]],
  // Papua New Guinea
  [[131, -1], [138, -3], [144, -4], [150, -6], [147, -9], [141, -9], [135, -8], [131, -4]],
  // Newfoundland
  [[-59, 47], [-55, 47], [-53, 49], [-56, 51], [-59, 49]],
  // Svalbard
  [[11, 77], [20, 78], [22, 80], [15, 80], [11, 79]],
  // Tierra del Fuego
  [[-74, -54], [-68, -53], [-66, -55], [-71, -55]],
];

/** Ray casting. Longitudes here never straddle the antimeridian. */
function contains(ring, lon, lat) {
  let inside = false;
  for (let i = 0, j = ring.length - 1; i < ring.length; j = i++) {
    const [xi, yi] = ring[i];
    const [xj, yj] = ring[j];
    if (yi > lat !== yj > lat && lon < ((xj - xi) * (lat - yi)) / (yj - yi) + xi) {
      inside = !inside;
    }
  }
  return inside;
}

const RINGS = [...Object.values(LAND), ...ISLANDS];

function isLand(lon, lat) {
  return RINGS.some((ring) => contains(ring, lon, lat));
}

/*
 * Equal-area sampling. A naive lat/lon grid crowds points at the poles, which
 * is the single most recognisable tell of a hand-rolled dot globe. Scaling the
 * longitude count by cos(latitude) keeps the spacing roughly constant across
 * the sphere.
 */
const LAT_STEP = 2.1;
const LAT_MIN = -58;
const LAT_MAX = 80;
const SPACING = 2.1;

const points = [];
for (let lat = LAT_MIN; lat <= LAT_MAX; lat += LAT_STEP) {
  const circumferenceFactor = Math.cos((lat * Math.PI) / 180);
  const count = Math.max(1, Math.round((360 / SPACING) * circumferenceFactor));
  for (let i = 0; i < count; i += 1) {
    const lon = -180 + (360 * i) / count;
    if (isLand(lon, lat)) {
      points.push([Math.round(lon * 10) / 10, Math.round(lat * 10) / 10]);
    }
  }
}

/*
 * A second, coarser sampling of the whole sphere — ocean included.
 *
 * Without it the globe reads as a globe only while a continent happens to face
 * the viewer; for the rest of the rotation it is a dark disc with dots on the
 * limb. The mesh is drawn far dimmer than the land, so the continents still
 * carry the shape, but the sphere is always legible as one.
 */
const MESH_SPACING = 4.6;
const mesh = [];
for (let lat = -76; lat <= 76; lat += MESH_SPACING) {
  const count = Math.max(1, Math.round((360 / MESH_SPACING) * Math.cos((lat * Math.PI) / 180)));
  for (let i = 0; i < count; i += 1) {
    mesh.push(Math.round((-180 + (360 * i) / count) * 10), Math.round(lat * 10));
  }
}

/*
 * Stored as tenths of a degree rather than unit-sphere floats: two short
 * integers per point instead of three long decimals, which roughly quarters
 * the emitted bytes. The component expands them to Cartesian coordinates once
 * on mount — a few thousand trig calls, far below a frame's budget — and the
 * render loop then only rotates around Y.
 */
const flat = [];
for (const [lon, lat] of points) {
  flat.push(Math.round(lon * 10), Math.round(lat * 10));
}

const body = `/**
 * Generated by scripts/generate-globe.mjs — do not edit by hand.
 *
 * Points for the hero globe, flattened as [lon, lat, lon, lat, ...] in tenths
 * of a degree. GLOBE_COORDS is land (${points.length} points, drawn bright);
 * GLOBE_MESH is the whole sphere at a coarser spacing (${mesh.length / 2}
 * points, drawn dim) so the planet stays legible when an ocean faces the
 * viewer.
 */
export const GLOBE_POINT_COUNT = ${points.length};

export const GLOBE_COORDS = new Int16Array([
${flat.join(",")}
]);

export const GLOBE_MESH_COUNT = ${mesh.length / 2};

export const GLOBE_MESH = new Int16Array([
${mesh.join(",")}
]);
`;

mkdirSync(dirname(OUT), { recursive: true });
writeFileSync(OUT, body);
console.log(`globe: ${points.length} land points -> ${OUT}`);
