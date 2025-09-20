import React, { useMemo, useRef, useState } from 'react';
import { useOutletContext } from 'react-router-dom';
import type { AppOutletContext } from '../App';

export default function Analytics() {
  const ctx = useOutletContext<AppOutletContext>();
  const [colWidths, setColWidths] = useState<Record<string, number>>({});
  const resizingRef = useRef<{ key: string; startX: number; startW: number } | null>(null);

  const startResize = (key: string, event: React.MouseEvent<HTMLDivElement>) => {
    const th = (event.currentTarget.parentElement as HTMLTableCellElement);
    const rect = th.getBoundingClientRect();
    resizingRef.current = { key, startX: event.clientX, startW: rect.width };
    document.body.classList.add('th-resizing');
    window.addEventListener('mousemove', onMouseMove);
    window.addEventListener('mouseup', onMouseUp, { once: true });
  };
  const onMouseMove = (e: MouseEvent) => {
    const r = resizingRef.current; if (!r) return;
    const delta = e.clientX - r.startX;
    const width = Math.max(80, Math.round(r.startW + delta));
    setColWidths(prev => ({ ...prev, [r.key]: width }));
  };
  const onMouseUp = () => {
    resizingRef.current = null;
    document.body.classList.remove('th-resizing');
    window.removeEventListener('mousemove', onMouseMove);
  };
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
            <table className="table table--compact table--resizable">
              <thead>
                <tr>
                  <th className="th-resizable" style={{ width: colWidths['p_name'] }}><div>Товар</div><div className="resizer" onMouseDown={(e)=>startResize('p_name',e)} /></th>
                  <th className="th-resizable" style={{ width: colWidths['p_article'] }}><div>Артикул</div><div className="resizer" onMouseDown={(e)=>startResize('p_article',e)} /></th>
                  <th className="numeric th-resizable" style={{ width: colWidths['p_price'] }}><div>Цена WB</div><div className="resizer" onMouseDown={(e)=>startResize('p_price',e)} /></th>
                  <th className="numeric th-resizable" style={{ width: colWidths['p_purchase'] }}><div>Закупка</div><div className="resizer" onMouseDown={(e)=>startResize('p_purchase',e)} /></th>
                  <th className="numeric th-resizable" style={{ width: colWidths['p_logistics'] }}><div>Логистика</div><div className="resizer" onMouseDown={(e)=>startResize('p_logistics',e)} /></th>
                  <th className="numeric th-resizable" style={{ width: colWidths['p_marketing'] }}><div>Маркетинг</div><div className="resizer" onMouseDown={(e)=>startResize('p_marketing',e)} /></th>
                  <th className="numeric th-resizable" style={{ width: colWidths['p_other'] }}><div>Прочие</div><div className="resizer" onMouseDown={(e)=>startResize('p_other',e)} /></th>
                  <th className="numeric th-resizable" style={{ width: colWidths['p_margin'] }}><div>Маржа</div><div className="resizer" onMouseDown={(e)=>startResize('p_margin',e)} /></th>
                  <th className="numeric th-resizable" style={{ width: colWidths['p_marginp'] }}><div>Маржа %</div><div className="resizer" onMouseDown={(e)=>startResize('p_marginp',e)} /></th>
                  <th className="numeric th-resizable" style={{ width: colWidths['p_local'] }}><div>Остаток (лок.)</div><div className="resizer" onMouseDown={(e)=>startResize('p_local',e)} /></th>
                  <th className="numeric th-resizable" style={{ width: colWidths['p_wb'] }}><div>Остаток WB</div><div className="resizer" onMouseDown={(e)=>startResize('p_wb',e)} /></th>
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

          <div className="table-wrapper table-wrapper--compact">
            <h3>Требуют корректировки или сопоставления</h3>
            <table className="table table--compact table--resizable">
              <thead>
                <tr>
                  <th className="th-resizable" style={{ width: colWidths['a_name'] }}><div>Товар</div><div className="resizer" onMouseDown={(e)=>startResize('a_name',e)} /></th>
                  <th className="th-resizable" style={{ width: colWidths['a_article'] }}><div>Артикул</div><div className="resizer" onMouseDown={(e)=>startResize('a_article',e)} /></th>
                  <th className="col-actions th-resizable" style={{ width: colWidths['a_actions'] }}><div>Действия</div><div className="resizer" onMouseDown={(e)=>startResize('a_actions',e)} /></th>
                  <th className="numeric th-resizable" style={{ width: colWidths['a_price'] }}><div>Цена</div><div className="resizer" onMouseDown={(e)=>startResize('a_price',e)} /></th>
                  <th className="numeric th-resizable" style={{ width: colWidths['a_purchase'] }}><div>Закупка</div><div className="resizer" onMouseDown={(e)=>startResize('a_purchase',e)} /></th>
                  <th className="numeric th-resizable" style={{ width: colWidths['a_logistics'] }}><div>Логистика</div><div className="resizer" onMouseDown={(e)=>startResize('a_logistics',e)} /></th>
                  <th className="numeric th-resizable" style={{ width: colWidths['a_marketing'] }}><div>Маркетинг</div><div className="resizer" onMouseDown={(e)=>startResize('a_marketing',e)} /></th>
                  <th className="numeric th-resizable" style={{ width: colWidths['a_other'] }}><div>Прочие</div><div className="resizer" onMouseDown={(e)=>startResize('a_other',e)} /></th>
                  <th className="numeric th-resizable" style={{ width: colWidths['a_margin'] }}><div>Маржа</div><div className="resizer" onMouseDown={(e)=>startResize('a_margin',e)} /></th>
                  <th className="numeric th-resizable" style={{ width: colWidths['a_marginp'] }}><div>Маржа %</div><div className="resizer" onMouseDown={(e)=>startResize('a_marginp',e)} /></th>
                  <th className="numeric th-resizable" style={{ width: colWidths['a_local'] }}><div>Остаток (лок.)</div><div className="resizer" onMouseDown={(e)=>startResize('a_local',e)} /></th>
                  <th className="numeric th-resizable" style={{ width: colWidths['a_wb'] }}><div>Остаток WB</div><div className="resizer" onMouseDown={(e)=>startResize('a_wb',e)} /></th>
                  <th className="col-comments th-resizable" style={{ width: colWidths['a_comments'] }}><div>Комментарии</div><div className="resizer" onMouseDown={(e)=>startResize('a_comments',e)} /></th>
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


