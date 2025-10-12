import { ChangeEvent, FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import axios from 'axios';
import { computeMargin } from './utils';
import { NavLink, Outlet, useLocation } from 'react-router-dom';
import { API_BASE_URL } from './config';

if (API_BASE_URL && axios.defaults.baseURL !== API_BASE_URL) {
  axios.defaults.baseURL = API_BASE_URL;
}

interface ProductAnalytics {
  productId?: number;
  wbProductId?: number;
  name?: string;
  wbArticle?: string;
  vendorCode?: string;
  brand?: string;
  category?: string;
  localPrice?: number;
  wbPrice?: number;
  wbDiscountPrice?: number;
  purchasePrice?: number;
  logisticsCost?: number;
  marketingCost?: number;
  otherExpenses?: number;
  localStock?: number;
  wbStock?: number;
  margin?: number;
  marginPercent?: number;
  dataSource?: 'LOCAL_ONLY' | 'WB_ONLY' | 'MERGED';
  requiresCorrection?: boolean;
  profitable?: boolean;
  marginBelowThreshold?: boolean;
  negativeMargin?: boolean;
  warnings?: string[];
}

interface ProductAnalyticsReport {
  profitable: ProductAnalytics[];
  requiresAttention: ProductAnalytics[];
  allItems: ProductAnalytics[];
  appliedMinMarginPercent?: number;
  totalProducts: number;
  profitableCount: number;
  requiresAttentionCount: number;
}

interface ProductImportResult {
  created: number;
  updated: number;
  skipped: number;
  warnings?: string[];
  errors?: string[];
}

interface ProductIssue {
  field: string;
  reason: string;
  suggestion?: string;
  blocking?: boolean;
}

interface ProductValidationItem {
  productId?: number;
  name?: string;
  wbArticle?: string;
  requiresCorrection?: boolean;
  issues: ProductIssue[];
}

interface WbProduct {
  id?: number;
  nmId?: number;
  name?: string;
  vendor?: string;
  vendorCode?: string;
  brand?: string;
  category?: string;
  subject?: string;
  price?: number;
  discount?: number;
  priceWithDiscount?: number;
  salePrice?: number;
  totalQuantity?: number;
}

interface AuthSuccessResponse {
  token: string;
  username: string;
}
type SellerInfo = {
  company?: string;
  inn?: string | number;
  name?: string;
  supplierName?: string;
  supplierId?: string | number;
  [key: string]: unknown;
};

const currency = (value?: number) =>
  value != null ? value.toLocaleString('ru-RU', { style: 'currency', currency: 'RUB' }) : '—';

const percent = (value?: number) =>
  value != null ? `${value.toFixed(2)}%` : '—';

const numberFormat = (value?: number) =>
  value != null ? value.toLocaleString('ru-RU') : '—';

const sourceBadge = (value?: ProductAnalytics['dataSource']) => {
  switch (value) {
    case 'MERGED':
      return 'WB + Excel';
    case 'LOCAL_ONLY':
      return 'Excel';
    case 'WB_ONLY':
      return 'Wildberries';
    default:
      return '—';
  }
};

export type AppOutletContext = {
  // Auth
  authToken: string | null;
  // WB Catalog
  wbProducts: WbProduct[];
  loadingWb: boolean;
  useLocalData: boolean;
  setUseLocalData: (v: boolean) => void;
  query: string; setQuery: (v: string) => void;
  brand: string; setBrand: (v: string) => void;
  category: string; setCategory: (v: string) => void;
  minPrice: string; setMinPrice: (v: string) => void;
  maxPrice: string; setMaxPrice: (v: string) => void;
  minDiscount: string; setMinDiscount: (v: string) => void;
  page: number; setPage: (v: number | ((p:number)=>number)) => void;
  totalPages: number;
  pagedProducts: WbProduct[];
  fetchWbProducts: () => Promise<void>;
  handleSyncWb: () => Promise<void>;
  fetchAnalytics: (o?: { minMarginPercent?: number }) => Promise<void>;

  // Analytics
  analyticsReport: ProductAnalyticsReport | null;
  loadingAnalytics: boolean;
  minMarginPercent: number | undefined;
  setMinMarginPercent: (v: number | undefined) => void;
  handleApplyMinMargin: () => void;
  handleExport: () => Promise<void>;

  // Corrections
  validationItems: ProductValidationItem[] | null;
  loadingValidation: boolean;
  fetchValidation: (o?: { minMarginPercent?: number }) => Promise<void>;

  // Import
  handleFileUpload: (e: ChangeEvent<HTMLInputElement>) => Promise<void>;

  // Demo
  demoMode: boolean; setDemoMode: (v: boolean) => void;
  runDemoAutofill: () => Promise<void>;
  generateMockCabinet: () => Promise<void>;
  genCount: string; setGenCount: (v: string) => void;
  genType: 'both'|'excel'|'wb'; setGenType: (v: 'both'|'excel'|'wb') => void;
  delCount: string; setDelCount: (v: string) => void;
  delAll: boolean; setDelAll: (v: boolean) => void; runDemoGenerate: () => Promise<void>; runDemoDelete: () => Promise<void>;

  // helpers
  currency: (v?: number) => string;
  percent: (v?: number) => string;
  numberFormat: (v?: number) => string;
  sourceBadge: (v?: ProductAnalytics['dataSource']) => string;
  // actions
  __openWhatIf?: (item: ProductAnalytics) => void;
  // Alerts
  alerts: { type: 'LOW_MARGIN' | 'NEGATIVE_MARGIN' | 'LOW_STOCK'; wbArticle?: string; name?: string; margin?: number; marginPercent?: number; localStock?: number; wbStock?: number; }[];
  fetchAlerts: () => Promise<void>;
};

const App = () => {
  useEffect(() => {
    axios.defaults.baseURL = API_BASE_URL || '';
  }, []);
  const [analyticsReport, setAnalyticsReport] = useState<ProductAnalyticsReport | null>(null);
  const [wbProducts, setWbProducts] = useState<WbProduct[]>([]);
  const [loadingAnalytics, setLoadingAnalytics] = useState(false);
  const [loadingWb, setLoadingWb] = useState(false);
  const [syncingWb, setSyncingWb] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [importResult, setImportResult] = useState<ProductImportResult | null>(null);
  const [minMarginPercent, setMinMarginPercent] = useState<number | undefined>(undefined);
  const [useLocalData, setUseLocalData] = useState<boolean>(true);
  const [query, setQuery] = useState('');
  const [brand, setBrand] = useState('');
  const [category, setCategory] = useState('');
  const [minPrice, setMinPrice] = useState<string>('');
  const [maxPrice, setMaxPrice] = useState<string>('');
  const [minDiscount, setMinDiscount] = useState<string>('');
  const [page, setPage] = useState(1);
  const [authToken, setAuthToken] = useState<string | null>(null);
  const [authUser, setAuthUser] = useState<string | null>(null);
  const [sellerInfo, setSellerInfo] = useState<SellerInfo | null>(null);
  const [mockMode, setMockMode] = useState<boolean>(false);
  const [login, setLogin] = useState('');
  const [password, setPassword] = useState('');
  const [authError, setAuthError] = useState<string | null>(null);
  const [authLoading, setAuthLoading] = useState(false);
  const pageSize = 10;
  const [wbStatuses, setWbStatuses] = useState<{ name: string; path: string; status: 'UP' | 'DOWN'; httpStatus?: number; message?: string }[] | null>(null);
  const [wbStatusLoading, setWbStatusLoading] = useState(false);
  const [demoMode, setDemoMode] = useState<boolean>(false);
  const [kpiDelta, setKpiDelta] = useState<{ movedToProfit?: number; remainedInFix?: number } | null>(null);
  const [whatIfOpen, setWhatIfOpen] = useState<{ open: boolean; item?: ProductAnalytics }>({ open: false });
  const [validationItems, setValidationItems] = useState<ProductValidationItem[] | null>(null);
  const [loadingValidation, setLoadingValidation] = useState(false);
  const [demoActionLoading, setDemoActionLoading] = useState(false);
  const [genCount, setGenCount] = useState<string>('10');
  const [genType, setGenType] = useState<'both' | 'excel' | 'wb'>('both');
  const [delCount, setDelCount] = useState<string>('5');
  const [delAll, setDelAll] = useState<boolean>(false);
  const [alerts, setAlerts] = useState<{ type: 'LOW_MARGIN' | 'NEGATIVE_MARGIN' | 'LOW_STOCK'; wbArticle?: string; name?: string; margin?: number; marginPercent?: number; localStock?: number; wbStock?: number; }[]>([]);
  const [theme, setTheme] = useState<string>(() => localStorage.getItem('mh_theme') || (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'));

  const location = useLocation();
  const isRoot = location.pathname === '/';

  const fetchWbStatuses = async () => {
    setWbStatusLoading(true);
    try {
      const { data } = await axios.get<{ checkedAt: string; endpoints: typeof wbStatuses }>('/api/wb-status');
      setWbStatuses(data.endpoints ?? []);
    } catch (err) {
      console.error(err);
      setWbStatuses(null);
    } finally {
      setWbStatusLoading(false);
    }
  };
  const fetchSellerInfo = async () => {
    if (!authToken) return;
    try {
      const { data } = await axios.get<SellerInfo>('/api/v2/wb-api/seller-info');
      setSellerInfo(data ?? null);
    } catch (_) {
      setSellerInfo(null);
    }
  };

  const fetchMockMode = async () => {
    if (!authToken) return;
    try {
      const { data } = await axios.get<{ mock: boolean }>('/api/v2/wb-api/mock-mode');
      setMockMode(Boolean((data as any)?.mock));
    } catch (_) {}
  };

  const handleLogout = useCallback((message?: string) => {
    setAuthToken(null);
    setAuthUser(null);
    localStorage.removeItem('mh_auth_token');
    localStorage.removeItem('mh_auth_username');
    setAnalyticsReport(null);
    setWbProducts([]);
    setMessage(null);
    setImportResult(null);
    setError(null);
    setLoadingAnalytics(false);
    setLoadingWb(false);
    setSyncingWb(false);
    setAuthError(message ?? null);
    setAuthLoading(false);
    setLogin('');
    setPassword('');
  }, []);

  useEffect(() => {
    const storedToken = localStorage.getItem('mh_auth_token');
    const storedUsername = localStorage.getItem('mh_auth_username');
    const storedDemo = localStorage.getItem('mh_demo_mode');
    if (storedToken) {
      setAuthToken(storedToken);
    }
    if (storedUsername) {
      setAuthUser(storedUsername);
    }
    if (storedDemo != null) setDemoMode(storedDemo === 'true');
  }, []);

  useEffect(() => {
    if (authToken) {
      axios.defaults.headers.common.Authorization = `Bearer ${authToken}`;
    } else {
      delete axios.defaults.headers.common.Authorization;
    }
  }, [authToken]);

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('mh_theme', theme);
  }, [theme]);

  useEffect(() => {
    // Refresh mock mode when navigating to ensure nav is updated
    if (authToken) {
      fetchMockMode();
    }
  }, [location.pathname, authToken]);

  useEffect(() => {
    const interceptor = axios.interceptors.response.use(
      response => response,
      error => {
        if (axios.isAxiosError(error) && error.response?.status === 401) {
          handleLogout('Сессия истекла. Войдите повторно.');
        }
        return Promise.reject(error);
      }
    );
    return () => {
      axios.interceptors.response.eject(interceptor);
    };
  }, [handleLogout]);

  useEffect(() => {
    localStorage.setItem('mh_demo_mode', String(demoMode));
  }, [demoMode]);

  const handleLogin = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setAuthLoading(true);
    setAuthError(null);
    try {
      const { data } = await axios.post<AuthSuccessResponse>('/api/auth/login', {
        username: login,
        password
      });
      setAuthToken(data.token);
      setAuthUser(data.username);
      localStorage.setItem('mh_auth_token', data.token);
      localStorage.setItem('mh_auth_username', data.username);
      setLogin('');
      setPassword('');
      setError(null);
      // Redirect to accounts page after login
      window.location.hash = '#/accounts';
    } catch (err) {
      console.error(err);
      if (axios.isAxiosError(err) && err.response?.status === 401) {
        setAuthError('Неверный логин или пароль.');
      } else {
        setAuthError('Не удалось выполнить вход. Попробуйте ещё раз.');
      }
    } finally {
      setAuthLoading(false);
    }
  };

  const fetchAnalytics = async (override?: { minMarginPercent?: number }) => {
    if (!authToken) {
      return;
    }
    setLoadingAnalytics(true);
    setError(null);
    try {
      const appliedMinMargin = override?.minMarginPercent ?? minMarginPercent;
      const params: Record<string, unknown> = {
        includeWithoutWb: true,
        includeUnprofitable: true
      };
      if (appliedMinMargin != null) {
        params.minMarginPercent = appliedMinMargin;
      }
      const { data } = await axios.get<ProductAnalyticsReport>('/api/analytics/products', {
        params
      });
      setAnalyticsReport(data);
      if (override?.minMarginPercent !== undefined) {
        setMinMarginPercent(override.minMarginPercent);
      } else if (minMarginPercent === undefined && data.appliedMinMarginPercent != null) {
        setMinMarginPercent(Number(data.appliedMinMarginPercent));
      }
    } catch (err) {
      console.error(err);
      if (axios.isAxiosError(err) && err.response?.status === 401) {
        return;
      }
      setError('Не удалось получить расчётные данные. Проверьте подключение к бэкенду.');
    } finally {
      setLoadingAnalytics(false);
    }
  };

  const fetchWbProducts = async () => {
    if (!authToken) {
      return;
    }
    setLoadingWb(true);
    setError(null);
    try {
      const params: Record<string, unknown> = { useLocalData };
      if (query) params.name = query;
      if (brand) params.brand = brand;
      if (category) params.category = category;
      if (minPrice) params.minPrice = minPrice;
      if (maxPrice) params.maxPrice = maxPrice;
      if (minDiscount) params.minDiscount = minDiscount;
      const { data } = await axios.get<WbProduct[]>('/api/v2/list/goods/filter', { params });
      setWbProducts(data);
    } catch (err) {
      console.error(err);
      if (axios.isAxiosError(err) && err.response?.status === 401) {
        return;
      }
      setError('Не удалось получить товары Wildberries.');
    } finally {
      setLoadingWb(false);
    }
  };

  const fetchValidation = async (override?: { minMarginPercent?: number }) => {
    if (!authToken) return;
    setLoadingValidation(true);
    try {
      const appliedMinMargin = override?.minMarginPercent ?? minMarginPercent;
      const params: Record<string, unknown> = { includeWithoutWb: true };
      if (appliedMinMargin != null) params.minMarginPercent = appliedMinMargin;
      const { data } = await axios.get<ProductValidationItem[]>('/api/analytics/validation', { params });
      setValidationItems(data);
    } catch (err) {
      console.error(err);
      setValidationItems([]);
    } finally {
      setLoadingValidation(false);
    }
  };

  const handleSyncWb = async () => {
    if (!authToken) {
      return;
    }
    setSyncingWb(true);
    setError(null);
    setMessage(null);
    try {
      const { data } = await axios.post('/api/v2/wb-api/sync');
      if (data && typeof data === 'object') {
        const fetched = (data as any).fetched ?? 0;
        const inserted = (data as any).inserted ?? 0;
        const updated = (data as any).updated ?? 0;
        setMessage(`WB: получено ${fetched}, добавлено ${inserted}, обновлено ${updated}.`);
      } else {
        setMessage('Синхронизация с WB выполнена.');
      }
      await fetchWbProducts();
    } catch (err) {
      console.error(err);
      if (axios.isAxiosError(err) && err.response?.status === 401) {
        return;
      }
      setError('Не удалось синхронизироваться с WB.');
    } finally {
      setSyncingWb(false);
    }
  };

  useEffect(() => {
    if (!authToken) {
      return;
    }
    
    const initData = async () => {
      // First fetch mock mode status
      await fetchMockMode();
      
      // Then fetch other data (seller info depends on mock mode)
      fetchAnalytics();
      fetchWbProducts();
      fetchWbStatuses();
      fetchValidation();
      fetchAlerts();
    };
    
    initData();
  }, [authToken]);

  // Update seller info when mock mode changes (only fetch if mock is OFF)
  useEffect(() => {
    if (authToken && !mockMode) {
      fetchSellerInfo();
    } else if (mockMode) {
      // Clear seller info when entering mock mode
      setSellerInfo(null);
    }
  }, [mockMode, authToken]);

  useEffect(() => {
    if (!authToken) {
      return;
    }
    fetchWbProducts();
  }, [useLocalData, authToken]);

  // Persist settings (theme omitted here, can be added via class on <html>)
  useEffect(() => {
    const storedLocal = localStorage.getItem('mh_useLocalData');
    const storedMinMargin = localStorage.getItem('mh_minMarginPercent');
    if (storedLocal != null) setUseLocalData(storedLocal === 'true');
    if (storedMinMargin != null) setMinMarginPercent(Number(storedMinMargin));
  }, []);

  useEffect(() => {
    localStorage.setItem('mh_useLocalData', String(useLocalData));
  }, [useLocalData]);

  useEffect(() => {
    if (minMarginPercent != null) localStorage.setItem('mh_minMarginPercent', String(minMarginPercent));
  }, [minMarginPercent]);

  const filteredProducts = useMemo(() => wbProducts, [wbProducts]);
  const totalPages = Math.max(1, Math.ceil(filteredProducts.length / pageSize));
  const pagedProducts = useMemo(
    () => filteredProducts.slice((page - 1) * pageSize, page * pageSize),
    [filteredProducts, page]
  );

  const handleFileUpload = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) {
      return;
    }

    if (!authToken) {
      (event.target as HTMLInputElement).value = '';
      return;
    }

    const formData = new FormData();
    formData.append('file', file);

    setMessage(null);
    setError(null);
    setImportResult(null);

    try {
      const { data } = await axios.post<ProductImportResult>('/api/products/import/excel', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
      setImportResult(data);
      setMessage(
        `Файл «${file.name}» загружен. Создано ${data.created}, обновлено ${data.updated}, пропущено ${data.skipped}.`
      );
      const before = analyticsReport;
      await fetchAnalytics();
      // Простейшая метрика перемещения
      setTimeout(() => {
        setKpiDelta(prev => {
          const after = analyticsReport;
          if (!before || !after) return prev;
          const moved = Math.max(0, after.profitableCount - before.profitableCount);
          const remained = Math.max(0, after.requiresAttentionCount - before.requiresAttentionCount);
          return { movedToProfit: moved, remainedInFix: remained };
        });
      }, 0);
    } catch (err) {
      console.error(err);
      if (axios.isAxiosError(err) && err.response?.status === 401) {
        return;
      }
      setError('Ошибка при загрузке файла. Убедитесь, что формат соответствует шаблону.');
    } finally {
      (event.target as HTMLInputElement).value = '';
    }
  };

  const applyPreset = async (preset: 'minimal' | 'typical' | 'edge') => {
    // Демонстрационные пресеты: запросим у бэка заполнение мок‑данных (если появится эндпоинт),
    // пока просто дернём синхронизацию и обновим данные.
    try {
      setMessage(null);
      if (preset === 'minimal') {
        setMinMarginPercent(10);
      } else if (preset === 'typical') {
        setMinMarginPercent(15);
      } else {
        setMinMarginPercent(20);
      }
      await fetchAnalytics({ minMarginPercent });
      await fetchWbProducts();
    } catch (_) {}
  };

  const runDemoAutofill = async () => {
    if (!authToken) return;
    setDemoActionLoading(true);
    setMessage(null);
    setError(null);
    try {
      await axios.post('/api/demo/fill-random-all');
      await fetchWbProducts();
      await fetchSellerInfo();
      await fetchAnalytics();
      await fetchValidation();
      setMessage('Сгенерирован mock‑кабинет: 100–300 товаров и продавец.');
    } catch (err) {
      console.error(err);
      if (axios.isAxiosError(err) && err.response?.status === 401) return;
      setError('Не удалось сгенерировать mock‑кабинет.');
    } finally {
      setDemoActionLoading(false);
    }
  };

  const generateMockCabinet = async () => {
    // отдельная явная кнопка может использовать этот метод
    return runDemoAutofill();
  };

  const runDemoGenerate = async () => {
    if (!authToken || !demoMode) return;
    setDemoActionLoading(true);
    setMessage(null);
    setError(null);
    try {
      const n = Math.max(0, Number(genCount || '0'));
      await axios.post(`/api/demo/generate?count=${n}&type=${genType}`);
      await fetchWbProducts();
      await fetchAnalytics();
      await fetchValidation();
      setMessage(`Сгенерировано ${n} объектов (${genType}).`);
    } catch (err) {
      console.error(err);
      if (axios.isAxiosError(err) && err.response?.status === 401) return;
      setError('Не удалось сгенерировать данные.');
    } finally {
      setDemoActionLoading(false);
    }
  };

  const runDemoDelete = async () => {
    if (!authToken || !demoMode) return;
    setDemoActionLoading(true);
    setMessage(null);
    setError(null);
    try {
      const n = Math.max(0, Number(delCount || '0'));
      const params = new URLSearchParams();
      if (!delAll) params.set('count', String(n));
      params.set('all', String(delAll));
      await axios.post(`/api/demo/delete?${params.toString()}`);
      await fetchWbProducts();
      await fetchAnalytics();
      await fetchValidation();
      setMessage(delAll ? 'Все данные удалены.' : `Удалено до ${n} позиций в каждом источнике.`);
    } catch (err) {
      console.error(err);
      if (axios.isAxiosError(err) && err.response?.status === 401) return;
      setError('Не удалось удалить данные.');
    } finally {
      setDemoActionLoading(false);
    }
  };

  const fetchAlerts = async () => {
    if (!authToken) return;
    try {
      const { data } = await axios.get('/api/alerts');
      setAlerts(Array.isArray(data) ? data : []);
    } catch (_) {
      setAlerts([]);
    }
  };

  const openWhatIf = (item: ProductAnalytics) => {
    setWhatIfOpen({ open: true, item });
  };

  const computeWhatIf = (base: ProductAnalytics, changes: Partial<ProductAnalytics>) => {
    const price = (changes.wbDiscountPrice ?? changes.wbPrice ?? base.wbDiscountPrice ?? base.wbPrice ?? base.localPrice);
    return computeMargin(
      price,
      changes.purchasePrice ?? base.purchasePrice,
      changes.logisticsCost ?? base.logisticsCost,
      changes.marketingCost ?? base.marketingCost,
      changes.otherExpenses ?? base.otherExpenses
    );
  };

  const handleMinMarginChange = (event: ChangeEvent<HTMLInputElement>) => {
    const value = event.target.value;
    if (value === '') {
      setMinMarginPercent(undefined);
    } else {
      setMinMarginPercent(Number(value));
    }
  };

  const handleApplyMinMargin = () => {
    fetchAnalytics({ minMarginPercent });
    fetchValidation({ minMarginPercent });
  };

  const handleExport = async () => {
    if (!authToken) {
      return;
    }
    try {
      const params: Record<string, unknown> = { includeWithoutWb: true };
      if (minMarginPercent != null) {
        params.minMarginPercent = minMarginPercent;
      }
      const response = await axios.get<Blob>('/api/analytics/products/export', {
        params,
        responseType: 'blob'
      });
      const blob = new Blob([response.data], {
        type: response.headers['content-type'] ??
          'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
      });
      let filename = 'analytics-report.xlsx';
      const disposition = response.headers['content-disposition'];
      if (typeof disposition === 'string') {
        const match = /filename\*=UTF-8''([^;]+)|filename="?([^";]+)"?/i.exec(disposition);
        const encoded = match?.[1] ?? match?.[2];
        if (encoded) {
          try {
            filename = decodeURIComponent(encoded);
          } catch (_) {
            filename = encoded;
          }
        }
      }
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', filename);
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (err) {
      console.error(err);
      if (axios.isAxiosError(err) && err.response?.status === 401) {
        return;
      }
      setError('Не удалось сформировать отчёт.');
    }
  };

  const renderAnalyticsRows = (items: ProductAnalytics[]) =>
    items.map(item => (
      <tr key={`${item.productId ?? item.wbProductId ?? item.wbArticle}`}>
        <td>
          <div className="cell-with-meta">
            <div>{item.name ?? '—'}</div>
            <span className="badge">{sourceBadge(item.dataSource)}</span>
          </div>
        </td>
        <td>{item.wbArticle ?? item.vendorCode ?? '—'}</td>
        <td className="numeric">{currency(item.wbDiscountPrice ?? item.wbPrice)}</td>
        <td className="numeric">{currency(item.purchasePrice)}</td>
        <td className="numeric">{currency(item.logisticsCost)}</td>
        <td className="numeric">{currency(item.marketingCost)}</td>
        <td className="numeric">{currency(item.otherExpenses)}</td>
        <td className={`numeric ${item.margin != null && item.margin < 0 ? 'negative' : ''}`}>{currency(item.margin)}</td>
        <td className={`numeric ${item.marginPercent != null && (item.marginPercent < 0 || item.marginBelowThreshold) ? 'warning' : ''}`}>
          {percent(item.marginPercent)}
        </td>
        <td className="numeric">{numberFormat(item.localStock)}</td>
        <td className="numeric">{numberFormat(item.wbStock)}</td>
      </tr>
    ));

  const renderAttentionRows = (items: ProductAnalytics[]) =>
    items.map(item => (
      <tr key={`attention-${item.productId ?? item.wbProductId ?? item.wbArticle}`}>
        <td>
          <div className="cell-with-meta">
            <div>{item.name ?? '—'}</div>
            <span className="badge badge--attention">{sourceBadge(item.dataSource)}</span>
          </div>
        </td>
        <td>{item.wbArticle ?? item.vendorCode ?? '—'}</td>
        <td className="numeric">{currency(item.wbDiscountPrice ?? item.wbPrice ?? item.localPrice)}</td>
        <td className="numeric">{currency(item.purchasePrice)}</td>
        <td className="numeric">{currency(item.logisticsCost)}</td>
        <td className="numeric">{currency(item.marketingCost)}</td>
        <td className="numeric">{currency(item.otherExpenses)}</td>
        <td className={`numeric ${item.negativeMargin ? 'negative' : ''}`}>{currency(item.margin)}</td>
        <td className={`numeric ${item.marginBelowThreshold ? 'warning' : ''}`}>{percent(item.marginPercent)}</td>
        <td className="numeric">{numberFormat(item.localStock)}</td>
        <td className="numeric">{numberFormat(item.wbStock)}</td>
        <td>
          {item.warnings && item.warnings.length > 0 ? (
            <>
              <ul className="warnings">
                {item.warnings.map((warning, index) => (
                  <li key={index}>{warning}</li>
                ))}
              </ul>
              <button className="btn btn--secondary" onClick={() => openWhatIf(item)}>Что если…</button>
            </>
          ) : (
            <button className="btn btn--secondary" onClick={() => openWhatIf(item)}>Что если…</button>
          )}
        </td>
      </tr>
    ));

  if (!authToken) {
    return (
      <div style={{ 
        minHeight: '100vh', 
        display: 'flex', 
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        background: 'var(--bg-app)',
        padding: 'var(--space-6)'
      }}>
        <div style={{ 
          maxWidth: '480px', 
          width: '100%',
          textAlign: 'center',
          marginBottom: 'var(--space-8)'
        }}>
          <div style={{
            width: '64px',
            height: '64px',
            background: 'linear-gradient(135deg, var(--brand-500), var(--brand-700))',
            borderRadius: 'var(--radius-lg)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            margin: '0 auto var(--space-5)',
            fontSize: '2rem',
            color: 'white',
            fontWeight: 700
          }}>M</div>
          <h1 style={{ fontSize: '2rem', marginBottom: 'var(--space-3)' }}>Marketplace Helper</h1>
          <p className="text-secondary" style={{ fontSize: '1rem' }}>
            Демонстрационное приложение для менеджера по маркетплейсам
          </p>
        </div>
        
        <div className="card" style={{ maxWidth: '420px', width: '100%' }}>
          <div className="card__header">
            <h2 className="card__title">Вход в систему</h2>
            <p className="card__description">Укажите логин и пароль для доступа к кабинету</p>
          </div>
          
          {authError && <div className="message message--error">{authError}</div>}
          
          <form onSubmit={handleLogin} style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-4)' }}>
            <div>
              <label style={{ display: 'block', marginBottom: 'var(--space-2)', fontWeight: 500, fontSize: '0.875rem' }}>
                Логин
              </label>
              <input 
                value={login} 
                onChange={event => setLogin(event.target.value)} 
                autoComplete="username" 
                placeholder="Введите логин" 
                required 
              />
            </div>
            
            <div>
              <label style={{ display: 'block', marginBottom: 'var(--space-2)', fontWeight: 500, fontSize: '0.875rem' }}>
                Пароль
              </label>
              <input 
                type="password" 
                value={password} 
                onChange={event => setPassword(event.target.value)} 
                autoComplete="current-password" 
                placeholder="Введите пароль" 
                required 
              />
            </div>
            
            <button type="submit" className="btn btn--primary btn--lg" disabled={authLoading} style={{ marginTop: 'var(--space-2)' }}>
              {authLoading ? 'Вход…' : 'Войти'}
            </button>
          </form>
        </div>
      </div>
    );
  }

  const isAccountsPage = location.pathname === '/' || location.pathname === '/accounts';
  
  // Get current page title
  const getPageTitle = () => {
    const path = location.pathname;
    if (path === '/' || path === '/accounts') return 'Мои кабинеты';
    if (path === '/dashboard') return 'Dashboard';
    if (path === '/wb') return 'WB Каталог';
    if (path === '/analytics') return 'Аналитика';
    if (path === '/prices') return 'Прайс-редактор';
    if (path === '/corrections') return 'Корректировки';
    if (path === '/import') return 'Импорт данных';
    if (path === '/demo') return 'Демо-центр';
    if (path === '/wb-status') return 'Стабильность WB API';
    return 'Marketplace Helper';
  };

  return (
    <div className="app">
      {/* Sidebar Navigation */}
      {!isAccountsPage && (
        <aside className="sidebar">
          <div className="sidebar__header">
            <NavLink to="/accounts" className="sidebar__logo">
              <div className="sidebar__logo-icon">M</div>
              <span>Marketplace Helper</span>
            </NavLink>
          </div>
          
          <nav className="sidebar__nav">
            <div className="sidebar__section">
              <div className="sidebar__section-title">Главное</div>
              <NavLink to="/dashboard" className="sidebar__link">
                <span className="sidebar__link-icon">📊</span>
                <span>Dashboard</span>
              </NavLink>
              <NavLink to="/wb" className="sidebar__link">
                <span className="sidebar__link-icon">📦</span>
                <span>WB Каталог</span>
              </NavLink>
              <NavLink to="/analytics" className="sidebar__link">
                <span className="sidebar__link-icon">📈</span>
                <span>Аналитика</span>
              </NavLink>
            </div>
            
            <div className="sidebar__section">
              <div className="sidebar__section-title">Управление</div>
              <NavLink to="/prices" className="sidebar__link">
                <span className="sidebar__link-icon">💰</span>
                <span>Прайс-редактор</span>
              </NavLink>
              <NavLink to="/corrections" className="sidebar__link">
                <span className="sidebar__link-icon">✏️</span>
                <span>Корректировки</span>
              </NavLink>
              <NavLink to="/import" className="sidebar__link">
                <span className="sidebar__link-icon">📥</span>
                <span>Импорт</span>
              </NavLink>
            </div>
            
            <div className="sidebar__section">
              <div className="sidebar__section-title">Система</div>
              {mockMode && (
                <NavLink to="/demo" className="sidebar__link">
                  <span className="sidebar__link-icon">🧪</span>
                  <span>Демо-центр</span>
                </NavLink>
              )}
              <NavLink to="/wb-status" className="sidebar__link">
                <span className="sidebar__link-icon">🔌</span>
                <span>Стабильность WB</span>
              </NavLink>
              <NavLink to="/accounts" className="sidebar__link">
                <span className="sidebar__link-icon">⚙️</span>
                <span>Кабинеты</span>
              </NavLink>
            </div>
          </nav>
          
          <div className="sidebar__footer">
            <div className="flex flex-col gap-2">
              <label className="toggle">
                <input type="checkbox" checked={theme === 'dark'} onChange={e => setTheme(e.target.checked ? 'dark' : 'light')} />
                <span className="text-sm">Тёмная тема</span>
              </label>
            </div>
          </div>
        </aside>
      )}

      {/* Main Content Area */}
      <main className="main">
        {/* Top Header */}
        <header className="main__header">
          <div className="main__header-left">
            <div>
              <h1 className="main__title">{getPageTitle()}</h1>
              {sellerInfo && (() => {
                const name = sellerInfo?.company || (sellerInfo as any)?.supplierName || (sellerInfo as any)?.name;
                const inn = (sellerInfo as any)?.inn || (sellerInfo as any)?.INN || (sellerInfo as any)?.taxpayerId;
                const isMock = (sellerInfo as any)?.mock || mockMode;
                if (!name && isMock) return null;
                const suffix = isMock ? ' (Тестовый)' : '';
                return (
                  <p className="main__subtitle">
                    {name ? `${name}${inn ? ` · ИНН ${inn}` : ''}${suffix}` : (isMock ? 'Тестовый кабинет' : '')}
                  </p>
                );
              })()}
            </div>
          </div>
          <div className="main__header-right">
            <span className="text-sm text-secondary">{authUser}</span>
            <button className="btn btn--ghost btn--sm" onClick={() => handleLogout()}>Выйти</button>
          </div>
        </header>

        {/* Content Area */}
        <div className="main__content">
          {error && <div className="message message--error">{error}</div>}
          {message && <div className="message message--success">{message}</div>}
          
          <Outlet context={{
          authToken,
          // WB
          wbProducts, loadingWb, useLocalData, setUseLocalData, query, setQuery, brand, setBrand, category, setCategory, minPrice, setMinPrice, maxPrice, setMaxPrice, minDiscount, setMinDiscount, page, setPage, totalPages, pagedProducts, fetchWbProducts, handleSyncWb, fetchAnalytics,
          // Analytics
          analyticsReport, loadingAnalytics, minMarginPercent, setMinMarginPercent, handleApplyMinMargin, handleExport,
          // Corrections
          validationItems, loadingValidation, fetchValidation,
          // Import
          handleFileUpload,
          // Demo
          demoMode, setDemoMode, runDemoAutofill, generateMockCabinet, genCount, setGenCount, genType, setGenType, delCount, setDelCount, delAll, setDelAll, runDemoGenerate, runDemoDelete,
          // helpers
          currency, percent, numberFormat, sourceBadge,
          __openWhatIf: openWhatIf,
          alerts,
          fetchAlerts
        } as AppOutletContext} />
        </div>
      </main>

      {whatIfOpen.open && whatIfOpen.item && (
        <div className="modal-backdrop" onClick={() => setWhatIfOpen({ open: false })}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <div className="modal__header">
              <h3 className="modal__title">Что если по: {whatIfOpen.item.name ?? whatIfOpen.item.wbArticle}</h3>
              <button className="btn btn--secondary" onClick={() => setWhatIfOpen({ open: false })}>Закрыть</button>
            </div>
            <WhatIfForm
              base={whatIfOpen.item}
              onClose={() => setWhatIfOpen({ open: false })}
              onApplied={async () => {
                await fetchAnalytics();
                await fetchValidation();
              }}
              compute={computeWhatIf}
            />
          </div>
        </div>
      )}
    </div>
  );
};

