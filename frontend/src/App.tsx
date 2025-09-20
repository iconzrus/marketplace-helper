import { ChangeEvent, useEffect, useMemo, useState } from 'react';
import axios from 'axios';

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
  const pageSize = 10;

  const fetchAnalytics = async (override?: { minMarginPercent?: number }) => {
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
      setError('Не удалось получить расчётные данные. Проверьте подключение к бэкенду.');
    } finally {
      setLoadingAnalytics(false);
    }
  };

  const fetchWbProducts = async () => {
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
      setError('Не удалось получить товары Wildberries.');
    } finally {
      setLoadingWb(false);
    }
  };

  const handleSyncWb = async () => {
    setSyncingWb(true);
    setError(null);
    setMessage(null);
    try {
      await axios.post('/api/v2/wb-api/sync');
      setMessage('Синхронизация с WB выполнена.');
      await fetchWbProducts();
    } catch (err) {
      console.error(err);
      setError('Не удалось синхронизироваться с WB.');
    } finally {
      setSyncingWb(false);
    }
  };

  useEffect(() => {
    fetchAnalytics();
    fetchWbProducts();
  }, []);

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
      await fetchAnalytics();
    } catch (err) {
      console.error(err);
      setError('Ошибка при загрузке файла. Убедитесь, что формат соответствует шаблону.');
    } finally {
      (event.target as HTMLInputElement).value = '';
    }
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
  };

  const handleExport = () => {
    const params = new URLSearchParams({ includeWithoutWb: 'true' });
    if (minMarginPercent != null) {
      params.append('minMarginPercent', String(minMarginPercent));
    }
    window.open(`/api/analytics/products/export?${params.toString()}`, '_blank');
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
            <ul className="warnings">
              {item.warnings.map((warning, index) => (
                <li key={index}>{warning}</li>
              ))}
            </ul>
          ) : (
            '—'
          )}
        </td>
      </tr>
    ));

  return (
    <div className="app">
      <header className="app__header">
        <h1>Marketplace Helper</h1>
        <p>
          Демонстрационное приложение для менеджера по маркетплейсам. Загружайте корпоративные Excel-данные,
          просматривайте товары Wildberries и анализируйте маржинальность.
        </p>
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
    </div>
  );
};

export default App;
