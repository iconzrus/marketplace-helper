import { describe, it, expect } from "vitest";
import { parseSelection, paginate } from "./selection.js";
const makeSet = (name, n) => ({
    name,
    stickers: Array.from({ length: n }, (_, i) => ({ fileId: `${name}-${i}`, format: "static", index: i })),
});
describe("selection", () => {
    it("parses ranges across multiple sets (flattened)", () => {
        const sets = [makeSet("a", 3), makeSet("b", 3)];
        const res = parseSelection("2-4", sets, "ranges");
        expect(res.errors.length).toBe(0);
        expect(res.chosen.length).toBe(3);
    });
    it("parses emojis filter", () => {
        const sets = [{ name: "s", stickers: [
                    { fileId: "1", emoji: "😀", format: "static", index: 0 },
                    { fileId: "2", emoji: "😂", format: "static", index: 1 },
                ] }];
        const res = parseSelection(":😀,😂", sets, "emojis");
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
