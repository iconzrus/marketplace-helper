import React, { useRef, useState } from 'react';
import { useOutletContext } from 'react-router-dom';
import type { AppOutletContext } from '../App';

export default function ImportView() {
  const ctx = useOutletContext<AppOutletContext>();
  const [dryRun, setDryRun] = useState(true);
  const [preview, setPreview] = useState<{ created: number; updated: number; skipped: number; warnings?: string[]; errors?: string[] } | null>(null);
  const fileRef = useRef<HTMLInputElement | null>(null);

  const runPreview = async () => {
    const file = fileRef.current?.files?.[0];
    if (!file) return;
    const formData = new FormData();
    formData.append('file', file);
    try {
      const res = await fetch((axios as any)?.defaults?.baseURL ? `${(axios as any).defaults.baseURL.replace(/\/$/, '')}/api/products/import/excel?dryRun=true` : '/api/products/import/excel?dryRun=true', {
        method: 'POST',
        body: formData,
        headers: (window as any).axios?.defaults?.headers?.common?.Authorization ? { Authorization: (window as any).axios.defaults.headers.common.Authorization } : undefined
      } as RequestInit);
      const data = await res.json();
      setPreview(data);
    } catch (_) {
      setPreview({ created: 0, updated: 0, skipped: 0, errors: ['Не удалось выполнить предпросмотр'] });
    }
  };
  return (
    <section className="panel">
      <h2>Импорт Excel</h2>
      <p>
        Шаблон колонок: <code>Название</code>, <code>Артикул WB</code>, <code>Штрихкод</code>, <code>Цена</code>,
        <code> Закупка</code>, <code>Логистика</code>, <code>Маркетинг</code>, <code>Прочие</code>, <code>Остаток</code>.
        Колонки можно располагать в любом порядке и именовать на русском или английском.
      </p>
      <div style={{ display: 'flex', gap: 12, alignItems: 'center', flexWrap: 'wrap' }}>
        <label className="upload">
          <input ref={fileRef} type="file" accept=".xlsx,.xls" onChange={dryRun ? () => runPreview() : ctx.handleFileUpload} />
          <span>{dryRun ? 'Предпросмотр (dry-run)' : 'Загрузить Excel'}</span>
        </label>
        <label className="toggle">
          <input type="checkbox" checked={dryRun} onChange={e => setDryRun(e.target.checked)} /> Предпросмотр перед загрузкой
        </label>
        {!dryRun && (
          <button className="btn" onClick={() => fileRef.current?.dispatchEvent(new Event('change', { bubbles: true }))}>Импортировать</button>
        )}
      </div>
      {preview && (
        <div className="message message--info">
          Найдено: создано {preview.created}, обновлено {preview.updated}, пропущено {preview.skipped}.
          {preview.warnings?.length ? (
            <div style={{ marginTop: 8 }}>
              Предупреждения:
              <ul className="warnings">{preview.warnings.map((w, i) => <li key={i}>{w}</li>)}</ul>
            </div>
          ) : null}
          {preview.errors?.length ? (
            <div style={{ marginTop: 8 }}>
              Ошибки:
              <ul className="warnings">{preview.errors.map((w, i) => <li key={i}>{w}</li>)}</ul>
            </div>
          ) : null}
        </div>
      )}
    </section>
  );
}


