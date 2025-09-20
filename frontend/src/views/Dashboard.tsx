import React, { useEffect, useState } from 'react';
import axios from 'axios';

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
  const [alerts, setAlerts] = useState<Alert[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const load = async () => {
      try {
        setLoading(true);
        const { data } = await axios.get<Alert[]>('/api/alerts');
        setAlerts(data ?? []);
      } catch (e) {
        setAlerts([]);
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  const groups = {
    NEGATIVE_MARGIN: alerts.filter(a => a.type === 'NEGATIVE_MARGIN'),
    LOW_MARGIN: alerts.filter(a => a.type === 'LOW_MARGIN'),
    LOW_STOCK: alerts.filter(a => a.type === 'LOW_STOCK'),
  } as const;

  return (
    <section className="panel">
      <h2>Dashboard</h2>
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

      <h3 style={{ marginTop: 20 }}>Alerts</h3>
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