export default App;

// What-if component inline for simplicity
function WhatIfForm({ base, onClose, onApplied, compute }: { base: ProductAnalytics; onClose: () => void; onApplied?: () => Promise<void> | void; compute: (b: ProductAnalytics, c: Partial<ProductAnalytics>) => { margin: number; marginPercent?: number } }) {
  const [price, setPrice] = useState<string>(String(base.wbDiscountPrice ?? base.wbPrice ?? base.localPrice ?? ''));
  const [purchase, setPurchase] = useState<string>(String(base.purchasePrice ?? ''));
  const [logistics, setLogistics] = useState<string>(String(base.logisticsCost ?? ''));
  const [marketing, setMarketing] = useState<string>(String(base.marketingCost ?? ''));
  const [other, setOther] = useState<string>(String(base.otherExpenses ?? ''));

  const parsed = {
    wbDiscountPrice: price === '' ? undefined : Number(price),
    purchasePrice: purchase === '' ? undefined : Number(purchase),
    logisticsCost: logistics === '' ? undefined : Number(logistics),
    marketingCost: marketing === '' ? undefined : Number(marketing),
    otherExpenses: other === '' ? undefined : Number(other)
  } as Partial<ProductAnalytics>;

  const res = compute(base, parsed);

  return (
    <div className="whatif">
      <div className="field">
        <label>Цена (со скидкой)</label>
        <input inputMode="decimal" value={price} onChange={e => setPrice(e.target.value)} />
      </div>
      <div className="field">
        <label>Закупка</label>
        <input inputMode="decimal" value={purchase} onChange={e => setPurchase(e.target.value)} />
      </div>
      <div className="field">
        <label>Логистика</label>
        <input inputMode="decimal" value={logistics} onChange={e => setLogistics(e.target.value)} />
      </div>
      <div className="field">
        <label>Маркетинг</label>
        <input inputMode="decimal" value={marketing} onChange={e => setMarketing(e.target.value)} />
      </div>
      <div className="field">
        <label>Прочие</label>
        <input inputMode="decimal" value={other} onChange={e => setOther(e.target.value)} />
      </div>
      <div className="out">
        <div>Новая маржа: {currency(res.margin)}</div>
        <div>Маржа %: {res.marginPercent != null ? res.marginPercent.toFixed(2) + '%' : '—'}</div>
      </div>
      <div className="modal__footer">
        <button className="btn btn--secondary" onClick={onClose}>Закрыть</button>
        {base.productId && (
          <button
            className="btn"
            onClick={async () => {
              try {
                await axios.patch(`/api/products/${base.productId}/costs`, {
                  purchasePrice: purchase === '' ? null : Number(purchase),
                  logisticsCost: logistics === '' ? null : Number(logistics),
                  marketingCost: marketing === '' ? null : Number(marketing),
                  otherExpenses: other === '' ? null : Number(other)
                });
                if (onApplied) await onApplied();
                onClose();
              } catch (e) {}
            }}
          >Применить</button>
        )}
      </div>
    </div>
  );
}
