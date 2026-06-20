export function toPercent(v: number, min: number, max: number) {
  return ((v - min) / (max - min)) * 100;
}

export function clamp(n: number, min: number, max: number) {
  return Math.max(min, Math.min(max, n));
}
