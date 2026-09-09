"use client";

import { useEffect, useRef } from "react";
import { GLOBE_COORDS, GLOBE_MESH, GLOBE_MESH_COUNT, GLOBE_POINT_COUNT } from "./globe-data";

/**
 * The hero globe.
 *
 * Canvas rather than DOM: two and a half thousand points as elements would be
 * two and a half thousand layout boxes, and the browser would spend more time
 * on the decoration than on the page. Here the whole planet is one composited
 * surface and one rAF.
 *
 * The loop is not unconditional. It starts when the globe enters the viewport,
 * stops when it leaves, and never starts at all if the user asked for reduced
 * motion — in that case a single static frame is drawn, facing the Americas.
 */

const ROTATION_PERIOD_MS = 32_000;
const TILT = (-16 * Math.PI) / 180;
/** Longitude −90° faces the viewer at rest, which centres the Americas. */
const INITIAL_ROTATION = Math.PI / 2;

/** Endpoints for the connection arcs, as [longitude, latitude]. */
const HUBS: Record<string, [number, number]> = {
  newYork: [-74, 40.7],
  sanFrancisco: [-122.4, 37.8],
  chicago: [-87.6, 41.9],
  london: [-0.1, 51.5],
  saoPaulo: [-46.6, -23.5],
  singapore: [103.8, 1.4],
};

const ARCS: Array<[keyof typeof HUBS, keyof typeof HUBS]> = [
  ["sanFrancisco", "newYork"],
  ["newYork", "london"],
  ["newYork", "saoPaulo"],
  ["chicago", "london"],
  ["sanFrancisco", "singapore"],
];

const ARC_SEGMENTS = 30;

type Vec3 = [number, number, number];

function toCartesian(lonDeg: number, latDeg: number): Vec3 {
  const phi = (latDeg * Math.PI) / 180;
  const theta = (lonDeg * Math.PI) / 180;
  const cosPhi = Math.cos(phi);
  return [cosPhi * Math.sin(theta), Math.sin(phi), cosPhi * Math.cos(theta)];
}

/**
 * Great-circle interpolation, lifted off the surface toward the midpoint so the
 * arc reads as a link rather than as a line drawn on the sphere.
 */
function buildArc(a: Vec3, b: Vec3): Float32Array {
  const dot = Math.min(1, Math.max(-1, a[0] * b[0] + a[1] * b[1] + a[2] * b[2]));
  const omega = Math.acos(dot);
  const sinOmega = Math.sin(omega);
  const out = new Float32Array((ARC_SEGMENTS + 1) * 3);

  for (let i = 0; i <= ARC_SEGMENTS; i += 1) {
    const t = i / ARC_SEGMENTS;
    const wa = sinOmega < 1e-6 ? 1 - t : Math.sin((1 - t) * omega) / sinOmega;
    const wb = sinOmega < 1e-6 ? t : Math.sin(t * omega) / sinOmega;
    const lift = 1 + 0.16 * Math.sin(Math.PI * t);
    out[i * 3] = (a[0] * wa + b[0] * wb) * lift;
    out[i * 3 + 1] = (a[1] * wa + b[1] * wb) * lift;
    out[i * 3 + 2] = (a[2] * wa + b[2] * wb) * lift;
  }
  return out;
}

