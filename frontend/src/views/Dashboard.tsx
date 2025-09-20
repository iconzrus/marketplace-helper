import React, { useEffect, useState } from 'react';
import axios from 'axios';
import { API_BASE_URL } from '../config';
import { useOutletContext } from 'react-router-dom';
import type { AppOutletContext } from '../App';

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

  const token = ctx?.authToken ?? (typeof localStorage !== 'undefined' ? localStorage.getItem('mh_auth_token') : null);

  useEffect(() => {
    let cancelled = false;
    const sleep = (ms: number) => new Promise(res => setTimeout(res, ms));
    const loadWithRetry = async () => {
      setLoading(true);
      const base = axios.defaults.baseURL || API_BASE_URL || '';
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
    loadWithRetry();
    return () => { cancelled = true; };
  }, [token]);

  // Also subscribe to global alerts from context to keep dashboard in sync after login or demo actions
  useEffect(() => {
    if (Array.isArray((ctx as any)?.alerts) && (ctx as any).alerts.length && !alerts.length) {
      setAlerts((ctx as any).alerts);
    }
  }, [(ctx as any)?.alerts]);

  const handleRefresh = async () => {
    setLoading(true);
    try {
      const currentToken = (ctx as any)?.authToken ?? (typeof localStorage !== 'undefined' ? localStorage.getItem('mh_auth_token') : null);
      if (!currentToken) {
        setAlerts([]);
        return;
      }
      if (typeof (ctx as any)?.fetchAlerts === 'function') {
        await (ctx as any).fetchAlerts();
        const list = (ctx as any).alerts as Alert[] | undefined;
        if (Array.isArray(list)) setAlerts(list);
      } else {
        const base = axios.defaults.baseURL || API_BASE_URL || '';
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
      <div className="kpi-cards">
        <div className="card card--danger">
          <div className="card__title">Отрицательная маржа</div>
          <div className="card__value">{loading ? '…' : groups.NEGATIVE_MARGIN.length}</div>
        </div>
        <div className="card card--warning">
          <div className="card__title">Низкая маржа</div>
          <div className="card__value">{loading ? '…' : groups.LOW_MARGIN.length}</div>
        </div>
        <div className="card">
          <div className="card__title">Низкие остатки</div>
          <div className="card__value">{loading ? '…' : groups.LOW_STOCK.length}</div>
        </div>
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


