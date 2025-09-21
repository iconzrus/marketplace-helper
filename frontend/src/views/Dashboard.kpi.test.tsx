import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import Dashboard from './Dashboard';
import { MemoryRouter } from 'react-router-dom';
import { createContext, useContext } from 'react';
import { vi } from 'vitest';

const Ctx = createContext<any>(null);
function useOutletContextMock() { return useContext(Ctx); }
vi.mock('react-router-dom', async () => {
  const actual: any = await vi.importActual('react-router-dom');
  return { ...actual, useOutletContext: () => useOutletContextMock() };
});

// Mock axios
const mockGet = vi.fn();
vi.mock('axios', () => ({ default: { defaults: { baseURL: '' }, get: (...args: any[]) => mockGet(...args) } }));

class MockEventSource { constructor(url: string) { (globalThis as any).__es = url; setTimeout(() => {}, 0); } close() {} }
(globalThis as any).EventSource = MockEventSource as any;

it('renders KPI tiles and sparkline', async () => {
  const context: any = {
    authToken: 't', wbProducts: [], loadingWb: false, useLocalData: true, setUseLocalData: () => {}, query: '', setQuery: () => {}, brand: '', setBrand: () => {}, category: '', setCategory: () => {}, minPrice: '', setMinPrice: () => {}, maxPrice: '', setMaxPrice: () => {}, minDiscount: '', setMinDiscount: () => {}, page: 1, setPage: () => {}, totalPages: 1, pagedProducts: [], fetchWbProducts: async () => {}, handleSyncWb: async () => {}, fetchAnalytics: async () => {}, analyticsReport: null, loadingAnalytics: false, minMarginPercent: undefined, setMinMarginPercent: () => {}, handleApplyMinMargin: () => {}, handleExport: async () => {}, validationItems: null, loadingValidation: false, fetchValidation: async () => {}, handleFileUpload: async () => {}, demoMode: false, setDemoMode: () => {}, runDemoAutofill: async () => {}, genCount: '0', setGenCount: () => {}, genType: 'both', setGenType: () => {}, delCount: '0', setDelCount: () => {}, delAll: false, setDelAll: () => {}, runDemoGenerate: async () => {}, runDemoDelete: async () => {}, currency: (n?: number) => String(n ?? ''), percent: (n?: number) => String(n ?? ''), numberFormat: (n?: number) => String(n ?? ''), sourceBadge: () => '', alerts: [], fetchAlerts: async () => {}
  };
  localStorage.setItem('mh_auth_token', 't');

  // First axios call is SSE fallback or alerts; second is snapshots
  mockGet.mockResolvedValueOnce({ data: [] });
  mockGet.mockResolvedValueOnce({ data: [
    { snapshotDate: '2025-01-01', margin: 1, marginPercent: 12, stockWb: 5 },
    { snapshotDate: '2025-01-02', margin: -1, marginPercent: -2, stockWb: 15 }
  ]});

  render(
    <MemoryRouter>
      <Ctx.Provider value={context}>
        <Dashboard />
      </Ctx.Provider>
    </MemoryRouter>
  );

  await waitFor(() => expect(screen.getByText('Отрицательная маржа')).toBeInTheDocument());
  // есть три KPI карточки
  const cards = document.querySelectorAll('.card__value');
  expect(cards.length).toBeGreaterThanOrEqual(3);
});


