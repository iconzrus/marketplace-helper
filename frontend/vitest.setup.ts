import '@testing-library/jest-dom';

// Provide jest globals shim for Vitest when needed
import { vi } from 'vitest';
// @ts-ignore
globalThis.jest = {
  mock: vi.mock,
} as any;