export function Globe() {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext("2d", { alpha: true });
    if (!ctx) return;

    // Expand the stored tenths-of-a-degree pairs into unit-sphere coordinates
    // once. A few thousand trig calls at mount, none per frame.
    function expand(source: Int16Array, count: number) {
      const out = new Float32Array(count * 3);
      for (let i = 0; i < count; i += 1) {
        const [x, y, z] = toCartesian(source[i * 2] / 10, source[i * 2 + 1] / 10);
        out[i * 3] = x;
        out[i * 3 + 1] = y;
        out[i * 3 + 2] = z;
      }
      return out;
    }

    const points = expand(GLOBE_COORDS, GLOBE_POINT_COUNT);
    const mesh = expand(GLOBE_MESH, GLOBE_MESH_COUNT);

    const arcs = ARCS.map(([from, to]) =>
      buildArc(toCartesian(...HUBS[from]), toCartesian(...HUBS[to])),
    );

    const cosTilt = Math.cos(TILT);
    const sinTilt = Math.sin(TILT);

    let width = 0;
    let height = 0;
    let radius = 0;
    let centerX = 0;
    let centerY = 0;

    function resize() {
      const rect = canvas!.getBoundingClientRect();
      if (rect.width === 0 || rect.height === 0) return;
      const dpr = Math.min(window.devicePixelRatio || 1, 2);
      width = rect.width;
      height = rect.height;
      canvas!.width = Math.round(width * dpr);
      canvas!.height = Math.round(height * dpr);
      ctx!.setTransform(dpr, 0, 0, dpr, 0, 0);
      radius = Math.min(width, height) * 0.42;
      centerX = width / 2;
      centerY = height / 2;
    }

    function draw(rotation: number, time: number) {
      if (radius === 0) return;
      ctx!.clearRect(0, 0, width, height);

      const cosR = Math.cos(rotation);
      const sinR = Math.sin(rotation);

      // The body of the planet: dark and translucent, so the grid behind it
      // stays faintly visible and it reads as glass rather than as a disc.
      const body = ctx!.createRadialGradient(
        centerX - radius * 0.3,
        centerY - radius * 0.35,
        radius * 0.1,
        centerX,
        centerY,
        radius,
      );
      body.addColorStop(0, "rgba(14, 38, 28, 0.72)");
      body.addColorStop(0.65, "rgba(7, 20, 15, 0.82)");
      body.addColorStop(1, "rgba(3, 9, 7, 0.92)");
      ctx!.beginPath();
      ctx!.arc(centerX, centerY, radius, 0, Math.PI * 2);
      ctx!.fillStyle = body;
      ctx!.fill();

      // Rim light.
      ctx!.beginPath();
      ctx!.arc(centerX, centerY, radius, 0, Math.PI * 2);
      ctx!.strokeStyle = "rgba(32, 224, 124, 0.28)";
      ctx!.lineWidth = 1;
      ctx!.stroke();

      // The mesh. Dim enough to read as the surface of the sphere rather than
      // as data, but present on every longitude so the planet never looks bald
      // when an ocean is facing us.
      ctx!.fillStyle = "#49a878";
      for (let i = 0; i < GLOBE_MESH_COUNT; i += 1) {
        const x = mesh[i * 3];
        const y = mesh[i * 3 + 1];
        const z = mesh[i * 3 + 2];

        const x1 = x * cosR + z * sinR;
        const z1 = -x * sinR + z * cosR;
        const y2 = y * cosTilt - z1 * sinTilt;
        const z2 = y * sinTilt + z1 * cosTilt;

        if (z2 <= 0.02) continue;

        ctx!.globalAlpha = 0.16 + 0.4 * z2;
        ctx!.fillRect(centerX + x1 * radius - 0.5, centerY - y2 * radius - 0.5, 1.1, 1.1);
      }

      // Land.
      for (let i = 0; i < GLOBE_POINT_COUNT; i += 1) {
        const x = points[i * 3];
        const y = points[i * 3 + 1];
        const z = points[i * 3 + 2];

        const x1 = x * cosR + z * sinR;
        const z1 = -x * sinR + z * cosR;
        const y2 = y * cosTilt - z1 * sinTilt;
        const z2 = y * sinTilt + z1 * cosTilt;

        if (z2 <= 0.02) continue;

        const sx = centerX + x1 * radius;
        const sy = centerY - y2 * radius;
        const depth = 0.2 + 0.8 * z2;
        const size = 0.7 + 0.8 * z2;

        ctx!.globalAlpha = 0.16 + 0.62 * depth;
        ctx!.fillStyle = "#3ce890";
        ctx!.fillRect(sx - size / 2, sy - size / 2, size, size);
      }
      ctx!.globalAlpha = 1;

      // Connection arcs, plus one packet travelling along each.
      arcs.forEach((arc, index) => {
        ctx!.beginPath();
        let started = false;
        for (let i = 0; i <= ARC_SEGMENTS; i += 1) {
          const x = arc[i * 3];
          const y = arc[i * 3 + 1];
          const z = arc[i * 3 + 2];
          const x1 = x * cosR + z * sinR;
          const z1 = -x * sinR + z * cosR;
          const y2 = y * cosTilt - z1 * sinTilt;
          const z2 = y * sinTilt + z1 * cosTilt;

          if (z2 <= 0) {
            started = false;
            continue;
          }
          const sx = centerX + x1 * radius;
          const sy = centerY - y2 * radius;
          if (started) ctx!.lineTo(sx, sy);
          else {
            ctx!.moveTo(sx, sy);
            started = true;
          }
        }
        ctx!.strokeStyle = "rgba(32, 224, 124, 0.3)";
        ctx!.lineWidth = 1;
        ctx!.stroke();

        // The packet. Its phase is offset per arc so they never pulse in unison.
        const t = ((time / 4200 + index * 0.23) % 1) * ARC_SEGMENTS;
        const seg = Math.floor(t);
        const x = arc[seg * 3];
        const y = arc[seg * 3 + 1];
        const z = arc[seg * 3 + 2];
        const x1 = x * cosR + z * sinR;
        const z1 = -x * sinR + z * cosR;
        const y2 = y * cosTilt - z1 * sinTilt;
        const z2 = y * sinTilt + z1 * cosTilt;
        if (z2 > 0) {
          const sx = centerX + x1 * radius;
          const sy = centerY - y2 * radius;
          ctx!.beginPath();
          ctx!.arc(sx, sy, 1.8, 0, Math.PI * 2);
          ctx!.fillStyle = "rgba(160, 255, 205, 0.9)";
          ctx!.fill();
        }
      });

      // Hub markers, drawn last so they sit above the land.
      for (const [lon, lat] of Object.values(HUBS)) {
        const [x, y, z] = toCartesian(lon, lat);
        const x1 = x * cosR + z * sinR;
        const z1 = -x * sinR + z * cosR;
        const y2 = y * cosTilt - z1 * sinTilt;
        const z2 = y * sinTilt + z1 * cosTilt;
        if (z2 <= 0.05) continue;
        const sx = centerX + x1 * radius;
        const sy = centerY - y2 * radius;
        ctx!.beginPath();
        ctx!.arc(sx, sy, 2.2, 0, Math.PI * 2);
        ctx!.fillStyle = `rgba(200, 255, 225, ${0.35 + 0.5 * z2})`;
        ctx!.fill();
      }
    }

    resize();

    const reduced = window.matchMedia("(prefers-reduced-motion: reduce)");

    let frame = 0;
    let running = false;
    let start = 0;

    function tick(now: number) {
      if (!start) start = now;
      const elapsed = now - start;
      draw(INITIAL_ROTATION + (elapsed / ROTATION_PERIOD_MS) * Math.PI * 2, elapsed);
      frame = requestAnimationFrame(tick);
    }

    function play() {
      if (running || reduced.matches) return;
      running = true;
      start = 0;
      frame = requestAnimationFrame(tick);
    }

    function pause() {
      if (!running) return;
      running = false;
      cancelAnimationFrame(frame);
    }

    function renderStatic() {
      draw(INITIAL_ROTATION, 0);
    }

    // Always paint one frame, so the globe is present before — and instead of,
    // under reduced motion — any animation begins.
    renderStatic();

    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) play();
        else pause();
      },
      { rootMargin: "120px" },
    );
    observer.observe(canvas);

    const resizeObserver = new ResizeObserver(() => {
      resize();
      if (!running) renderStatic();
    });
    resizeObserver.observe(canvas);

    function onMotionPreferenceChange() {
      if (reduced.matches) {
        pause();
        renderStatic();
      } else {
        play();
      }
    }
    reduced.addEventListener("change", onMotionPreferenceChange);

    function onVisibilityChange() {
      if (document.hidden) pause();
      else play();
    }
    document.addEventListener("visibilitychange", onVisibilityChange);

    return () => {
      pause();
      observer.disconnect();
      resizeObserver.disconnect();
      reduced.removeEventListener("change", onMotionPreferenceChange);
      document.removeEventListener("visibilitychange", onVisibilityChange);
    };
  }, []);

  return (
    <div className="pointer-events-none relative aspect-square w-full">
      <div className="globe-halo absolute inset-[-14%] rounded-full" aria-hidden />
      <canvas ref={canvasRef} className="absolute inset-0 h-full w-full" aria-hidden />
      <Orbit inset="-5%" />
      <Orbit inset="3%" className="opacity-60" spinClassName="[animation-delay:-13s] [animation-duration:47s]" />
    </div>
  );
}

/** A tilted ring with one satellite on it, so the rotation is actually visible. */
function Orbit({
  inset,
  className = "",
  spinClassName = "",
}: {
  inset: string;
  className?: string;
  spinClassName?: string;
}) {
  return (
    <div className={`globe-orbit absolute ${className}`} style={{ inset }} aria-hidden>
      <div className={`globe-orbit-spin absolute inset-0 ${spinClassName}`}>
        <span className="globe-orbit-dot absolute left-1/2 top-0 h-1.5 w-1.5 -translate-x-1/2 -translate-y-1/2 rounded-full" />
      </div>
    </div>
  );
}

export default Globe;
