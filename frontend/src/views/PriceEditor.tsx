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
    await axios.post('/api/pricing/batch-update', { items });
    await ctx.fetchAnalytics();
    await loadRecs();
  };

  return (
    <section className="panel">
      <div className="panel__title">
        <h2>Массовый прайс‑эдитор</h2>
        <div className="analytics-controls">
          <label>
            Целевая маржа, %
            <input type="number" inputMode="decimal" value={target} onChange={e => setTarget(e.target.value)} />
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



