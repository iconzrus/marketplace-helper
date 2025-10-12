import React, { useEffect, useState } from 'react';
import axios from 'axios';

export default function Accounts() {
  const [hasToken, setHasToken] = useState<boolean>(false);
  const [tokenInput, setTokenInput] = useState<string>('');
  const [seller, setSeller] = useState<any | null>(null);
  const [mockEnabled, setMockEnabled] = useState<boolean>(false);
  const [loading, setLoading] = useState<boolean>(false);

  useEffect(() => {
    const init = async () => {
      try {
        const { data } = await axios.get('/api/v2/wb-api/token');
        setHasToken(Boolean((data as any)?.hasToken));
        const mock = await axios.get('/api/v2/wb-api/mock-mode');
        setMockEnabled(Boolean((mock.data as any)?.mock));
        if ((data as any)?.hasToken) {
          const s = await axios.get('/api/v2/wb-api/seller-info');
          setSeller(s.data);
        }
      } catch (_) {}
    };
    init();
  }, []);

  const saveToken = async () => {
    setLoading(true);
    try {
      await axios.post(`/api/v2/wb-api/token?token=${encodeURIComponent(tokenInput)}`);
      setHasToken(true);
      const s = await axios.get('/api/v2/wb-api/seller-info');
      setSeller(s.data);
      setTokenInput('');
    } catch (_) {}
    setLoading(false);
  };

  const toggleMock = async (enabled: boolean) => {
    setLoading(true);
    try {
      await axios.post(`/api/v2/wb-api/mock-mode?enabled=${String(enabled)}`);
      setMockEnabled(enabled);
      if (!enabled && hasToken) {
        const s = await axios.get('/api/v2/wb-api/seller-info');
        setSeller(s.data);
      }
    } catch (_) {}
    setLoading(false);
  };

  return (
    <section className="panel">
      <div className="panel__title">
        <h2>Мои кабинеты</h2>
      </div>
      <div className="cards" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: 16 }}>
        <div className="card">
          <h3>Реальный кабинет</h3>
          {!hasToken ? (
            <div>
              <input placeholder="WB API Token" value={tokenInput} onChange={e => setTokenInput(e.target.value)} style={{ width: '100%' }} />
              <button className="btn" onClick={saveToken} disabled={loading || !tokenInput}>Сохранить токен</button>
            </div>
          ) : (
            <div>
              <div className="badge badge--success">Токен введён</div>
              <div className="meta" style={{ marginTop: 8 }}>
                <div><strong>Продавец</strong>: {seller?.company ?? '—'}</div>
                {seller?.inn && <div><strong>ИНН</strong>: {seller.inn}</div>}
              </div>
            </div>
          )}
        </div>
        <div className="card">
          <h3>Mock кабинет</h3>
          <label className="toggle">
            <input type="checkbox" checked={mockEnabled} onChange={e => toggleMock(e.target.checked)} /> Включить Mock
          </label>
          <p style={{ marginTop: 8 }}>Для разработки без реального API. В Демо‑центре можно сгенерировать 100–300 товаров.</p>
        </div>
      </div>
    </section>
  );
}


