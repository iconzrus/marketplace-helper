import React, { useEffect, useState } from 'react';
import axios from 'axios';
import { useNavigate, useLocation } from 'react-router-dom';

export default function Accounts() {
  const navigate = useNavigate();
  const location = useLocation();
  const [hasToken, setHasToken] = useState<boolean>(false);
  const [tokenInput, setTokenInput] = useState<string>('');
  const [seller, setSeller] = useState<any | null>(null);
  const [mockEnabled, setMockEnabled] = useState<boolean>(false);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const loadAccountsData = async () => {
    try {
      const { data } = await axios.get('/api/v2/wb-api/token');
      setHasToken(Boolean((data as any)?.hasToken));
      const mock = await axios.get('/api/v2/wb-api/mock-mode');
      const isMockEnabled = Boolean((mock.data as any)?.mock);
      
      // Auto-disable mock mode when returning to Accounts page
      if (isMockEnabled) {
        try {
          await axios.post('/api/v2/wb-api/mock-mode?enabled=false');
          setMockEnabled(false);
        } catch (e) {
          console.error('Failed to disable mock mode', e);
          setMockEnabled(isMockEnabled);
        }
      } else {
        setMockEnabled(false);
      }
      
      // Only fetch seller info if token exists AND mock is disabled
      if ((data as any)?.hasToken && !isMockEnabled) {
        try {
          const s = await axios.get('/api/v2/wb-api/seller-info');
          // Only set seller if response is not mock data
          if (!s.data?.mock) {
            setSeller(s.data);
          } else {
            setSeller(null);
          }
        } catch (e) {
          console.error('Failed to fetch seller info', e);
          setSeller(null);
        }
      } else {
        setSeller(null);
      }
    } catch (e) {
      console.error('Failed to load accounts data', e);
    }
  };

  useEffect(() => {
    loadAccountsData();
  }, [location.pathname]); // Reload when returning to this page

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
      setSeller(null); // Clear seller info when entering mock mode
      navigate('/demo');
    } catch (err) {
      setError('Не удалось включить Mock режим');
      console.error(err);
    }
    setLoading(false);
  };

  return (
    <div style={{ maxWidth: '1400px', margin: '0 auto' }}>
      <div className="mb-6">
        <h2 className="text-lg font-semibold mb-2">Выберите кабинет для работы</h2>
        <p className="text-secondary text-sm">Подключите реальный WB API токен или используйте тестовый кабинет для разработки</p>
      </div>
      
      {error && <div className="message message--error mb-4">{error}</div>}
      
      <div className="grid grid-cols-2" style={{ gap: 'var(--space-6)' }}>
        {/* Real Cabinet Card */}
        <div className="card">
          <div className="card__header">
            <div className="flex items-center gap-3">
              <div style={{ fontSize: '2rem' }}>🏢</div>
              <div>
                <h3 className="card__title">Реальный кабинет</h3>
                <p className="card__description">Подключение к WB API</p>
              </div>
            </div>
          </div>
          
          <div className="card__content">
            {!hasToken ? (
              <div>
                <label className="text-sm font-medium text-secondary mb-2" style={{ display: 'block' }}>
                  WB API Token
                </label>
                <input 
                  type="password"
                  placeholder="Введите токен из личного кабинета WB" 
                  value={tokenInput} 
                  onChange={e => setTokenInput(e.target.value)}
                  className="mb-4"
                />
                <button className="btn btn--primary" onClick={saveToken} disabled={loading || !tokenInput}>
                  {loading ? 'Сохранение...' : 'Сохранить и подключить'}
                </button>
              </div>
            ) : (
              <div>
                <div className="flex items-center gap-2 mb-4">
                  <span style={{ color: 'var(--success-solid)', fontSize: '1.5rem' }}>✓</span>
                  <span className="font-semibold text-success">Токен подключён</span>
                </div>
                <div className="mb-4 p-3" style={{ background: 'var(--bg-subtle)', borderRadius: 'var(--radius-md)' }}>
                  <div className="text-sm mb-1">
                    <span className="text-tertiary">Продавец:</span>{' '}
                    <span className="font-medium">{seller?.company || seller?.supplierName || seller?.name || '—'}</span>
                  </div>
                  {(seller?.inn || seller?.INN) && (
                    <div className="text-sm">
                      <span className="text-tertiary">ИНН:</span>{' '}
                      <span className="font-medium">{seller.inn || seller.INN}</span>
                    </div>
                  )}
                </div>
                <button className="btn btn--primary" onClick={enterRealCabinet} disabled={loading}>
                  Войти в кабинет →
                </button>
              </div>
            )}
          </div>
        </div>

        {/* Mock Cabinet Card */}
        <div className="card">
          <div className="card__header">
            <div className="flex items-center gap-3">
              <div style={{ fontSize: '2rem' }}>🧪</div>
              <div>
                <h3 className="card__title">Тестовый кабинет</h3>
                <p className="card__description">Для разработки и демо</p>
              </div>
            </div>
          </div>
          
          <div className="card__content">
            <p className="text-sm text-secondary mb-4">
              Работа без реального API. Можно сгенерировать 100–300 товаров с случайными данными для тестирования функционала.
            </p>
            <button className="btn btn--secondary" onClick={enterMockCabinet} disabled={loading}>
              {loading ? 'Загрузка...' : 'Войти в тестовый кабинет →'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}


