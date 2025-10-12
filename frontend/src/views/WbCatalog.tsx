import React, { useEffect, useState } from 'react';
import axios from 'axios';
import { useOutletContext } from 'react-router-dom';
import type { AppOutletContext } from '../App';

export default function WbCatalog() {
  const ctx = useOutletContext<AppOutletContext>();
  const [recs, setRecs] = useState<Record<string, { recommendedPrice: number; delta: number; target: number }>>({});
  const [targetMargin, setTargetMargin] = useState<string>('15');
  const [contentLocale] = useState<string>('ru');
  const [contentCardsTotal, setContentCardsTotal] = useState<number | null>(null);
  const [contentCardsCursor, setContentCardsCursor] = useState<{ nmID?: number; updatedAt?: string } | null>(null);
  const [contentTrashTotal, setContentTrashTotal] = useState<number | null>(null);
  const [contentLimits, setContentLimits] = useState<any | null>(null);
  const [contentLoading, setContentLoading] = useState<boolean>(false);

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
          <button className="btn" onClick={async () => {
            setContentLoading(true);
            try {
              const { data } = await axios.post(`/api/v2/wb-api/content/cards?locale=${encodeURIComponent(contentLocale)}`);
              const total = (data?.cursor?.total) ?? (Array.isArray((data as any)?.data) ? (data as any).data.length : ((data as any)?.cards?.length ?? 0));
              setContentCardsTotal(typeof total === 'number' ? total : 0);
              setContentCardsCursor({ nmID: data?.cursor?.nmID, updatedAt: data?.cursor?.updatedAt });
            } catch (e: any) {
              setContentCardsTotal(null);
              setContentCardsCursor(null);
            } finally {
              setContentLoading(false);
            }
          }}>Проверить карточки (Контент)</button>
          <button className="btn btn--secondary" onClick={async () => {
            setContentLoading(true);
            try {
              const { data } = await axios.get(`/api/v2/wb-api/content/cards/limits?locale=${encodeURIComponent(contentLocale)}`);
              setContentLimits(data);
            } catch (e: any) {
              setContentLimits({ error: e?.response?.data?.error ?? e?.message });
            } finally {
              setContentLoading(false);
            }
          }}>Проверить лимиты</button>
          <button className="btn btn--secondary" onClick={async () => {
            setContentLoading(true);
            try {
              const { data } = await axios.post(`/api/v2/wb-api/content/cards/trash?locale=${encodeURIComponent(contentLocale)}`);
              const total = data?.cursor?.total ?? 0;
              setContentTrashTotal(total);
            } catch (e: any) {
              setContentTrashTotal(null);
            } finally {
              setContentLoading(false);
            }
          }}>Проверить корзину</button>
        </div>
      </div>
      <div className="table-wrapper">
        {(contentCardsTotal != null || contentTrashTotal != null || contentLimits) && (
          <div className="panel panel--sub" style={{ marginBottom: 12 }}>
            <div style={{ display: 'flex', gap: 16, alignItems: 'baseline', flexWrap: 'wrap' }}>
              <span><strong>Контент total</strong>: {contentCardsTotal ?? '—'}</span>
              {contentCardsCursor && (
                <span><strong>Курсор</strong>: nmID={contentCardsCursor.nmID ?? '—'}, updatedAt={contentCardsCursor.updatedAt ?? '—'}</span>
              )}
              <span><strong>Корзина total</strong>: {contentTrashTotal ?? '—'}</span>
              {contentLimits && (
                <span><strong>Лимиты</strong>: {(() => {
                  const d = (contentLimits as any)?.data ?? contentLimits;
                  const fl = d?.freeLimits; const pl = d?.paidLimits;
                  return fl != null || pl != null ? `free=${fl ?? '—'}, paid=${pl ?? '—'}` : JSON.stringify(contentLimits).slice(0, 120);
                })()}</span>
              )}
              {contentLoading && <span>Загрузка…</span>}
            </div>
          </div>
        )}
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


