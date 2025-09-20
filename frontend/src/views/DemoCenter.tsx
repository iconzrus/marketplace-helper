import React from 'react';
import { useOutletContext } from 'react-router-dom';
import type { AppOutletContext } from '../App';

export default function DemoCenter() {
  const ctx = useOutletContext<AppOutletContext>();
  return (
    <section className="panel">
      <div className="panel__title">
        <h2>Демо‑центр</h2>
        <div className="demo-toolbar">
          <label className="chip">
            <input type="checkbox" checked={ctx.demoMode} onChange={e => ctx.setDemoMode(e.target.checked)} /> Demo Mode
          </label>
          <div className="presets">
            <button className="btn btn--secondary" onClick={() => ctx.setMinMarginPercent(10)}>Пресет: Минимум</button>
            <button className="btn" onClick={ctx.runDemoAutofill} disabled={!ctx.demoMode}>Автозаполнить расходы</button>
          </div>
          <div className="gen-controls">
            <input inputMode="numeric" placeholder="Сколько сгенерировать" value={ctx.genCount} onChange={e => ctx.setGenCount(e.target.value)} style={{ width: 180 }} disabled={!ctx.demoMode} />
            <select value={ctx.genType} onChange={e => ctx.setGenType(e.target.value as any)} disabled={!ctx.demoMode}>
              <option value="both">WB + Excel (поровну)</option>
              <option value="excel">Только Excel</option>
              <option value="wb">Только WB</option>
            </select>
            <button className="btn" onClick={ctx.runDemoGenerate} disabled={!ctx.demoMode}>Сгенерировать</button>
          </div>
          <div className="gen-controls" style={{ marginTop: 8 }}>
            <input inputMode="numeric" placeholder="Сколько удалить" value={ctx.delCount} onChange={e => ctx.setDelCount(e.target.value)} style={{ width: 180 }} disabled={!ctx.demoMode || ctx.delAll} />
            <label className="chip">
              <input type="checkbox" checked={ctx.delAll} onChange={e => ctx.setDelAll(e.target.checked)} disabled={!ctx.demoMode} /> Удалить всё
            </label>
            <button className="btn btn--secondary" onClick={ctx.runDemoDelete} disabled={!ctx.demoMode}>Удалить</button>
          </div>
        </div>
      </div>
      {ctx.demoMode && <div className="demo-banner">Демо‑режим активен: можно безопасно экспериментировать с данными и порогами.</div>}
    </section>
  );
}


