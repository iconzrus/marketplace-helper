import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import Dashboard from './Dashboard';
import axios from 'axios';
import { MemoryRouter } from 'react-router-dom';
import { createContext, useContext } from 'react';

vi.mock('axios', () => {
  return {
    default: {
      get: vi.fn()
    }
  };
});
const mockedAxios = axios as any;

// Create a minimal Outlet context provider mock
const Ctx = createContext<any>(null);
function useOutletContextMock() {
  return useContext(Ctx);
}
jest.mock('react-router-dom', () => {
  const actual = jest.requireActual('react-router-dom');
  return {
    ...actual,
    useOutletContext: () => useOutletContextMock()
  };
});

function Wrapper({ token }: { token: string | null }) {
  const context: any = {
    authToken: token,
    // stubs for required context fields not used in this test
    wbProducts: [], loadingWb: false, useLocalData: true, setUseLocalData: () => {}, query: '', setQuery: () => {}, brand: '', setBrand: () => {}, category: '', setCategory: () => {}, minPrice: '', setMinPrice: () => {}, maxPrice: '', setMaxPrice: () => {}, minDiscount: '', setMinDiscount: () => {}, page: 1, setPage: () => {}, totalPages: 1, pagedProducts: [], fetchWbProducts: async () => {}, handleSyncWb: async () => {}, fetchAnalytics: async () => {}, analyticsReport: null, loadingAnalytics: false, minMarginPercent: undefined, setMinMarginPercent: () => {}, handleApplyMinMargin: () => {}, handleExport: async () => {}, validationItems: null, loadingValidation: false, fetchValidation: async () => {}, handleFileUpload: async () => {}, demoMode: false, setDemoMode: () => {}, runDemoAutofill: async () => {}, genCount: '0', setGenCount: () => {}, genType: 'both', setGenType: () => {}, delCount: '0', setDelCount: () => {}, delAll: false, setDelAll: () => {}, runDemoGenerate: async () => {}, runDemoDelete: async () => {}, currency: (n?: number) => String(n ?? ''), percent: (n?: number) => String(n ?? ''), numberFormat: (n?: number) => String(n ?? ''), sourceBadge: () => ''
  };
  // Ensure localStorage has token when provided
  if (token) localStorage.setItem('mh_auth_token', token);
  else localStorage.removeItem('mh_auth_token');
  return (
    <MemoryRouter>
      <Ctx.Provider value={context}>
        <Dashboard />
      </Ctx.Provider>
    </MemoryRouter>
  );
}

describe('Dashboard alerts loading', () => {
  beforeEach(() => {
    (mockedAxios.get as any).mockReset?.();
  });

  it('loads alerts after mount when auth token present', async () => {
    (mockedAxios.get as any).mockResolvedValueOnce({ data: [
      { type: 'NEGATIVE_MARGIN', wbArticle: '321757512', name: 'A', margin: -16.29, marginPercent: -3.48 }
    ]});

    render(<Wrapper token={'token'} />);

    await waitFor(() => {
      expect(screen.getByText('Отрицательная маржа')).toBeInTheDocument();
      expect(screen.getAllByText('1')[0]).toBeInTheDocument();
    });
  });

  it('does not call API when no auth token and shows zeroes', async () => {
    render(<Wrapper token={null} />);
    await waitFor(() => {
      expect(screen.getByText('Отрицательная маржа')).toBeInTheDocument();
      const zeros = screen.getAllByText('0');
      expect(zeros.length).toBeGreaterThanOrEqual(3);
    });
    expect((mockedAxios.get as any)).not.toHaveBeenCalled();
  });
});


