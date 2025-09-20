import { ChangeEvent, FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import axios from 'axios';
import { computeMargin } from './utils';

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

const App = () => {
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
      await axios.post('/api/v2/wb-api/sync');
      setMessage('Синхронизация с WB выполнена.');
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
    fetchAnalytics();
    fetchWbProducts();
    fetchWbStatuses();
    fetchValidation();
  }, [authToken]);

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
    if (!authToken || !demoMode) return;
    setDemoActionLoading(true);
    setMessage(null);
    setError(null);
    try {
      await axios.post('/api/demo/fill-random-all');
      await fetchAnalytics();
      await fetchValidation();
      setMessage('Демо‑заполнение: товары объединены и данные сгенерированы.');
    } catch (err) {
      console.error(err);
      if (axios.isAxiosError(err) && err.response?.status === 401) return;
      setError('Не удалось выполнить демо‑заполнение.');
    } finally {
      setDemoActionLoading(false);
    }
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
      <div className="app">
        <header className="app__header">
          <div className="app__header-content">
            <h1>Marketplace Helper</h1>
            <p>
              Демонстрационное приложение для менеджера по маркетплейсам. Загружайте корпоративные Excel-данные,
              просматривайте товары Wildberries и анализируйте маржинальность.
            </p>
          </div>
        </header>
        <div className="auth-container">
          <section className="panel auth-panel">
            <h2>Вход в систему</h2>
            <p>
              Укажите логин и пароль, чтобы открыть рабочий кабинет и управлять товарами Wildberries.
            </p>
            {authError && <div className="message message--error">{authError}</div>}
            <form className="auth-form" onSubmit={handleLogin}>
              <label>
                <span>Логин</span>
                <input
                  value={login}
                  onChange={event => setLogin(event.target.value)}
                  autoComplete="username"
                  placeholder="Введите логин"
                  required
                />
              </label>
              <label>
                <span>Пароль</span>
                <input
                  type="password"
                  value={password}
                  onChange={event => setPassword(event.target.value)}
                  autoComplete="current-password"
                  placeholder="Введите пароль"
                  required
                />
              </label>
              <button type="submit" disabled={authLoading}>
                {authLoading ? 'Вход…' : 'Войти'}
              </button>
            </form>
          </section>
        </div>
      </div>
    );
  }

  return (
    <div className="app">
      <header className="app__header">
        <div className="app__header-content">
          <h1>Marketplace Helper</h1>
          <p>
            Демонстрационное приложение для менеджера по маркетплейсам. Загружайте корпоративные Excel-данные,
            просматривайте товары Wildberries и анализируйте маржинальность.
          </p>
        </div>
        <div className="auth-status">
          <span>
            Вошли как <span className="auth-status__user">{authUser ?? 'пользователь'}</span>
          </span>
          <button className="btn btn--secondary" onClick={() => handleLogout()}>
            Выйти
          </button>
        </div>
      </header>

      {error && <div className="message message--error">{error}</div>}

      <section className="panel">
        <h2>Импорт Excel</h2>
        <p>
          Шаблон колонок: <code>Название</code>, <code>Артикул WB</code>, <code>Штрихкод</code>, <code>Цена</code>,
          <code> Закупка</code>, <code>Логистика</code>, <code>Маркетинг</code>, <code>Прочие</code>, <code>Остаток</code>.
          Колонки можно располагать в любом порядке и именовать на русском или английском.
        </p>
        <label className="upload">
          <input type="file" accept=".xlsx,.xls" onChange={handleFileUpload} />
          <span>Загрузить Excel</span>
        </label>
        {message && <div className="message message--success">{message}</div>}
        {importResult?.warnings && importResult.warnings.length > 0 && (
          <div className="message message--warning">
            <strong>Внимание.</strong>
            <ul>
              {importResult.warnings.map((warning, index) => (
                <li key={index}>{warning}</li>
              ))}
            </ul>
          </div>
        )}
        {importResult?.errors && importResult.errors.length > 0 && (
          <div className="message message--error">
            <strong>Строки пропущены.</strong>
            <ul>
              {importResult.errors.map((warning, index) => (
                <li key={index}>{warning}</li>
              ))}
            </ul>
          </div>
        )}
      </section>

      <section className="panel">
        <div className="panel__title">
          <h2>Корректировки: причины и рекомендации</h2>
          <button className="btn btn--secondary" onClick={() => fetchValidation()} disabled={loadingValidation}>
            {loadingValidation ? 'Обновление…' : 'Обновить список'}
          </button>
        </div>
        {!validationItems ? (
          <div className="message message--info">Нет данных о корректировках. Выполните расчёт или загрузите Excel.</div>
        ) : (validationItems.filter(v => v.issues && v.issues.length > 0).length === 0) ? (
          <div className="message message--success">Все товары проходят без корректировок.</div>
        ) : (
          <div className="table-wrapper">
            <table>
              <thead>
                <tr>
                  <th>Товар</th>
                  <th>Артикул</th>
                  <th>Проблемы</th>
                  <th>Рекомендации</th>
                </tr>
              </thead>
              <tbody>
                {validationItems.filter(item => item.issues && item.issues.length > 0).map(item => (
                  <tr key={`val-${item.productId ?? item.wbArticle ?? item.name}`}>
                    <td>{item.name ?? '—'}</td>
                    <td>{item.wbArticle ?? '—'}</td>
                    <td>
                      {item.issues && item.issues.length > 0 ? (
                        <ul className="warnings">
                          {item.issues.map((iss, idx) => (
                            <li key={idx}>
                              {iss.blocking && <span className="badge badge--attention" style={{ marginRight: 6 }}>Блокер</span>}
                              <strong>{iss.field}</strong>: {iss.reason}
                            </li>
                          ))}
                        </ul>
                      ) : (
                        '—'
                      )}
                    </td>
                    <td>
                      {item.issues && item.issues.some(i => i.suggestion) ? (
                        <ul className="hints">
                          {item.issues.filter(i => i.suggestion).map((i, idx) => (
                            <li key={idx}>{i.suggestion}</li>
                          ))}
                        </ul>
                      ) : (
                        '—'
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <section className="panel">
        <div className="panel__title">
          <h2>Демо‑центр</h2>
          <div className="demo-toolbar">
            <label className="chip">
              <input type="checkbox" checked={demoMode} onChange={e => setDemoMode(e.target.checked)} /> Demo Mode
            </label>
            <div className="presets">
              <button className="btn btn--secondary" onClick={() => applyPreset('minimal')}>Пресет: Минимум</button>
              <button className="btn" onClick={runDemoAutofill} disabled={demoActionLoading || !demoMode}>
                {demoActionLoading ? 'Заполнение…' : 'Автозаполнить недостающие расходы'}
              </button>
            </div>
            <div className="gen-controls">
              <input
                inputMode="numeric"
                placeholder="Сколько сгенерировать"
                value={genCount}
                onChange={e => setGenCount(e.target.value)}
                style={{ width: 180 }}
                disabled={!demoMode}
              />
              <select value={genType} onChange={e => setGenType(e.target.value as any)} disabled={!demoMode}>
                <option value="both">WB + Excel (поровну)</option>
                <option value="excel">Только Excel</option>
                <option value="wb">Только WB</option>
              </select>
              <button className="btn" onClick={runDemoGenerate} disabled={demoActionLoading || !demoMode}>
                {demoActionLoading ? 'Генерация…' : 'Сгенерировать'}
              </button>
            </div>
            <div className="gen-controls" style={{ marginTop: 8 }}>
              <input
                inputMode="numeric"
                placeholder="Сколько удалить"
                value={delCount}
                onChange={e => setDelCount(e.target.value)}
                style={{ width: 180 }}
                disabled={!demoMode || delAll}
              />
              <label className="chip">
                <input type="checkbox" checked={delAll} onChange={e => setDelAll(e.target.checked)} disabled={!demoMode} /> Удалить всё
              </label>
              <button className="btn btn--secondary" onClick={runDemoDelete} disabled={demoActionLoading || !demoMode}>
                {demoActionLoading ? 'Удаление…' : 'Удалить'}
              </button>
            </div>
          </div>
        </div>
        {demoMode && <div className="demo-banner">Демо‑режим активен: можно безопасно экспериментировать с данными и порогами.</div>}
        {kpiDelta && (kpiDelta.movedToProfit != null || kpiDelta.remainedInFix != null) && (
          <div className="kpis">
            {kpiDelta.movedToProfit != null && <span className="delta delta--up">↑ В маржинальность: +{kpiDelta.movedToProfit}</span>}
            {kpiDelta.remainedInFix != null && <span className="delta delta--down">→ Оставались в корректировке: {kpiDelta.remainedInFix}</span>}
          </div>
        )}
      </section>

      <section className="panel">
        <div className="panel__title">
          <h2>Состояние API Wildberries</h2>
          <button className="btn btn--secondary" onClick={fetchWbStatuses} disabled={wbStatusLoading}>
            {wbStatusLoading ? 'Проверка…' : 'Обновить статусы'}
          </button>
        </div>
        {!wbStatuses ? (
          <div className="message message--info">Нет данных. Нажмите «Обновить статусы».</div>
        ) : (
          <div className="table-wrapper">
            <table>
              <thead>
                <tr>
                  <th>Эндпоинт</th>
                  <th>Путь</th>
                  <th>Статус</th>
                  <th>HTTP</th>
                  <th>Сообщение</th>
                </tr>
              </thead>
              <tbody>
                {wbStatuses.map((s, i) => (
                  <tr key={`${s.path}-${i}`}>
                    <td>{s.name}</td>
                    <td><code>{s.path}</code></td>
                    <td>
                      <span className={`badge ${s.status === 'UP' ? '' : 'badge--attention'}`}>{s.status}</span>
                    </td>
                    <td className="numeric">{s.httpStatus ?? '—'}</td>
                    <td>{s.message ?? '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <section className="panel">
        <div className="panel__title">
          <h2>Карточки Wildberries</h2>
          <div className="toolbar">
            <label className="toggle">
              <input
                type="checkbox"
                checked={useLocalData}
                onChange={e => setUseLocalData(e.target.checked)}
              />
              Использовать локальные данные
            </label>
            <input
              placeholder="Поиск по названию"
              value={query}
              onChange={e => setQuery(e.target.value)}
            />
            <input
              placeholder="Бренд"
              value={brand}
              onChange={e => setBrand(e.target.value)}
            />
            <input
              placeholder="Категория"
              value={category}
              onChange={e => setCategory(e.target.value)}
            />
            <input
              placeholder="Мин. цена"
              inputMode="decimal"
              value={minPrice}
              onChange={e => setMinPrice(e.target.value)}
              style={{ width: 100 }}
            />
            <input
              placeholder="Макс. цена"
              inputMode="decimal"
              value={maxPrice}
              onChange={e => setMaxPrice(e.target.value)}
              style={{ width: 110 }}
            />
            <input
              placeholder="Мин. скидка, %"
              inputMode="numeric"
              value={minDiscount}
              onChange={e => setMinDiscount(e.target.value)}
              style={{ width: 130 }}
            />
            <button className="btn btn--secondary" onClick={fetchWbProducts} disabled={loadingWb}>
              {loadingWb ? 'Обновление…' : 'Обновить товары'}
            </button>
            <button className="btn" onClick={handleSyncWb} disabled={syncingWb}>
              {syncingWb ? 'Синхронизация…' : 'Синхронизировать с WB'}
            </button>
            <button className="btn" onClick={() => fetchAnalytics()} disabled={loadingAnalytics}>
              {loadingAnalytics ? 'Расчёт…' : 'Рассчитать прибыльность'}
            </button>
          </div>
        </div>
        <div className="table-wrapper">
          <table>
            <thead>
              <tr>
                <th>Артикул</th>
                <th>Название</th>
                <th>Бренд</th>
                <th>Категория</th>
                <th className="numeric">Цена</th>
                <th className="numeric">Цена со скидкой</th>
                <th className="numeric">Скидка</th>
                <th className="numeric">Остаток</th>
              </tr>
            </thead>
            <tbody>
              {loadingWb ? (
                <tr>
                  <td colSpan={8}>Загрузка…</td>
                </tr>
              ) : wbProducts.length === 0 ? (
                <tr>
                  <td colSpan={8}>Нет данных. Попробуйте импортировать или синхронизировать товары.</td>
                </tr>
              ) : (
                pagedProducts.map(product => (
                  <tr key={`${product.nmId ?? product.id ?? product.vendorCode}`}>
                    <td>{product.nmId ?? product.vendorCode ?? '—'}</td>
                    <td>{product.name ?? '—'}</td>
                    <td>{product.brand ?? '—'}</td>
                    <td>{product.category ?? '—'}</td>
                    <td className="numeric">{currency(product.price)}</td>
                    <td className="numeric">{currency(product.priceWithDiscount ?? product.salePrice)}</td>
                    <td className="numeric">{product.discount != null ? `${product.discount}%` : '—'}</td>
                    <td className="numeric">{numberFormat(product.totalQuantity)}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
          {!loadingWb && wbProducts.length > 0 && (
            <div className="pagination">
              <span className="page-info">
                Стр. {page} из {totalPages}
              </span>
              <button onClick={() => setPage(1)} disabled={page === 1}>⏮</button>
              <button onClick={() => setPage(p => Math.max(1, p - 1))} disabled={page === 1}>Назад</button>
              <button onClick={() => setPage(p => Math.min(totalPages, p + 1))} disabled={page === totalPages}>Вперёд</button>
              <button onClick={() => setPage(totalPages)} disabled={page === totalPages}>⏭</button>
            </div>
          )}
        </div>
      </section>

      <section className="panel">
        <div className="panel__title analytics-header">
          <div>
            <h2>Маржинальность и сопоставление</h2>
            {analyticsReport && (
              <div className="analytics-summary">
                <span>Всего позиций: {analyticsReport.totalProducts}</span>
                <span>Профитных: {analyticsReport.profitableCount}</span>
                <span>Нуждаются в корректировке: {analyticsReport.requiresAttentionCount}</span>
              </div>
            )}
          </div>
          <div className="analytics-controls">
            <label>
              Порог маржи, %
              <input
                type="number"
                inputMode="decimal"
                value={minMarginPercent ?? ''}
                onChange={handleMinMarginChange}
                placeholder={analyticsReport?.appliedMinMarginPercent?.toString() ?? '—'}
              />
            </label>
            <button onClick={handleApplyMinMargin} disabled={loadingAnalytics}>
              {loadingAnalytics ? 'Пересчёт…' : 'Применить'}
            </button>
            <button onClick={handleExport}>Скачать отчёт</button>
          </div>
        </div>
        {loadingAnalytics ? (
          <div className="message message--info">Расчёт…</div>
        ) : !analyticsReport ? (
          <div className="message message--info">Нет загруженных данных.</div>
        ) : (
          <>
            <div className="table-wrapper">
              <h3>Профитные товары</h3>
              <table>
                <thead>
                  <tr>
                    <th>Товар</th>
                    <th>Артикул</th>
                    <th className="numeric">Цена WB</th>
                    <th className="numeric">Закупка</th>
                    <th className="numeric">Логистика</th>
                    <th className="numeric">Маркетинг</th>
                    <th className="numeric">Прочие</th>
                    <th className="numeric">Маржа</th>
                    <th className="numeric">Маржа %</th>
                    <th className="numeric">Остаток (лок.)</th>
                    <th className="numeric">Остаток WB</th>
                  </tr>
                </thead>
                <tbody>
                  {analyticsReport.profitable.length === 0 ? (
                    <tr>
                      <td colSpan={11}>Нет позиций, проходящих порог маржи. Проверьте данные и загрузите обновления.</td>
                    </tr>
                  ) : (
                    renderAnalyticsRows(analyticsReport.profitable)
                  )}
                </tbody>
              </table>
            </div>

            <div className="table-wrapper">
              <h3>Требуют корректировки или сопоставления</h3>
              <table>
                <thead>
                  <tr>
                    <th>Товар</th>
                    <th>Артикул</th>
                    <th className="numeric">Цена</th>
                    <th className="numeric">Закупка</th>
                    <th className="numeric">Логистика</th>
                    <th className="numeric">Маркетинг</th>
                    <th className="numeric">Прочие</th>
                    <th className="numeric">Маржа</th>
                    <th className="numeric">Маржа %</th>
                    <th className="numeric">Остаток (лок.)</th>
                    <th className="numeric">Остаток WB</th>
                    <th>Комментарии</th>
                  </tr>
                </thead>
                <tbody>
                  {analyticsReport.requiresAttention.length === 0 ? (
                    <tr>
                      <td colSpan={12}>Ошибок и неполных данных не обнаружено.</td>
                    </tr>
                  ) : (
                    renderAttentionRows(analyticsReport.requiresAttention)
                  )}
                </tbody>
              </table>
            </div>
          </>
        )}
      </section>

      {whatIfOpen.open && whatIfOpen.item && (
        <div className="modal-backdrop" onClick={() => setWhatIfOpen({ open: false })}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <div className="modal__header">
              <h3 className="modal__title">Что если по: {whatIfOpen.item.name ?? whatIfOpen.item.wbArticle}</h3>
              <button className="btn btn--secondary" onClick={() => setWhatIfOpen({ open: false })}>Закрыть</button>
            </div>
            <WhatIfForm base={whatIfOpen.item} onClose={() => setWhatIfOpen({ open: false })} compute={computeWhatIf} />
          </div>
        </div>
      )}
    </div>
  );
};

export default App;

// What-if component inline for simplicity
function WhatIfForm({ base, onClose, compute }: { base: ProductAnalytics; onClose: () => void; compute: (b: ProductAnalytics, c: Partial<ProductAnalytics>) => { margin: number; marginPercent?: number } }) {
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
      </div>
    </div>
  );
}
