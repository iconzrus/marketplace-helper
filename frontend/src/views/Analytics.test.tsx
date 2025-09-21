import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import Analytics from './Analytics';
import { MemoryRouter } from 'react-router-dom';
import { createContext, useContext } from 'react';
import { vi } from 'vitest';

// Minimal Outlet context mock
const Ctx = createContext<any>(null);
function useOutletContextMock() {
  return useContext(Ctx);
}
vi.mock('react-router-dom', async () => {
  const actual: any = await vi.importActual('react-router-dom');
  return { ...actual, useOutletContext: () => useOutletContextMock() };
});

function Wrapper({ report, extra }: { report: any; extra?: Record<string, any> }) {
  const context: any = {
    // analytics
    analyticsReport: report,
    loadingAnalytics: false,
    minMarginPercent: undefined,
    setMinMarginPercent: () => {},
    handleApplyMinMargin: () => {},
    handleExport: () => {},
    // helpers
    currency: (n?: number) => String(n ?? ''),
    percent: (n?: number) => String(n ?? ''),
    numberFormat: (n?: number) => String(n ?? ''),
    sourceBadge: (v?: any) => (v === 'MERGED' ? 'WB + Excel' : v === 'LOCAL_ONLY' ? 'Excel' : v === 'WB_ONLY' ? 'Wildberries' : '—'),
    ...(extra ?? {})
  };
  return (
    <MemoryRouter>
      <Ctx.Provider value={context}>
        <Analytics />
      </Ctx.Provider>
    </MemoryRouter>
  );
}

describe('Analytics table UX (render)', () => {
  it('renders both tables without resizer handles', () => {
    const sample = {
      totalProducts: 1,
      profitableCount: 1,
      requiresAttentionCount: 1,
      profitable: [{ name: 'A', wbArticle: '1', wbPrice: 10 }],
      requiresAttention: [{ name: 'B', wbArticle: '2', wbPrice: 12 }]
    };

    const { container } = render(<Wrapper report={sample} />);
    // headers present
    expect(screen.getAllByText('Артикул')[0]).toBeInTheDocument();
    expect(screen.getAllByText('Остаток WB')[0]).toBeInTheDocument();
    // ensure no legacy resizer handles in DOM
    const resizers = container.querySelectorAll('.resizer');
    expect(resizers.length).toBe(0);
  });

  it('shows action button in attention table when handler provided', () => {
    const sample = {
      totalProducts: 1,
      profitableCount: 0,
      requiresAttentionCount: 1,
      profitable: [],
      requiresAttention: [{ name: 'X', wbArticle: '3', wbPrice: 15 }]
    };

    render(<Wrapper report={sample} extra={{ __openWhatIf: () => {} }} />);
    expect(screen.getByText('Что если…')).toBeInTheDocument();
  });
});


