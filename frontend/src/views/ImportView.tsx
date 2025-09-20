import React from 'react';
import { useOutletContext } from 'react-router-dom';
import type { AppOutletContext } from '../App';

export default function ImportView() {
  const ctx = useOutletContext<AppOutletContext>();
  return (
    <section className="panel">
      <h2>Импорт Excel</h2>
      <p>
        Шаблон колонок: <code>Название</code>, <code>Артикул WB</code>, <code>Штрихкод</code>, <code>Цена</code>,
        <code> Закупка</code>, <code>Логистика</code>, <code>Маркетинг</code>, <code>Прочие</code>, <code>Остаток</code>.
        Колонки можно располагать в любом порядке и именовать на русском или английском.
      </p>
      <label className="upload">
        <input type="file" accept=".xlsx,.xls" onChange={ctx.handleFileUpload} />
        <span>Загрузить Excel</span>
      </label>
    </section>
  );
}


