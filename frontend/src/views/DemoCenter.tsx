import React from 'react';
import { useOutletContext } from 'react-router-dom';
import type { AppOutletContext } from '../App';

export default function DemoCenter() {
  const ctx = useOutletContext<AppOutletContext>();
  return (
    <div style={{ maxWidth: '1200px', margin: '0 auto' }}>
      {/* Banner */}
      <div className="message message--info mb-6">
        <strong>Mock режим активен.</strong> Вы можете безопасно экспериментировать с данными без подключения к реальному API.
      </div>

      {/* Main Cabinet Generation */}
      <div className="card mb-6">
        <div className="card__header">
          <h3 className="card__title">Генерация тестового кабинета</h3>
          <p className="card__description">Создайте случайного продавца и 100–300 товаров для работы</p>
        </div>
        <div className="card__content">
          <button className="btn btn--primary btn--lg" onClick={ctx.generateMockCabinet}>
            Сгенерировать кабинет (100–300 товаров)
          </button>
        </div>
      </div>

      {/* Quick Actions */}
      <div className="card mb-6">
        <div className="card__header">
          <h3 className="card__title">Быстрые действия</h3>
          <p className="card__description">Пресеты и автоматизация</p>
        </div>
        <div className="card__content">
          <div style={{ display: 'flex', gap: 'var(--space-3)', flexWrap: 'wrap' }}>
            <button className="btn btn--secondary" onClick={() => ctx.setMinMarginPercent(10)}>
              Пресет: Минимум (10%)
            </button>
            <button className="btn btn--secondary" onClick={ctx.runDemoAutofill}>
              Автозаполнить расходы
            </button>
          </div>
        </div>
      </div>

      {/* Manual Generation */}
      <div className="card mb-6">
        <div className="card__header">
          <h3 className="card__title">Добавить товары вручную</h3>
          <p className="card__description">Сгенерируйте дополнительные товары поверх существующих</p>
        </div>
        <div className="card__content">
          <div style={{ display: 'flex', gap: 'var(--space-3)', alignItems: 'flex-end', flexWrap: 'wrap' }}>
            <div style={{ flex: '0 0 200px' }}>
              <label style={{ display: 'block', marginBottom: 'var(--space-2)', fontSize: '0.875rem', fontWeight: 500 }}>
                Количество
              </label>
              <input 
                type="number" 
                inputMode="numeric" 
                placeholder="Количество" 
                value={ctx.genCount} 
                onChange={e => ctx.setGenCount(e.target.value)} 
              />
            </div>
            <div style={{ flex: '0 0 250px' }}>
              <label style={{ display: 'block', marginBottom: 'var(--space-2)', fontSize: '0.875rem', fontWeight: 500 }}>
                Тип товаров
              </label>
              <select value={ctx.genType} onChange={e => ctx.setGenType(e.target.value as any)}>
                <option value="both">WB + Excel (поровну)</option>
                <option value="excel">Только Excel</option>
                <option value="wb">Только WB</option>
              </select>
            </div>
            <button className="btn btn--primary" onClick={ctx.runDemoGenerate}>
              Сгенерировать
            </button>
          </div>
        </div>
      </div>

      {/* Delete Actions */}
      <div className="card">
        <div className="card__header">
          <h3 className="card__title">Удаление товаров</h3>
          <p className="card__description">Очистка тестовых данных</p>
        </div>
        <div className="card__content">
          <div style={{ display: 'flex', gap: 'var(--space-3)', alignItems: 'flex-end', flexWrap: 'wrap' }}>
            <div style={{ flex: '0 0 200px' }}>
              <label style={{ display: 'block', marginBottom: 'var(--space-2)', fontSize: '0.875rem', fontWeight: 500 }}>
                Количество
              </label>
              <input 
                type="number" 
                inputMode="numeric" 
                placeholder="Количество" 
                value={ctx.delCount} 
                onChange={e => ctx.setDelCount(e.target.value)} 
                disabled={ctx.delAll}
              />
            </div>
            <label className="toggle" style={{ marginBottom: 'var(--space-2)' }}>
              <input type="checkbox" checked={ctx.delAll} onChange={e => ctx.setDelAll(e.target.checked)} />
              <span>Удалить всё</span>
            </label>
            <button className="btn btn--danger" onClick={ctx.runDemoDelete}>
              Удалить
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}


