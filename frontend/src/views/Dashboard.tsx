import React, { useEffect, useMemo, useState } from 'react';
import axios from 'axios';
import { API_BASE_URL } from '../config';
import { useOutletContext } from 'react-router-dom';
import type { AppOutletContext } from '../App';
import { Area, AreaChart, Tooltip, ResponsiveContainer } from 'recharts';

type Alert = {
  type: 'LOW_MARGIN' | 'NEGATIVE_MARGIN' | 'LOW_STOCK';
  wbArticle?: string;
  name?: string;
  margin?: number;
  marginPercent?: number;
  localStock?: number;
  wbStock?: number;
};

export default function Dashboard() {
  const ctx = (() => {
    try {
      return useOutletContext<AppOutletContext>();
    } catch (_) {
      return undefined as unknown as AppOutletContext;
    }
  })();
  const [alerts, setAlerts] = useState<Alert[]>([]);
  const [loading, setLoading] = useState(false);
  const [snap, setSnap] = useState<{ date: string; lowMargin: number; negative: number; lowStock: number }[]>([]);

  const token = ctx?.authToken ?? (typeof localStorage !== 'undefined' ? localStorage.getItem('mh_auth_token') : null);

  useEffect(() => {
    let cancelled = false;
    let es: EventSource | null = null;
    if (!token) {
      // Без токена не показываем загрузку и сразу очищаем алерты
      setAlerts([]);
      setLoading(false);
      return () => { cancelled = true; };
    }
    const sleep = (ms: number) => new Promise(res => setTimeout(res, ms));
    const loadWithRetry = async () => {
      setLoading(true);
      const base = (axios as any)?.defaults?.baseURL || API_BASE_URL || '';
      const url = base ? `${base.replace(/\/$/, '')}/api/alerts` : '/api/alerts';
      for (let attempt = 0; attempt < 8 && !cancelled; attempt++) {
        try {
          const currentToken = (ctx as any)?.authToken ?? (typeof localStorage !== 'undefined' ? localStorage.getItem('mh_auth_token') : null);
          if (!currentToken) {
            await sleep(200);
            continue;
          }
          if (typeof (ctx as any)?.fetchAlerts === 'function') {
            await (ctx as any).fetchAlerts();
            const list = (ctx as any).alerts as Alert[] | undefined;
            if (Array.isArray(list)) {
              if (!cancelled) setAlerts(list);
              break;
            }
          } else {
            const { data } = await axios.get<Alert[]>(url, { headers: { Authorization: `Bearer ${currentToken}` } });
            if (!cancelled) setAlerts(data ?? []);
            break;
          }
        } catch (e: any) {
          // 401 / network → подождать и попробовать снова
          await sleep(300);
        }
      }
      if (!cancelled) setLoading(false);
    };
    // Try SSE first
    try {
      const base = (axios as any)?.defaults?.baseURL || API_BASE_URL || '';
      const sseUrl = (base ? `${base.replace(/\/$/, '')}` : '') + `/api/alerts/stream?token=${encodeURIComponent(token)}`;
      es = new EventSource(sseUrl);
      es.onmessage = (ev) => {
        try {
          const data = JSON.parse(ev.data);
          if (Array.isArray(data)) setAlerts(data);
        } catch (_) {}
      };
      es.onerror = () => {
        es?.close();
        loadWithRetry();
      };
      setLoading(false);
    } catch (_) {
      loadWithRetry();
    }

    return () => { cancelled = true; es?.close?.(); };
  }, [token]);

  // Load snapshots to render KPI trends
  useEffect(() => {
    let cancelled = false;
    const load = async () => {
      const currentToken = (ctx as any)?.authToken ?? (typeof localStorage !== 'undefined' ? localStorage.getItem('mh_auth_token') : null);
      if (!currentToken) return;
      try {
        const base = (axios as any)?.defaults?.baseURL || API_BASE_URL || '';
        const to = new Date();
        const from = new Date(Date.now() - 1000 * 60 * 60 * 24 * 29);
        const url = (base ? `${base.replace(/\/$/, '')}` : '') + `/api/snapshots?from=${from.toISOString().slice(0,10)}&to=${to.toISOString().slice(0,10)}`;
        const { data } = await axios.get<any[]>(url, { headers: { Authorization: `Bearer ${currentToken}` } });
        if (cancelled) return;
        // naive aggregation: per day counts by thresholds similar to AlertService
        const byDate: Record<string, { lowMargin: number; negative: number; lowStock: number }> = {};
        for (const s of (data ?? [])) {
          const d = s.snapshotDate ?? s.date ?? (s.snapshot_date);
          if (!d) continue;
          const key = String(d);
          const entry = byDate[key] || { lowMargin: 0, negative: 0, lowStock: 0 };
          if (s.margin && Number(s.margin) < 0) entry.negative++;
          if (s.marginPercent != null && Number(s.marginPercent) < 10) entry.lowMargin++;
          const st = (s.stockWb ?? s.stockLocal);
          if (st != null && Number(st) < 10) entry.lowStock++;
          byDate[key] = entry;
        }
        const series = Object.keys(byDate).sort().map(date => ({ date, ...byDate[date] }));
        setSnap(series);
      } catch (_) {
        setSnap([]);
      }
    };
    load();
    return () => { cancelled = true; };
  }, [ctx?.authToken]);

  const kpi = useMemo(() => {
    const last = snap[snap.length - 1];
    return {
      negative: last?.negative ?? 0,
      lowMargin: last?.lowMargin ?? 0,
      lowStock: last?.lowStock ?? 0
    };
  }, [snap]);

  // Also subscribe to global alerts from context to keep dashboard in sync after login or demo actions
  useEffect(() => {
    if (Array.isArray((ctx as any)?.alerts) && (ctx as any).alerts.length && !alerts.length) {
      setAlerts((ctx as any).alerts);
    }
  }, [(ctx as any)?.alerts]);

  const handleRefresh = async () => {
    const currentToken = (ctx as any)?.authToken ?? (typeof localStorage !== 'undefined' ? localStorage.getItem('mh_auth_token') : null);
    if (!currentToken) {
      // Ничего не делаем, если пользователь не авторизован
      setAlerts([]);
      return;
    }
    setLoading(true);
    try {
      if (typeof (ctx as any)?.fetchAlerts === 'function') {
        await (ctx as any).fetchAlerts();
        const list = (ctx as any).alerts as Alert[] | undefined;
        if (Array.isArray(list)) setAlerts(list);
      } else {
        const base = (axios as any)?.defaults?.baseURL || API_BASE_URL || '';
        const url = base ? `${base.replace(/\/$/, '')}/api/alerts` : '/api/alerts';
        const { data } = await axios.get<Alert[]>(url, { headers: { Authorization: `Bearer ${currentToken}` } });
        setAlerts(data ?? []);
      }
    } catch (_) {
      setAlerts([]);
    } finally {
      setLoading(false);
    }
  };

  const groups = {
    NEGATIVE_MARGIN: alerts.filter(a => a.type === 'NEGATIVE_MARGIN'),
    LOW_MARGIN: alerts.filter(a => a.type === 'LOW_MARGIN'),
    LOW_STOCK: alerts.filter(a => a.type === 'LOW_STOCK'),
  } as const;

  return (
    <section className="panel">
      <h2>Dashboard</h2>
      <div style={{ margin: '8px 0 16px 0' }}>
        <button className="btn btn--secondary" onClick={handleRefresh} disabled={loading}>
          {loading ? 'Обновление…' : 'Обновить'}
        </button>
      </div>
      <div className="kpi-cards" style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
        <KpiTile title="Отрицательная маржа" value={loading ? '…' : String(groups.NEGATIVE_MARGIN.length)} trend={snap.map(s => s.negative)} color="#dc2626" />
        <KpiTile title="Низкая маржа" value={loading ? '…' : String(groups.LOW_MARGIN.length)} trend={snap.map(s => s.lowMargin)} color="#f59e0b" />
        <KpiTile title="Низкие остатки" value={loading ? '…' : String(groups.LOW_STOCK.length)} trend={snap.map(s => s.lowStock)} />
      </div>

      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: 20 }}>
        <h3 style={{ margin: 0 }}>Alerts</h3>
        <button className="btn btn--secondary" onClick={handleRefresh} disabled={loading}>
          {loading ? 'Обновление…' : 'Обновить'}
        </button>
      </div>
      {loading ? (
        <div className="message">Загрузка…</div>
      ) : alerts.length === 0 ? (
        <div className="message">Проблем не обнаружено</div>
      ) : (
        <div className="table-wrapper">
          <table>
            <thead>
              <tr>
                <th>Тип</th>
                <th>Артикул</th>
                <th>Товар</th>
                <th className="numeric">Маржа</th>
                <th className="numeric">Маржа %</th>
                <th className="numeric">Остаток (лок./WB)</th>
              </tr>
            </thead>
            <tbody>
              {alerts.map((a, i) => (
                <tr key={i}>
                  <td>{a.type}</td>
                  <td>{a.wbArticle ?? '—'}</td>
                  <td>{a.name ?? '—'}</td>
                  <td className="numeric">{a.margin != null ? a.margin.toFixed(2) : '—'}</td>
                  <td className="numeric">{a.marginPercent != null ? `${a.marginPercent.toFixed(2)}%` : '—'}</td>
                  <td className="numeric">{a.localStock ?? '—'} / {a.wbStock ?? '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}


function KpiTile({ title, value, trend, color }: { title: string; value: string; trend: number[]; color?: string }) {
  const stroke = color ?? '#6366f1';
  const data = trend.map((y, i) => ({ i, y }));
  return (
    <div className="card" style={{ background: 'var(--bg-surface)', borderRadius: 12, padding: 12, minWidth: 240, flex: '0 0 auto' }}>
      <div className="card__title" style={{ color: 'var(--text-secondary)', fontSize: 12 }}>{title}</div>
      <div className="card__value" style={{ fontSize: 28, fontWeight: 700 }}>{value}</div>
      <div style={{ width: '100%', height: 36 }}>
        <ResponsiveContainer>
          <AreaChart data={data} margin={{ top: 6, bottom: 0, left: 0, right: 0 }}>
            <defs>
              <linearGradient id="grad" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor={stroke} stopOpacity={0.35} />
                <stop offset="100%" stopColor={stroke} stopOpacity={0} />
              </linearGradient>
            </defs>
            <Tooltip cursor={false} formatter={(v: any) => [String(v), '']} labelFormatter={() => ''} contentStyle={{ background: 'var(--bg-surface)', border: '1px solid var(--border)', borderRadius: 8 }} />
            <Area type="monotone" dataKey="y" stroke={stroke} fillOpacity={1} fill="url(#grad)" strokeWidth={2} />
          </AreaChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}


