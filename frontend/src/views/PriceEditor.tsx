import React, { useEffect, useMemo, useState } from 'react';
import axios from 'axios';
import { useOutletContext } from 'react-router-dom';
import type { AppOutletContext } from '../App';

type Recommendation = {
  wbProductId?: number;
  wbArticle?: string;
  name?: string;
  currentPrice?: number;
  targetMarginPercent?: number;
  recommendedPrice?: number;
  priceDelta?: number;
};

export default function PriceEditor() {
  const ctx = useOutletContext<AppOutletContext>();
  const [target, setTarget] = useState<string>(() => String(ctx.minMarginPercent ?? '15'));
  const [recs, setRecs] = useState<Recommendation[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<Record<string, boolean>>({});
  const [dryRun, setDryRun] = useState(true);
  const [roundingRule, setRoundingRule] = useState<'NONE'|'NEAREST_1'|'NEAREST_5'|'NEAREST_10'>('NEAREST_1');
  const [floorPrice, setFloorPrice] = useState<string>('');
  const [ceilPrice, setCeilPrice] = useState<string>('');
  const [maxDeltaPercent, setMaxDeltaPercent] = useState<string>('');

  const targetNum = useMemo(() => (target === '' ? undefined : Number(target)), [target]);

  const loadRecs = async () => {
    if (!ctx.authToken) return;
    setLoading(true);
    try {
      const params: Record<string, unknown> = {};
      if (targetNum != null) params.targetMarginPercent = targetNum;
      const { data } = await axios.get<Recommendation[]>('/api/pricing/recommendations', { params });
      setRecs(Array.isArray(data) ? data : []);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadRecs();
  }, [ctx.authToken]);

  const toggleAll = (checked: boolean) => {
    const next: Record<string, boolean> = {};
    for (const r of recs) {
      const id = String(r.wbProductId ?? r.wbArticle ?? r.name ?? '');
      if (!id) continue;
      next[id] = checked;
    }
    setSelected(next);
  };

  const applySelected = async () => {
    const items = recs
      .filter(r => selected[String(r.wbProductId ?? r.wbArticle ?? r.name ?? '')])
      .map(r => ({ wbProductId: r.wbProductId, wbArticle: r.wbArticle, newPrice: r.recommendedPrice }));
    if (!items.length) return;
    const body: any = { items, dryRun, roundingRule };
    if (floorPrice !== '') body.floorPrice = Number(floorPrice);
    if (ceilPrice !== '') body.ceilPrice = Number(ceilPrice);
    if (maxDeltaPercent !== '') body.maxDeltaPercent = Number(maxDeltaPercent);
    await axios.post('/api/pricing/batch-update', body);
    await ctx.fetchAnalytics();
    await loadRecs();
  };

  return (
    <section className="panel">
      <div className="panel__title">
        <h2>Массовый прайс‑эдитор</h2>
        <div className="analytics-controls" style={{ gap: 8, display: 'flex', flexWrap: 'wrap', alignItems: 'flex-end' }}>
          <label>
            Целевая маржа, %
            <input type="number" inputMode="decimal" value={target} onChange={e => setTarget(e.target.value)} />
          </label>
          <label>
            Округление
            <select value={roundingRule} onChange={e => setRoundingRule(e.target.value as any)}>
              <option value="NONE">Без округления</option>
              <option value="NEAREST_1">До 1 ₽</option>
              <option value="NEAREST_5">До 5 ₽</option>
              <option value="NEAREST_10">До 10 ₽</option>
            </select>
          </label>
          <label>
            Минимальная цена
            <input type="number" inputMode="decimal" value={floorPrice} onChange={e => setFloorPrice(e.target.value)} />
          </label>
          <label>
            Максимальная цена
            <input type="number" inputMode="decimal" value={ceilPrice} onChange={e => setCeilPrice(e.target.value)} />
          </label>
          <label>
            Лимит изменения, %
            <input type="number" inputMode="decimal" value={maxDeltaPercent} onChange={e => setMaxDeltaPercent(e.target.value)} />
          </label>
          <label className="toggle">
            <input type="checkbox" checked={dryRun} onChange={e => setDryRun(e.target.checked)} /> Dry‑run
          </label>
          <button onClick={loadRecs} disabled={loading}>{loading ? 'Загрузка…' : 'Рассчитать цены'}</button>
          <button onClick={applySelected} disabled={loading}>Применить выбранные</button>
        </div>
      </div>

      {recs.length === 0 ? (
        <div className="message">Рекомендации не найдены.</div>
      ) : (
        <div className="table-wrapper">
          <table className="table table--compact">
            <thead>
              <tr>
                <th>
                  <input type="checkbox" onChange={e => toggleAll(e.target.checked)} />
                </th>
                <th>Артикул</th>
                <th>Товар</th>
                <th className="numeric">Текущая цена</th>
                <th className="numeric">Рекомменд. цена</th>
                <th className="numeric">Δ Цена</th>
              </tr>
            </thead>
            <tbody>
              {recs.map(r => {
                const id = String(r.wbProductId ?? r.wbArticle ?? r.name ?? '');
                return (
                  <tr key={`rec-${id}`}>
                    <td><input type="checkbox" checked={!!selected[id]} onChange={e => setSelected(prev => ({ ...prev, [id]: e.target.checked }))} /></td>
                    <td>{r.wbArticle ?? '—'}</td>
                    <td>{r.name ?? '—'}</td>
                    <td className="numeric">{ctx.currency(r.currentPrice)}</td>
                    <td className="numeric">{ctx.currency(r.recommendedPrice)}</td>
                    <td className="numeric">{ctx.currency(r.priceDelta)}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}



