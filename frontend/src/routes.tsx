import React from 'react';
import { createBrowserRouter } from 'react-router-dom';
import App from './App';

const Dashboard = React.lazy(() => import('./views/Dashboard'));
const WbCatalog = React.lazy(() => import('./views/WbCatalog'));
const Analytics = React.lazy(() => import('./views/Analytics'));
const Corrections = React.lazy(() => import('./views/Corrections'));
const ImportView = React.lazy(() => import('./views/ImportView'));
const DemoCenter = React.lazy(() => import('./views/DemoCenter'));

export const router = createBrowserRouter([
  {
    path: '/',
    element: <App />,
    children: [
      { index: true, element: <Dashboard /> },
      { path: 'wb', element: <WbCatalog /> },
      { path: 'analytics', element: <Analytics /> },
      { path: 'corrections', element: <Corrections /> },
      { path: 'import', element: <ImportView /> },
      { path: 'demo', element: <DemoCenter /> }
    ]
  }
]);


