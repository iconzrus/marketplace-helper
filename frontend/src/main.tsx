import React from 'react';
import ReactDOM from 'react-dom/client';
import { RouterProvider } from 'react-router-dom';
import { router } from './routes';
import './styles.css';
/// <reference types="vitest" />

ReactDOM.createRoot(document.getElementById('root') as HTMLElement).render(
  <React.StrictMode>
    <React.Suspense fallback={<div className="panel" style={{margin: 16}}>Загрузка…</div>}>
      <RouterProvider router={router} />
    </React.Suspense>
  </React.StrictMode>
);
