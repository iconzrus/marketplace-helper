import React from 'react';
import { useOutletContext } from 'react-router-dom';
import type { AppOutletContext } from '../App';

export default function Corrections() {
  const ctx = useOutletContext<AppOutletContext>();
  return (
    <section className="panel">
      <div className="panel__title">
        <h2>Корректировки: причины и рекомендации</h2>
        <button className="btn btn--secondary" onClick={() => ctx.fetchValidation()} disabled={ctx.loadingValidation}>{ctx.loadingValidation ? 'Обновление…' : 'Обновить список'}</button>
      </div>
      {!ctx.validationItems ? (
        <div className="message message--info">Нет данных о корректировках. Выполните расчёт или загрузите Excel.</div>
      ) : (ctx.validationItems.filter(v => v.issues && v.issues.length > 0).length === 0) ? (
        <div className="message message--success">Все товары проходят без корректировок.</div>
      ) : (
        <div className="table-wrapper">
          <table>
            <thead>
              <tr>
                <th>Товар</th>
                <th>Артикул</th>
                <th>Проблемы</th>
                <th>Рекомендации</th>
              </tr>
            </thead>
            <tbody>
              {ctx.validationItems.filter(item => item.issues && item.issues.length > 0).map(item => (
                <tr key={`val-${item.productId ?? item.wbArticle ?? item.name}`}>
                  <td>{item.name ?? '—'}</td>
                  <td>{item.wbArticle ?? '—'}</td>
                  <td>
                    <ul className="warnings">
                      {item.issues.map((iss, idx) => (
                        <li key={idx}>
                          {iss.blocking && <span className="badge badge--attention" style={{ marginRight: 6 }}>Блокер</span>}
                          <strong>{iss.field}</strong>: {iss.reason}
                        </li>
                      ))}
                    </ul>
                  </td>
                  <td>
                    {item.issues.some(i => i.suggestion) ? (
                      <ul className="hints">{item.issues.filter(i => i.suggestion).map((i, idx) => (<li key={idx}>{i.suggestion}</li>))}</ul>
                    ) : '—'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}


