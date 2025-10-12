import React, { useEffect, useState } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';

export default function Accounts() {
  const navigate = useNavigate();
  const [hasToken, setHasToken] = useState<boolean>(false);
  const [tokenInput, setTokenInput] = useState<string>('');
  const [seller, setSeller] = useState<any | null>(null);
  const [mockEnabled, setMockEnabled] = useState<boolean>(false);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const init = async () => {
      try {
        const { data } = await axios.get('/api/v2/wb-api/token');
        setHasToken(Boolean((data as any)?.hasToken));
        const mock = await axios.get('/api/v2/wb-api/mock-mode');
        setMockEnabled(Boolean((mock.data as any)?.mock));
        if ((data as any)?.hasToken) {
          try {
            const s = await axios.get('/api/v2/wb-api/seller-info');
            setSeller(s.data);
          } catch (e) {
            console.error('Failed to fetch seller info', e);
          }
        }
      } catch (e) {
        console.error('Failed to init accounts', e);
      }
    };
    init();
  }, []);

  const saveToken = async () => {
    setLoading(true);
    setError(null);
    try {
      await axios.post(`/api/v2/wb-api/token?token=${encodeURIComponent(tokenInput)}`);
      await axios.post('/api/v2/wb-api/mock-mode?enabled=false');
      setHasToken(true);
      setMockEnabled(false);
      const s = await axios.get('/api/v2/wb-api/seller-info');
      setSeller(s.data);
      setTokenInput('');
    } catch (err: any) {
      setError('Не удалось сохранить токен или получить информацию о продавце');
      console.error(err);
    }
    setLoading(false);
  };

  const enterRealCabinet = () => {
    navigate('/dashboard');
  };

  const enterMockCabinet = async () => {
    setLoading(true);
    setError(null);
    try {
      await axios.post('/api/v2/wb-api/mock-mode?enabled=true');
      setMockEnabled(true);
      navigate('/demo');
    } catch (err) {
      setError('Не удалось включить Mock режим');
      console.error(err);
    }
    setLoading(false);
  };

  return (
    <section className="panel">
      <div className="panel__title">
        <h2>Мои кабинеты</h2>
        <p>Выберите кабинет для работы</p>
      </div>
      {error && <div className="message message--error">{error}</div>}
      <div className="cards" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: 16, marginTop: 16 }}>
        <div className="card" style={{ padding: 24, border: '1px solid var(--border-color)', borderRadius: 8 }}>
          <h3 style={{ marginTop: 0 }}>Реальный кабинет</h3>
          {!hasToken ? (
            <div>
              <input 
                placeholder="Введите WB API Token" 
                value={tokenInput} 
                onChange={e => setTokenInput(e.target.value)} 
                style={{ width: '100%', marginBottom: 12 }} 
              />
              <button className="btn" onClick={saveToken} disabled={loading || !tokenInput}>
                {loading ? 'Сохранение...' : 'Сохранить токен'}
              </button>
            </div>
          ) : (
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 12 }}>
                <span style={{ color: 'green', fontSize: 24 }}>✓</span>
                <span style={{ fontWeight: 600 }}>Токен введён</span>
              </div>
              <div style={{ marginBottom: 16, padding: 12, background: 'var(--bg-secondary)', borderRadius: 4 }}>
                <div><strong>Продавец:</strong> {seller?.company || seller?.supplierName || seller?.name || '—'}</div>
                {(seller?.inn || seller?.INN) && <div><strong>ИНН:</strong> {seller.inn || seller.INN}</div>}
              </div>
              <button className="btn btn--primary" onClick={enterRealCabinet} disabled={loading}>
                Войти в кабинет
              </button>
            </div>
          )}
        </div>
        <div className="card" style={{ padding: 24, border: '1px solid var(--border-color)', borderRadius: 8 }}>
          <h3 style={{ marginTop: 0 }}>Mock кабинет</h3>
          <p style={{ marginBottom: 16, color: 'var(--text-secondary)' }}>
            Для разработки и тестирования без реального API. 
            Можно сгенерировать 100–300 товаров с случайными данными.
          </p>
          <button className="btn" onClick={enterMockCabinet} disabled={loading}>
            {loading ? 'Загрузка...' : 'Войти в Mock кабинет'}
          </button>
        </div>
      </div>
    </section>
  );
}


