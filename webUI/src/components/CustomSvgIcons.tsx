import { InputOutputDTOTypeEnum } from "@/generated/client";

export function GrassBlockSvg() {
  return (
    <svg width="64" height="64" viewBox="0 0 64 64" xmlns="http://www.w3.org/2000/svg">
      <polygon points="32,4 60,18 32,32 4,18" fill="#4CAF50" />
      <polygon points="4,18 32,32 32,60 4,46" fill="#8B5A2B" />
      <polygon points="60,18 32,32 32,60 60,46" fill="#6E3F1F" />
      <polygon points="32,4 60,18 56,20 32,8 8,20 4,18" fill="#3E8E41" opacity="0.6" />
      <polyline points="4,18 32,32 60,18" fill="none" stroke="#2b2b2b" stroke-width="1" />
      <polyline points="32,32 32,60" fill="none" stroke="#2b2b2b" stroke-width="1" />
    </svg>
  );
}

function GenericMountainSvg() {
  return (
    <svg width="64" height="64" viewBox="0 0 64 64" xmlns="http://www.w3.org/2000/svg">
      <path d="M6 48 L22 22 L34 36 L42 28 L58 48 Z" fill="#90A4AE" />

      <path d="M10 48 L28 14 L46 48 Z" fill="#607D8B" />

      <path d="M22 28 L28 18 L34 28 L28 24 Z" fill="#ECEFF1" />

      <circle cx="48" cy="16" r="6" fill="#FFD54F" opacity="0.9" />

      <rect x="8" y="48" width="48" height="2" fill="#455A64" />
    </svg>
  );
}

function VoronoiNoise() {
  return (
    <svg width="64" height="64" viewBox="0 0 64 64" xmlns="http://www.w3.org/2000/svg">
      <path
        d="M6 10 L22 6 L30 16 L18 26 L6 22 Z"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linejoin="round"
      />

      <path
        d="M22 6 L40 8 L46 18 L30 16 Z"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linejoin="round"
      />

      <path
        d="M40 8 L58 14 L54 28 L42 24 L46 18 Z"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linejoin="round"
      />

      <path
        d="M6 22 L18 26 L16 44 L6 50 Z"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linejoin="round"
      />

      <path
        d="M18 26 L30 16 L42 24 L34 42 L16 44 Z"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linejoin="round"
      />

      <path
        d="M42 24 L54 28 L58 46 L40 54 L34 42 Z"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linejoin="round"
      />

      <circle cx="18" cy="18" r="2" fill="currentColor" />
      <circle cx="38" cy="14" r="2" fill="currentColor" />
      <circle cx="50" cy="22" r="2" fill="currentColor" />
      <circle cx="14" cy="40" r="2" fill="currentColor" />
      <circle cx="28" cy="34" r="2" fill="currentColor" />
      <circle cx="46" cy="40" r="2" fill="currentColor" />
    </svg>
  );
}

function WaterDepthSvg() {
  return (
    <svg width="64" height="64" viewBox="0 0 64 64" xmlns="http://www.w3.org/2000/svg">
      <path
        d="M6 34
           C12 28, 18 28, 24 34
           C30 40, 36 40, 42 34
           C48 28, 54 28, 60 34"
        fill="none"
        stroke="#c9c9c9"
        stroke-width="3"
        stroke-linecap="round"
        stroke-linejoin="round"
      />
      <path
        d="M6 42
           C12 36, 18 36, 24 42
           C30 48, 36 48, 42 42
           C48 36, 54 36, 60 42"
        fill="none"
        stroke="#e1e1e1"
        stroke-width="2"
        stroke-linecap="round"
        opacity="0.8"
      />
      <line
        x1="32"
        y1="8"
        x2="32"
        y2="26"
        stroke="#c8c8c8"
        stroke-width="3"
        stroke-linecap="round"
      />

      <polyline
        points="24,20 32,28 40,20"
        fill="none"
        stroke="#d2d2d2"
        stroke-width="3"
        stroke-linecap="round"
        stroke-linejoin="round"
      />
    </svg>
  );
}
export function PerlinNoiseIcon() {
  return (
    <svg viewBox="0 0 64 64">
      <defs>
        <filter id="perlin">
          <feTurbulence
            type="fractalNoise"
            baseFrequency="0.08"
            numOctaves="4"
            seed="2"
            stitchTiles="stitch"
          />
          <feComponentTransfer>
            <feFuncR type="linear" slope="1.4" intercept="-0.2" />
            <feFuncG type="linear" slope="1.4" intercept="-0.2" />
            <feFuncB type="linear" slope="1.4" intercept="-0.2" />
          </feComponentTransfer>
        </filter>
      </defs>

      <rect x="4" y="4" width="56" height="56" filter="url(#perlin)" />
      <rect x="4" y="4" width="56" height="56" fill="none" stroke="black" strokeWidth="2" rx="6" />
    </svg>
  );
}
export function GetIconForIoType(io: InputOutputDTOTypeEnum) {
  switch (io) {
    case "TERRAIN":
      return GrassBlockSvg();
    case "WATER_DEPTH":
      return WaterDepthSvg();
    case "VORONOI_NOISE":
      return VoronoiNoise();
    case "PERLIN_NOISE":
      return PerlinNoiseIcon();
    default:
      return GenericMountainSvg();
  }
}
