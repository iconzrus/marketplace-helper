import React, { useEffect, useState } from 'react';
import axios from 'axios';

type Endpoint = { name: string; path: string; status: 'UP' | 'DOWN'; httpStatus?: number; message?: string };
type Report = { checkedAt: string; endpoints: Endpoint[] };

export default function WbStatus() {
  const [loading, setLoading] = useState(false);
  const [report, setReport] = useState<Report | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = async () => {
    setLoading(true);
    setError(null);
    try {
      const { data } = await axios.get<Report>('/api/wb-status');
      setReport(data);
    } catch (e) {
      setError('Не удалось получить статусы сервисов WB.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  return (
    <section className="panel">
      <div className="panel__title">
        <h2>Стабильность сервисов WB</h2>
        <div className="toolbar">
          <button className="btn btn--secondary" onClick={load} disabled={loading}>{loading ? 'Обновление…' : 'Обновить'}</button>
        </div>
      </div>

      {error && <div className="message message--error">{error}</div>}

      <div className="table-wrapper">
        <table>
          <thead>
            <tr>
              <th>Статус</th>
              <th>Сервис</th>
              <th>URL</th>
              <th>HTTP</th>
              <th>Сообщение</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={5}>Загрузка…</td></tr>
            ) : report?.endpoints?.length ? (
              report.endpoints.map((e, i) => (
                <tr key={i}>
                  <td>
                    <span title={e.status} style={{display:'inline-block',width:10,height:10,borderRadius:'50%',backgroundColor:e.status==='UP'?'#22c55e':'#ef4444', boxShadow:'0 0 0 2px rgba(0,0,0,0.05)'}} />
                  </td>
                  <td>{e.name}</td>
                  <td><a href={e.path} target="_blank" rel="noreferrer">{e.path}</a></td>
                  <td>{e.httpStatus ?? '—'}</td>
                  <td>{e.message ?? '—'}</td>
                </tr>
              ))
            ) : (
              <tr><td colSpan={5}>Нет данных.</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </section>
  );
}


