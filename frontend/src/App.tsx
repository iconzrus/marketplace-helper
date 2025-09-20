import { ChangeEvent, useEffect, useState } from 'react';
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
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [importResult, setImportResult] = useState<ProductImportResult | null>(null);
  const [minMarginPercent, setMinMarginPercent] = useState<number | undefined>(undefined);

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
      const { data } = await axios.get<WbProduct[]>('/api/v2/list/goods/filter', {
        params: { useLocalData: true }
      });
      setWbProducts(data);
    } catch (err) {
      console.error(err);
      setError('Не удалось получить товары Wildberries.');
    } finally {
      setLoadingWb(false);
    }
  };

  useEffect(() => {
    fetchAnalytics();
    fetchWbProducts();
  }, []);

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
      event.target.value = '';
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
        <td>{currency(item.wbDiscountPrice ?? item.wbPrice)}</td>
        <td>{currency(item.purchasePrice)}</td>
        <td>{currency(item.logisticsCost)}</td>
        <td>{currency(item.marketingCost)}</td>
        <td>{currency(item.otherExpenses)}</td>
        <td className={item.margin != null && item.margin < 0 ? 'negative' : ''}>{currency(item.margin)}</td>
        <td
          className={
            item.marginPercent != null && (item.marginPercent < 0 || item.marginBelowThreshold)
              ? 'warning'
              : ''
          }
        >
          {percent(item.marginPercent)}
        </td>
        <td>{numberFormat(item.localStock)}</td>
        <td>{numberFormat(item.wbStock)}</td>
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
        <td>{currency(item.wbDiscountPrice ?? item.wbPrice ?? item.localPrice)}</td>
        <td>{currency(item.purchasePrice)}</td>
        <td>{currency(item.logisticsCost)}</td>
        <td>{currency(item.marketingCost)}</td>
        <td>{currency(item.otherExpenses)}</td>
        <td className={item.negativeMargin ? 'negative' : ''}>{currency(item.margin)}</td>
        <td className={item.marginBelowThreshold ? 'warning' : ''}>{percent(item.marginPercent)}</td>
        <td>{numberFormat(item.localStock)}</td>
        <td>{numberFormat(item.wbStock)}</td>
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
        {error && <div className="message message--error">{error}</div>}
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
          <button onClick={fetchWbProducts} disabled={loadingWb}>
            {loadingWb ? 'Обновление…' : 'Обновить'}
          </button>
        </div>
        <div className="table-wrapper">
          <table>
            <thead>
              <tr>
                <th>Артикул</th>
                <th>Название</th>
                <th>Бренд</th>
                <th>Категория</th>
                <th>Цена</th>
                <th>Цена со скидкой</th>
                <th>Скидка</th>
                <th>Остаток</th>
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
                wbProducts.map(product => (
                  <tr key={`${product.nmId ?? product.id ?? product.vendorCode}`}>
                    <td>{product.nmId ?? product.vendorCode ?? '—'}</td>
                    <td>{product.name ?? '—'}</td>
                    <td>{product.brand ?? '—'}</td>
                    <td>{product.category ?? '—'}</td>
                    <td>{currency(product.price)}</td>
                    <td>{currency(product.priceWithDiscount ?? product.salePrice)}</td>
                    <td>{product.discount != null ? `${product.discount}%` : '—'}</td>
                    <td>{numberFormat(product.totalQuantity)}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
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
                    <th>Цена WB</th>
                    <th>Закупка</th>
                    <th>Логистика</th>
                    <th>Маркетинг</th>
                    <th>Прочие</th>
                    <th>Маржа</th>
                    <th>Маржа %</th>
                    <th>Остаток (лок.)</th>
                    <th>Остаток WB</th>
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
                    <th>Цена</th>
                    <th>Закупка</th>
                    <th>Логистика</th>
                    <th>Маркетинг</th>
                    <th>Прочие</th>
                    <th>Маржа</th>
                    <th>Маржа %</th>
                    <th>Остаток (лок.)</th>
                    <th>Остаток WB</th>
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
