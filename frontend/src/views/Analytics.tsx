import React, { useEffect, useMemo, useRef, useState } from 'react';
import { useOutletContext } from 'react-router-dom';
import type { AppOutletContext } from '../App';

export default function Analytics() {
  const ctx = useOutletContext<AppOutletContext>();
  const attentionWrapperRef = useRef<HTMLDivElement | null>(null);
  const attentionTableRef = useRef<HTMLTableElement | null>(null);
  const topScrollRef = useRef<HTMLDivElement | null>(null);
  const [topScrollWidth, setTopScrollWidth] = useState<number>(0);
  const [selected, setSelected] = useState<Record<string, boolean>>({});
  const [hidden, setHidden] = useState<Record<string, boolean>>({});

  // Persist hidden attention items between sessions
  useEffect(() => {
    try {
      const raw = localStorage.getItem('mh_hidden_attention_ids');
      if (raw) setHidden(JSON.parse(raw) as Record<string, boolean>);
    } catch (_) {}
  }, []);
  useEffect(() => {
    try {
      localStorage.setItem('mh_hidden_attention_ids', JSON.stringify(hidden));
    } catch (_) {}
  }, [hidden]);

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
  
  const appliedThreshold = useMemo(() => {
    return ctx.minMarginPercent ?? Number(ctx.analyticsReport?.appliedMinMarginPercent ?? 0);
  }, [ctx.minMarginPercent, ctx.analyticsReport?.appliedMinMarginPercent]);

  const getStatusClass = (marginPercent?: number | null, negative?: boolean, below?: boolean) => {
    if (marginPercent == null) return '';
    if (negative) return 'status--bad';
    const thr = appliedThreshold ?? 0;
    if (marginPercent < 0) return 'status--bad';
    if (marginPercent < thr || below) return 'status--warn';
    return 'status--ok';
  };

  const toggleSelectAll = (checked: boolean, items: any[]) => {
    const next: Record<string, boolean> = { ...selected };
    for (const it of items) {
      const id = String(it.productId ?? it.wbProductId ?? it.wbArticle ?? '');
      if (!id) continue;
      next[id] = checked;
    }
    setSelected(next);
  };

  const toggleOne = (id: string, checked: boolean) => {
    setSelected(prev => ({ ...prev, [id]: checked }));
  };

  const hideSelected = (items: any[]) => {
    const toHideIds = items
      .map(it => String(it.productId ?? it.wbProductId ?? it.wbArticle ?? ''))
      .filter(id => id && selected[id]);
    if (!toHideIds.length) return;
    setHidden(prev => {
      const next = { ...prev } as Record<string, boolean>;
      for (const id of toHideIds) next[id] = true;
      return next;
    });
    setSelected(prev => {
      const next = { ...prev } as Record<string, boolean>;
      for (const id of toHideIds) delete next[id];
      return next;
    });
  };

  const resetHidden = () => setHidden({});
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
                    <td className={`numeric ${item.marginPercent != null && (item.marginPercent < 0 || item.marginBelowThreshold) ? 'warning' : ''}`}>
                      <span className={`status-dot ${getStatusClass(item.marginPercent, item.negativeMargin, item.marginBelowThreshold)}`} /> {ctx.percent(item.marginPercent)}
                    </td>
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
            <div className="toolbar">
              <button className="btn btn--secondary" onClick={() => hideSelected((ctx.analyticsReport.requiresAttention ?? []))}>Скрыть выбранные</button>
              <button className="btn btn--secondary" onClick={resetHidden}>Показать все</button>
            </div>
            <table className="table table--compact" ref={attentionTableRef}>
              <thead>
                <tr>
                  <th>Товар</th>
                  <th>Артикул</th>
                  <th className="col-actions">
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                      <input type="checkbox" onChange={e => toggleSelectAll(e.target.checked, (ctx.analyticsReport.requiresAttention ?? []).filter(it => !hidden[String(it.productId ?? it.wbProductId ?? it.wbArticle ?? '')]))} />
                      Действия
                    </div>
                  </th>
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
                {(ctx.analyticsReport.requiresAttention ?? [])
                  .filter(item => !hidden[String(item.productId ?? item.wbProductId ?? item.wbArticle ?? '')])
                  .map(item => {
                    const id = String(item.productId ?? item.wbProductId ?? item.wbArticle ?? '');
                    return (
                  <tr key={`attention-${id}`}>
                    <td>
                      <div className="cell-with-meta">
                        <div>{item.name ?? '—'}</div>
                        <span className="badge badge--attention">{ctx.sourceBadge(item.dataSource)}</span>
                      </div>
                    </td>
                    <td>{item.wbArticle ?? item.vendorCode ?? '—'}</td>
                    <td className="col-actions">
                      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                        <input type="checkbox" checked={!!selected[id]} onChange={e => toggleOne(id, e.target.checked)} />
                        {ctx.__openWhatIf && (
                          <button className="btn btn--secondary" onClick={() => ctx.__openWhatIf!(item)}>Что если…</button>
                        )}
                      </div>
                    </td>
                    <td className="numeric">{ctx.currency(item.wbDiscountPrice ?? item.wbPrice ?? item.localPrice)}</td>
                    <td className="numeric">{ctx.currency(item.purchasePrice)}</td>
                    <td className="numeric">{ctx.currency(item.logisticsCost)}</td>
                    <td className="numeric">{ctx.currency(item.marketingCost)}</td>
                    <td className="numeric">{ctx.currency(item.otherExpenses)}</td>
                    <td className={`numeric ${item.negativeMargin ? 'negative' : ''}`}>{ctx.currency(item.margin)}</td>
                    <td className={`numeric ${item.marginBelowThreshold ? 'warning' : ''}`}>
                      <span className={`status-dot ${getStatusClass(item.marginPercent, item.negativeMargin, item.marginBelowThreshold)}`} /> {ctx.percent(item.marginPercent)}
                    </td>
                    <td className="numeric">{ctx.numberFormat(item.localStock)}</td>
                    <td className="numeric">{ctx.numberFormat(item.wbStock)}</td>
                    <td className="col-comments">
                      {item.warnings && item.warnings.length > 0 ? (
                        <ul className="warnings">{item.warnings.map((w, i) => <li key={i}>{w}</li>)}</ul>
                      ) : '—'}
                    </td>
                  </tr>
                );})}
              </tbody>
            </table>
          </div>
        </>
      )}
    </section>
  );
}


