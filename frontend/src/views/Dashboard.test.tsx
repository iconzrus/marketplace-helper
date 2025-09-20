import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import Dashboard from './Dashboard';
import axios from 'axios';
import { Outlet, MemoryRouter } from 'react-router-dom';

jest.mock('axios');
const mockedAxios = axios as jest.Mocked<typeof axios>;

function Wrapper({ token }: { token: string | null }) {
  const context: any = {
    authToken: token,
    // stubs for required context fields not used in this test
    wbProducts: [], loadingWb: false, useLocalData: true, setUseLocalData: () => {}, query: '', setQuery: () => {}, brand: '', setBrand: () => {}, category: '', setCategory: () => {}, minPrice: '', setMinPrice: () => {}, maxPrice: '', setMaxPrice: () => {}, minDiscount: '', setMinDiscount: () => {}, page: 1, setPage: () => {}, totalPages: 1, pagedProducts: [], fetchWbProducts: async () => {}, handleSyncWb: async () => {}, fetchAnalytics: async () => {}, analyticsReport: null, loadingAnalytics: false, minMarginPercent: undefined, setMinMarginPercent: () => {}, handleApplyMinMargin: () => {}, handleExport: async () => {}, validationItems: null, loadingValidation: false, fetchValidation: async () => {}, handleFileUpload: async () => {}, demoMode: false, setDemoMode: () => {}, runDemoAutofill: async () => {}, genCount: '0', setGenCount: () => {}, genType: 'both', setGenType: () => {}, delCount: '0', setDelCount: () => {}, delAll: false, setDelAll: () => {}, runDemoGenerate: async () => {}, runDemoDelete: async () => {}, currency: (n?: number) => String(n ?? ''), percent: (n?: number) => String(n ?? ''), numberFormat: (n?: number) => String(n ?? ''), sourceBadge: () => ''
  };
  return (
    <MemoryRouter>
      <React.Suspense>
        {/* Emulate Outlet context provider */}
        <Outlet context={context} />
        <Dashboard />
      </React.Suspense>
    </MemoryRouter>
  );
}

describe('Dashboard alerts loading', () => {
  beforeEach(() => {
    mockedAxios.get.mockReset();
  });

  it('loads alerts after mount when auth token present', async () => {
    mockedAxios.get.mockResolvedValueOnce({ data: [
      { type: 'NEGATIVE_MARGIN', wbArticle: '321757512', name: 'A', margin: -16.29, marginPercent: -3.48 }
    ]});

    render(<Wrapper token={'token'} />);

    await waitFor(() => {
      expect(screen.getByText('Отрицательная маржа')).toBeInTheDocument();
      expect(screen.getByText('1')).toBeInTheDocument();
    });
  });

  it('does not call API when no auth token and shows zeroes', async () => {
    render(<Wrapper token={null} />);
    await waitFor(() => {
      expect(screen.getByText('Отрицательная маржа')).toBeInTheDocument();
      expect(screen.getByText('0')).toBeInTheDocument();
    });
    expect(mockedAxios.get).not.toHaveBeenCalled();
  });
});


