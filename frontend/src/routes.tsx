import React from 'react';
import { createHashRouter } from 'react-router-dom';
import App from './App';

const Dashboard = React.lazy(() => import('./views/Dashboard'));
const WbCatalog = React.lazy(() => import('./views/WbCatalog'));
const Analytics = React.lazy(() => import('./views/Analytics'));
const Corrections = React.lazy(() => import('./views/Corrections'));
const PriceEditor = React.lazy(() => import('./views/PriceEditor'));
const ImportView = React.lazy(() => import('./views/ImportView'));
const DemoCenter = React.lazy(() => import('./views/DemoCenter'));
const WbStatus = React.lazy(() => import('./views/WbStatus'));
const Accounts = React.lazy(() => import('./views/Accounts'));

export const router = createHashRouter([
  {
    path: '/',
    element: <App />,
    children: [
      { index: true, element: <Accounts /> },
      { path: 'wb', element: <WbCatalog /> },
      { path: 'analytics', element: <Analytics /> },
      { path: 'prices', element: <PriceEditor /> },
      { path: 'corrections', element: <Corrections /> },
      { path: 'import', element: <ImportView /> },
      { path: 'demo', element: <DemoCenter /> },
      { path: 'wb-status', element: <WbStatus /> }
    ]
  }
]);


