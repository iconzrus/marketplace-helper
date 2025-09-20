import { ChosenStickerRef, SourceSet, TelegramStickerLite } from "../types.js";

export interface ParsedSelection {
  chosen: ChosenStickerRef[];
  errors: string[];
}

// Parse user selection input like:
// "1-5, 7, 10-12" or ":😂,😀" (emojis)
export function parseSelection(
  input: string,
  sourceSets: SourceSet[],
  mode: "ranges" | "emojis"
): ParsedSelection {
  const errors: string[] = [];
  const chosen: ChosenStickerRef[] = [];

  if (mode === "ranges") {
    const tokens = input
      .split(/[\s,]+/)
      .map((t) => t.trim())
      .filter(Boolean);
    for (const token of tokens) {
      const m = token.match(/^(\d+)(?:-(\d+))?$/);
      if (!m) {
        errors.push(`Invalid range token: ${token}`);
        continue;
      }
      const start = Number(m[1]);
      const end = m[2] ? Number(m[2]) : start;
      if (start < 1 || end < 1 || end < start) {
        errors.push(`Invalid bounds: ${token}`);
        continue;
      }
      // Apply across concatenated list of all stickers from all sets in displayed order
      const flat: Array<{ sourceSetName: string; indexInSource: number }> = [];
      for (const set of sourceSets) {
        for (let i = 0; i < set.stickers.length; i++) {
          flat.push({ sourceSetName: set.name, indexInSource: i });
        }
      }
      for (let idx = start - 1; idx < end && idx < flat.length; idx++) {
        chosen.push(flat[idx]);
      }
    }
  } else if (mode === "emojis") {
    // Expect input like ":😀,😂" or just emojis separated by space/commas
    const emojis = input
      .replaceAll(":", " ")
      .split(/[\s,]+/)
      .map((t) => t.trim())
      .filter(Boolean);
    if (emojis.length === 0) {
      errors.push("No emojis provided");
    }
    for (const set of sourceSets) {
      set.stickers.forEach((s, index) => {
        if (s.emoji && emojis.includes(s.emoji)) {
          chosen.push({ sourceSetName: set.name, indexInSource: index });
        }
      });
    }
  }

  // Deduplicate while preserving order
  const seen = new Set<string>();
  const deduped: ChosenStickerRef[] = [];
  for (const ref of chosen) {
    const key = `${ref.sourceSetName}#${ref.indexInSource}`;
    if (!seen.has(key)) {
      seen.add(key);
      deduped.push(ref);
    }
  }

  return { chosen: deduped, errors };
}

export function paginate<T>(arr: T[], page: number, perPage: number): {
  items: T[];
  total: number;
  page: number;
  pages: number;
} {
  const total = arr.length;
  const pages = Math.max(1, Math.ceil(total / perPage));
  const clampedPage = Math.min(Math.max(1, page), pages);
  const start = (clampedPage - 1) * perPage;
  const end = Math.min(start + perPage, total);
  return {
    items: arr.slice(start, end),
    total,
    page: clampedPage,
    pages,
  };
}


