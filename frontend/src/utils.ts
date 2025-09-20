export function computeMargin(
  price?: number,
  purchase?: number,
  logistics?: number,
  marketing?: number,
  other?: number
) {
  const p = price ?? 0;
  const m = (purchase ?? 0) + (logistics ?? 0) + (marketing ?? 0) + (other ?? 0);
  const margin = p - m;
  const marginPercent = p > 0 ? (margin / p) * 100 : undefined;
  return { margin, marginPercent };
}

export type SortOrder = 'asc' | 'desc';
export function sortBy<T>(arr: T[], getter: (t: T) => number | string | undefined | null, order: SortOrder = 'asc'): T[] {
  const factor = order === 'asc' ? 1 : -1;
  return [...arr].sort((a, b) => {
    const va = getter(a);
    const vb = getter(b);
    if (va == null && vb == null) return 0;
    if (va == null) return 1;
    if (vb == null) return -1;
    if (typeof va === 'number' && typeof vb === 'number') return factor * (va - vb);
    return factor * String(va).localeCompare(String(vb));
  });
}

export function persist<T>(key: string, value: T) {
  localStorage.setItem(key, JSON.stringify(value));
}
export function restore<T>(key: string, fallback: T): T {
  const v = localStorage.getItem(key);
  if (!v) return fallback;
  try { return JSON.parse(v) as T; } catch { return fallback; }
}

