import React, { useEffect, useState } from 'react';
import axios from 'axios';
import { useOutletContext } from 'react-router-dom';
import type { AppOutletContext } from '../App';

export default function WbCatalog() {
  const ctx = useOutletContext<AppOutletContext>();
  const [recs, setRecs] = useState<Record<string, { recommendedPrice: number; delta: number; target: number }>>({});
  const [targetMargin, setTargetMargin] = useState<string>('15');

  useEffect(() => {
    const load = async () => {
      const params: Record<string, string> = {};
      if (targetMargin) params.targetMarginPercent = targetMargin;
      const { data } = await axios.get('/api/pricing/recommendations', { params });
      const map: Record<string, { recommendedPrice: number; delta: number; target: number }> = {};
      for (const r of data as any[]) {
        if (r.wbArticle) {
          map[String(r.wbArticle)] = { recommendedPrice: Number(r.recommendedPrice), delta: Number(r.priceDelta), target: Number(r.targetMarginPercent) };
        }
      }
      setRecs(map);
    };
    load();
  }, [targetMargin]);
  return (
    <section className="panel">
      <div className="panel__title">
        <h2>WB Каталог</h2>
        <div className="toolbar">
          <label className="toggle">
            <input type="checkbox" checked={ctx.useLocalData} onChange={e => ctx.setUseLocalData(e.target.checked)} /> Использовать локальные данные
          </label>
          <input placeholder="Поиск по названию" value={ctx.query} onChange={e => ctx.setQuery(e.target.value)} />
          <input placeholder="Бренд" value={ctx.brand} onChange={e => ctx.setBrand(e.target.value)} />
          <input placeholder="Категория" value={ctx.category} onChange={e => ctx.setCategory(e.target.value)} />
          <input placeholder="Мин. цена" inputMode="decimal" value={ctx.minPrice} onChange={e => ctx.setMinPrice(e.target.value)} style={{ width: 100 }} />
          <input placeholder="Макс. цена" inputMode="decimal" value={ctx.maxPrice} onChange={e => ctx.setMaxPrice(e.target.value)} style={{ width: 110 }} />
          <input placeholder="Мин. скидка, %" inputMode="numeric" value={ctx.minDiscount} onChange={e => ctx.setMinDiscount(e.target.value)} style={{ width: 130 }} />
          <button className="btn btn--secondary" onClick={ctx.fetchWbProducts} disabled={ctx.loadingWb}>{ctx.loadingWb ? 'Обновление…' : 'Обновить товары'}</button>
          <button className="btn" onClick={ctx.handleSyncWb}>Синхронизировать с WB</button>
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
              <th className="numeric">Цель маржи, %</th>
              <th className="numeric">Реком. цена</th>
              <th className="numeric">Δ к текущей</th>
            </tr>
          </thead>
          <tbody>
            {ctx.loadingWb ? (
              <tr><td colSpan={11}>Загрузка…</td></tr>
            ) : ctx.wbProducts.length === 0 ? (
              <tr><td colSpan={11}>Нет данных. Попробуйте импортировать или синхронизировать товары.</td></tr>
            ) : (
              ctx.pagedProducts.map(product => (
                <tr key={`${product.nmId ?? product.id ?? product.vendorCode}`}>
                  <td>{product.nmId ?? product.vendorCode ?? '—'}</td>
                  <td>{product.name ?? '—'}</td>
                  <td>{product.brand ?? '—'}</td>
                  <td>{product.category ?? '—'}</td>
                  <td className="numeric">{ctx.currency(product.price)}</td>
                  <td className="numeric">{ctx.currency(product.priceWithDiscount ?? product.salePrice)}</td>
                  <td className="numeric">{product.discount != null ? `${product.discount}%` : '—'}</td>
                  <td className="numeric">{ctx.numberFormat(product.totalQuantity)}</td>
                  <td className="numeric"><input style={{ width: 60 }} value={targetMargin} onChange={e => setTargetMargin(e.target.value)} /></td>
                  <td className="numeric">{(() => { const r = recs[String(product.nmId ?? product.vendorCode ?? '')]; return r ? ctx.currency(r.recommendedPrice) : '—'; })()}</td>
                  <td className="numeric">{(() => { const r = recs[String(product.nmId ?? product.vendorCode ?? '')]; return r ? ctx.currency(r.delta) : '—'; })()}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
        {!ctx.loadingWb && ctx.wbProducts.length > 0 && (
          <div className="pagination">
            <span className="page-info">Стр. {ctx.page} из {ctx.totalPages}</span>
            <button onClick={() => ctx.setPage(1)} disabled={ctx.page === 1}>⏮</button>
            <button onClick={() => ctx.setPage(p => Math.max(1, (typeof p === 'number'? p : 1) - 1))} disabled={ctx.page === 1}>Назад</button>
            <button onClick={() => ctx.setPage(p => Math.min(ctx.totalPages, (typeof p === 'number'? p : 1) + 1))} disabled={ctx.page === ctx.totalPages}>Вперёд</button>
            <button onClick={() => ctx.setPage(ctx.totalPages)} disabled={ctx.page === ctx.totalPages}>⏭</button>
          </div>
        )}
      </div>
    </section>
  );
}


