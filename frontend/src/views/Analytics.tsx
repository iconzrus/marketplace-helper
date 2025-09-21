import React, { useEffect, useRef, useState } from 'react';
import { useOutletContext } from 'react-router-dom';
import type { AppOutletContext } from '../App';

export default function Analytics() {
  const ctx = useOutletContext<AppOutletContext>();
  const attentionWrapperRef = useRef<HTMLDivElement | null>(null);
  const attentionTableRef = useRef<HTMLTableElement | null>(null);
  const topScrollRef = useRef<HTMLDivElement | null>(null);
  const [topScrollWidth, setTopScrollWidth] = useState<number>(0);

  useEffect(() => {
    const table = attentionTableRef.current;
    const bottom = attentionWrapperRef.current;
    const top = topScrollRef.current;
    if (!table || !bottom || !top) return;

    const syncFromBottom = () => {
      if (!top || !bottom) return;
      if (Math.abs((top.scrollLeft ?? 0) - (bottom.scrollLeft ?? 0)) > 1) {
        top.scrollLeft = bottom.scrollLeft;
      }
    };
    const syncFromTop = () => {
      if (!top || !bottom) return;
      if (Math.abs((bottom.scrollLeft ?? 0) - (top.scrollLeft ?? 0)) > 1) {
        bottom.scrollLeft = top.scrollLeft;
      }
    };

    bottom.addEventListener('scroll', syncFromBottom);
    top.addEventListener('scroll', syncFromTop);

    let ro: any = null;
    if (typeof (window as any).ResizeObserver === 'function') {
      ro = new (window as any).ResizeObserver(() => {
        setTopScrollWidth(table.scrollWidth);
      });
      ro.observe(table);
    }
    // initial width
    setTopScrollWidth(table.scrollWidth);

    return () => {
      bottom.removeEventListener('scroll', syncFromBottom);
      top.removeEventListener('scroll', syncFromTop);
      if (ro && table) ro.unobserve(table);
    };
  }, [ctx.analyticsReport]);
  // Placeholder: trend charts could use /api/snapshots?from=...&to=...
  return (
    <section className="panel">
      <div className="panel__title analytics-header">
        <div>
          <h2>Маржинальность и сопоставление</h2>
          {ctx.analyticsReport && (
            <div className="analytics-summary">
              <span>Всего позиций: {ctx.analyticsReport.totalProducts}</span>
              <span>Профитных: {ctx.analyticsReport.profitableCount}</span>
              <span>Нуждаются в корректировке: {ctx.analyticsReport.requiresAttentionCount}</span>
            </div>
          )}
        </div>
        <div className="analytics-controls">
          <label>
            Порог маржи, %
            <input type="number" inputMode="decimal" value={ctx.minMarginPercent ?? ''} onChange={e => ctx.setMinMarginPercent(e.target.value === '' ? undefined : Number(e.target.value))} placeholder={ctx.analyticsReport?.appliedMinMarginPercent?.toString() ?? '—'} />
          </label>
          <button onClick={ctx.handleApplyMinMargin} disabled={ctx.loadingAnalytics}>{ctx.loadingAnalytics ? 'Пересчёт…' : 'Применить'}</button>
          <button onClick={ctx.handleExport}>Скачать отчёт</button>
        </div>
      </div>

      {ctx.loadingAnalytics ? (
        <div className="message message--info">Расчёт…</div>
      ) : !ctx.analyticsReport ? (
        <div className="message message--info">Нет загруженных данных.</div>
      ) : (
        <>
          <div className="table-wrapper table-wrapper--compact">
            <h3>Профитные товары</h3>
            <table className="table table--compact">
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
                {(ctx.analyticsReport.profitable ?? []).map(item => (
                  <tr key={`${item.productId ?? item.wbProductId ?? item.wbArticle}`}>
                    <td>
                      <div className="cell-with-meta">
                        <div>{item.name ?? '—'}</div>
                        <span className="badge">{ctx.sourceBadge(item.dataSource)}</span>
                      </div>
                    </td>
                    <td>{item.wbArticle ?? item.vendorCode ?? '—'}</td>
                    <td className="numeric">{ctx.currency(item.wbDiscountPrice ?? item.wbPrice)}</td>
                    <td className="numeric">{ctx.currency(item.purchasePrice)}</td>
                    <td className="numeric">{ctx.currency(item.logisticsCost)}</td>
                    <td className="numeric">{ctx.currency(item.marketingCost)}</td>
                    <td className="numeric">{ctx.currency(item.otherExpenses)}</td>
                    <td className={`numeric ${item.margin != null && item.margin < 0 ? 'negative' : ''}`}>{ctx.currency(item.margin)}</td>
                    <td className={`numeric ${item.marginPercent != null && (item.marginPercent < 0 || item.marginBelowThreshold) ? 'warning' : ''}`}>{ctx.percent(item.marginPercent)}</td>
                    <td className="numeric">{ctx.numberFormat(item.localStock)}</td>
                    <td className="numeric">{ctx.numberFormat(item.wbStock)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Top horizontal scrollbar synced with the table below */}
          <div className="top-scrollbar" ref={topScrollRef}>
            <div style={{ width: topScrollWidth }} />
          </div>
          <div className="table-wrapper table-wrapper--compact" ref={attentionWrapperRef}>
            <h3>Требуют корректировки или сопоставления</h3>
            <table className="table table--compact" ref={attentionTableRef}>
              <thead>
                <tr>
                  <th>Товар</th>
                  <th>Артикул</th>
                  <th className="col-actions">Действия</th>
                  <th className="numeric">Цена</th>
                  <th className="numeric">Закупка</th>
                  <th className="numeric">Логистика</th>
                  <th className="numeric">Маркетинг</th>
                  <th className="numeric">Прочие</th>
                  <th className="numeric">Маржа</th>
                  <th className="numeric">Маржа %</th>
                  <th className="numeric">Остаток (лок.)</th>
                  <th className="numeric">Остаток WB</th>
                  <th className="col-comments">Комментарии</th>
                </tr>
              </thead>
              <tbody>
                {(ctx.analyticsReport.requiresAttention ?? []).map(item => (
                  <tr key={`attention-${item.productId ?? item.wbProductId ?? item.wbArticle}`}>
                    <td>
                      <div className="cell-with-meta">
                        <div>{item.name ?? '—'}</div>
                        <span className="badge badge--attention">{ctx.sourceBadge(item.dataSource)}</span>
                      </div>
                    </td>
                    <td>{item.wbArticle ?? item.vendorCode ?? '—'}</td>
                    <td className="col-actions">
                      {ctx.__openWhatIf && (
                        <button className="btn btn--secondary" onClick={() => ctx.__openWhatIf!(item)}>Что если…</button>
                      )}
                    </td>
                    <td className="numeric">{ctx.currency(item.wbDiscountPrice ?? item.wbPrice ?? item.localPrice)}</td>
                    <td className="numeric">{ctx.currency(item.purchasePrice)}</td>
                    <td className="numeric">{ctx.currency(item.logisticsCost)}</td>
                    <td className="numeric">{ctx.currency(item.marketingCost)}</td>
                    <td className="numeric">{ctx.currency(item.otherExpenses)}</td>
                    <td className={`numeric ${item.negativeMargin ? 'negative' : ''}`}>{ctx.currency(item.margin)}</td>
                    <td className={`numeric ${item.marginBelowThreshold ? 'warning' : ''}`}>{ctx.percent(item.marginPercent)}</td>
                    <td className="numeric">{ctx.numberFormat(item.localStock)}</td>
                    <td className="numeric">{ctx.numberFormat(item.wbStock)}</td>
                    <td className="col-comments">
                      {item.warnings && item.warnings.length > 0 ? (
                        <ul className="warnings">{item.warnings.map((w, i) => <li key={i}>{w}</li>)}</ul>
                      ) : '—'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </section>
  );
}


