import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import ImportView from './ImportView';
import { MemoryRouter } from 'react-router-dom';
import { createContext, useContext } from 'react';

// Provide a minimal context
const Ctx = createContext<any>(null);
function useOutletContextMock() { return useContext(Ctx); }
vi.mock('react-router-dom', async () => {
  const actual: any = await vi.importActual('react-router-dom');
  return { ...actual, useOutletContext: () => useOutletContextMock() };
});

describe('ImportView dry-run preview', () => {
  beforeEach(() => {
    (globalThis as any).fetch = vi.fn().mockResolvedValue({ json: async () => ({ created: 1, updated: 2, skipped: 0 }) });
  });
  it('shows preview block after selecting file in dry-run mode', async () => {
    const ctx: any = { handleFileUpload: vi.fn() };
    const { container } = render(
      <MemoryRouter>
        <Ctx.Provider value={ctx}>
          <ImportView />
        </Ctx.Provider>
      </MemoryRouter>
    );
    const input = container.querySelector('input[type="file"]') as HTMLInputElement;
    // simulate selecting a file
    Object.defineProperty(input, 'files', { value: [new File(['a'], 'a.xlsx', { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })] });
    fireEvent.change(input);

    await waitFor(() => expect(screen.getByText(/создано 1, обновлено 2/i)).toBeInTheDocument());
  });
});


