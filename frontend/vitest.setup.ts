import '@testing-library/jest-dom';

// Provide jest globals shim for Vitest when needed
import { vi } from 'vitest';
// @ts-ignore
globalThis.jest = {
  mock: vi.mock,
} as any;

// Polyfill ResizeObserver for Recharts in JSDOM
if (!(globalThis as any).ResizeObserver) {
  (globalThis as any).ResizeObserver = class {
    observe() {}
    unobserve() {}
    disconnect() {}
  } as any;
}


