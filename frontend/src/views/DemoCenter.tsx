import React from 'react';
import { useOutletContext } from 'react-router-dom';
import type { AppOutletContext } from '../App';

export default function DemoCenter() {
  const ctx = useOutletContext<AppOutletContext>();
  return (
    <section className="panel">
      <div className="panel__title">
        <h2>Mock кабинет — Демо‑центр</h2>
        <p>Генерация и управление тестовыми данными</p>
      </div>
      <div className="demo-banner">Mock режим активен. Вы можете безопасно экспериментировать с данными.</div>
      <div className="demo-toolbar" style={{ marginTop: 16 }}>
        <div style={{ marginBottom: 16 }}>
          <button className="btn btn--primary" onClick={ctx.generateMockCabinet}>Сгенерировать кабинет (100–300)</button>
          <span style={{ marginLeft: 12, color: 'var(--text-secondary)' }}>Создаст случайного продавца и 100–300 товаров</span>
        </div>
        <div className="presets" style={{ marginBottom: 16 }}>
          <button className="btn btn--secondary" onClick={() => ctx.setMinMarginPercent(10)}>Пресет: Минимум</button>
          <button className="btn" onClick={ctx.runDemoAutofill}>Автозаполнить расходы</button>
        </div>
        <div className="gen-controls" style={{ marginBottom: 16 }}>
          <input inputMode="numeric" placeholder="Сколько сгенерировать" value={ctx.genCount} onChange={e => ctx.setGenCount(e.target.value)} style={{ width: 180 }} />
          <select value={ctx.genType} onChange={e => ctx.setGenType(e.target.value as any)}>
            <option value="both">WB + Excel (поровну)</option>
            <option value="excel">Только Excel</option>
            <option value="wb">Только WB</option>
          </select>
          <button className="btn" onClick={ctx.runDemoGenerate}>Сгенерировать</button>
        </div>
        <div className="gen-controls">
          <input inputMode="numeric" placeholder="Сколько удалить" value={ctx.delCount} onChange={e => ctx.setDelCount(e.target.value)} style={{ width: 180 }} disabled={ctx.delAll} />
          <label className="chip">
            <input type="checkbox" checked={ctx.delAll} onChange={e => ctx.setDelAll(e.target.checked)} /> Удалить всё
          </label>
          <button className="btn btn--secondary" onClick={ctx.runDemoDelete}>Удалить</button>
        </div>
      </div>
    </section>
  );
}


