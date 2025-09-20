import { describe, it, expect } from "vitest";
import { parseSelection, paginate } from "./selection.js";

const makeSet = (name: string, n: number) => ({
  name,
  stickers: Array.from({ length: n }, (_, i) => ({ fileId: `${name}-${i}`, format: "static" as const, index: i })),
});

describe("selection", () => {
  it("parses ranges across multiple sets (flattened)", () => {
    const sets = [makeSet("a", 3), makeSet("b", 3)];
    const res = parseSelection("2-4", sets as any, "ranges");
    expect(res.errors.length).toBe(0);
    expect(res.chosen.length).toBe(3);
  });

  it("parses emojis filter", () => {
    const sets = [{ name: "s", stickers: [
      { fileId: "1", emoji: "😀", format: "static" as const, index: 0 },
      { fileId: "2", emoji: "😂", format: "static" as const, index: 1 },
    ] }];
    const res = parseSelection(":😀,😂", sets as any, "emojis");
    expect(res.errors.length).toBe(0);
    expect(res.chosen.length).toBe(2);
  });

  it("paginates lists", () => {
    const arr = Array.from({ length: 45 }, (_, i) => i);
    const p1 = paginate(arr, 1, 20);
    const p3 = paginate(arr, 3, 20);
    expect(p1.items.length).toBe(20);
    expect(p3.items.length).toBe(5);
    expect(p3.pages).toBe(3);
  });
});


