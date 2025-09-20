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

function Wrapper({ report }: { report: any }) {
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
  };
  return (
    <MemoryRouter>
      <Ctx.Provider value={context}>
        <Analytics />
      </Ctx.Provider>
    </MemoryRouter>
  );
}

describe('Analytics table UX', () => {
  const sample = {
    totalProducts: 1,
    profitableCount: 1,
    requiresAttentionCount: 0,
    profitable: [{ name: 'A', wbArticle: '1', wbPrice: 10 }],
    requiresAttention: []
  };

  it('renders resizable headers', () => {
    const { container } = render(<Wrapper report={sample} />);
    const resizers = container.querySelectorAll('.resizer');
    expect(resizers.length).toBeGreaterThan(0);
  });

  it('resizes a column on drag', () => {
    const { container } = render(<Wrapper report={sample} />);
    // Find the header cell for "Артикул"
    const th = screen.getAllByText('Артикул')[0].closest('th') as HTMLTableCellElement;
    expect(th).toBeInTheDocument();
    const resizer = th.querySelector('.resizer') as HTMLDivElement;
    expect(resizer).toBeInTheDocument();

    // Drag the resizer to the right
    fireEvent.mouseDown(resizer, { clientX: 0 });
    fireEvent.mouseMove(window, { clientX: 120 });
    fireEvent.mouseUp(window);

    // Width should be set inline (>= 80px because of clamp)
    const width = (th.style.width || '').replace('px','');
    expect(Number(width)).toBeGreaterThanOrEqual(80);
  });
});


