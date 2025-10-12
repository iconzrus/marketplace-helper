import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import App from './App';
import { MemoryRouter } from 'react-router-dom';
import { vi } from 'vitest';

// Minimal axios mock to satisfy App side-effects
vi.mock('axios', () => {
  const get = vi.fn(async (url: string) => {
    if (url.includes('/api/wb-status')) return { data: { checkedAt: new Date().toISOString(), endpoints: [] } } as any;
    if (url.includes('/api/analytics/products')) return { data: { profitable: [], requiresAttention: [], allItems: [], totalProducts: 0, profitableCount: 0, requiresAttentionCount: 0 } } as any;
    if (url.includes('/api/analytics/validation')) return { data: [] } as any;
    if (url.includes('/api/v2/list/goods/filter')) return { data: [] } as any;
    if (url.includes('/api/alerts')) return { data: [] } as any;
    return { data: {} } as any;
  });
  const post = vi.fn(async () => ({ data: {} }));
  const defaults = { headers: { common: {} as any }, baseURL: '' } as any;
  const interceptors = { response: { use: vi.fn(() => 0), eject: vi.fn() } } as any;
  const isAxiosError = (e: any) => !!e?.isAxiosError;
  return { default: { get, post, defaults, interceptors, isAxiosError } };
});

// Suppress Outlet rendering to avoid nested routes in this unit test
vi.mock('react-router-dom', async () => {
  const actual: any = await vi.importActual('react-router-dom');
  return {
    ...actual,
    Outlet: () => null
  };
});

describe('Theme toggle', () => {
  beforeEach(() => {
    localStorage.setItem('mh_auth_token', 'test-token');
    localStorage.setItem('mh_auth_username', 'tester');
    document.documentElement.removeAttribute('data-theme');
    localStorage.removeItem('mh_theme');
  });

  it('switches to dark theme and persists preference', async () => {
    render(
      <MemoryRouter initialEntries={['/dashboard']}>
        <App />
      </MemoryRouter>
    );

    // дождёмся появления переключателя (он теперь в sidebar, поэтому нужна не Accounts страница)
    const checkbox = await screen.findByLabelText('Тёмная тема');
    expect(document.documentElement.getAttribute('data-theme')).toBe('light');

    // включаем тёмную тему
    fireEvent.click(checkbox);
    await waitFor(() => {
      expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
      expect(localStorage.getItem('mh_theme')).toBe('dark');
    });

    // выключаем обратно
    fireEvent.click(checkbox);
    await waitFor(() => {
      expect(document.documentElement.getAttribute('data-theme')).toBe('light');
      expect(localStorage.getItem('mh_theme')).toBe('light');
    });
  });
});


