import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import Dashboard from './Dashboard';
import { MemoryRouter } from 'react-router-dom';
import { createContext, useContext } from 'react';
import { vi } from 'vitest';

// Mock axios baseURL
vi.mock('axios', () => ({ default: { defaults: { baseURL: '' }, get: vi.fn() } }));

// Mock EventSource
class MockEventSource {
  url: string;
  onmessage: ((this: EventSource, ev: MessageEvent) => any) | null = null;
  onerror: ((this: EventSource, ev: Event) => any) | null = null;
  constructor(url: string) { this.url = url; (globalThis as any).__es = this; setTimeout(() => {
    const ev = { data: JSON.stringify([{ type: 'LOW_STOCK', name: 'X' }]) } as MessageEvent;
    this.onmessage?.call(this as any, ev);
  }, 0); }
  close() {}
}
(globalThis as any).EventSource = MockEventSource as any;

const Ctx = createContext<any>(null);
function useOutletContextMock() { return useContext(Ctx); }
vi.mock('react-router-dom', async () => {
  const actual: any = await vi.importActual('react-router-dom');
  return { ...actual, useOutletContext: () => useOutletContextMock() };
});

it('receives alerts via SSE', async () => {
  const context: any = {
    authToken: 'token',
    wbProducts: [], loadingWb: false, useLocalData: true, setUseLocalData: () => {}, query: '', setQuery: () => {}, brand: '', setBrand: () => {}, category: '', setCategory: () => {}, minPrice: '', setMinPrice: () => {}, maxPrice: '', setMaxPrice: () => {}, minDiscount: '', setMinDiscount: () => {}, page: 1, setPage: () => {}, totalPages: 1, pagedProducts: [], fetchWbProducts: async () => {}, handleSyncWb: async () => {}, fetchAnalytics: async () => {}, analyticsReport: null, loadingAnalytics: false, minMarginPercent: undefined, setMinMarginPercent: () => {}, handleApplyMinMargin: () => {}, handleExport: async () => {}, validationItems: null, loadingValidation: false, fetchValidation: async () => {}, handleFileUpload: async () => {}, demoMode: false, setDemoMode: () => {}, runDemoAutofill: async () => {}, genCount: '0', setGenCount: () => {}, genType: 'both', setGenType: () => {}, delCount: '0', setDelCount: () => {}, delAll: false, setDelAll: () => {}, runDemoGenerate: async () => {}, runDemoDelete: async () => {}, currency: (n?: number) => String(n ?? ''), percent: (n?: number) => String(n ?? ''), numberFormat: (n?: number) => String(n ?? ''), sourceBadge: () => '', alerts: [], fetchAlerts: async () => {}
  };
  localStorage.setItem('mh_auth_token', 'token');

  render(
    <MemoryRouter>
      <Ctx.Provider value={context}>
        <Dashboard />
      </Ctx.Provider>
    </MemoryRouter>
  );

  await waitFor(() => expect(screen.getByText('Низкие остатки')).toBeInTheDocument());
});


