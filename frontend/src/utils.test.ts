import { describe, it, expect } from 'vitest';
import { computeMargin, sortBy } from './utils';

describe('computeMargin', () => {
  it('computes margin and percent with all inputs', () => {
    const res = computeMargin(1000, 400, 100, 50, 50);
    expect(res.margin).toBe(400);
    expect(res.marginPercent).toBeCloseTo(40);
  });

  it('handles missing price', () => {
    const res = computeMargin(undefined, 100, 0, 0, 0);
    expect(res.margin).toBe(-100);
    expect(res.marginPercent).toBeUndefined();
  });
});

describe('sortBy', () => {
  it('sorts numbers asc/desc', () => {
    const arr = [{ n: 3 }, { n: 1 }, { n: 2 }];
    expect(sortBy(arr, x => x.n, 'asc').map(x => x.n)).toEqual([1, 2, 3]);
    expect(sortBy(arr, x => x.n, 'desc').map(x => x.n)).toEqual([3, 2, 1]);
  });

  it('sorts strings and puts undefined last', () => {
    const arr = [{ s: 'b' }, { s: undefined }, { s: 'a' }];
    expect(sortBy(arr, x => x.s, 'asc').map(x => x.s)).toEqual(['a', 'b', undefined]);
  });
});



