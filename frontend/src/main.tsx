import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AuthProvider, useAuth } from './shared/auth';
import { LoginPage } from './pages/LoginPage';
import { RegisterPage } from './pages/RegisterPage';
import { Dashboard } from './pages/Dashboard';
import { ClientsPage } from './pages/ClientsPage';
import { SchedulePage } from './pages/SchedulePage';
import { CatalogPage } from './pages/CatalogPage';
import { MembershipsPage } from './pages/MembershipsPage';
import { ReportsPage } from './pages/ReportsPage';
import { BookingsPage } from './pages/BookingsPage';
import { VisitsPage } from './pages/VisitsPage';
import { NotificationsPage } from './pages/NotificationsPage';
import { AppShell } from './shared/AppShell';
import './styles.css';

function PrivateRoute() {
  const { user, loading } = useAuth();
  if (loading) return <main className="center">Загружаем профиль...</main>;
  return user ? <AppShell /> : <Navigate to="/login" replace />;
}

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route element={<PrivateRoute />}>
            <Route path="/" element={<Dashboard />} />
            <Route path="/clients" element={<ClientsPage />} />
            <Route path="/memberships" element={<MembershipsPage />} />
            <Route path="/schedule" element={<SchedulePage />} />
            <Route path="/bookings" element={<BookingsPage />} />
            <Route path="/visits" element={<VisitsPage />} />
            <Route path="/catalogs" element={<CatalogPage />} />
            <Route path="/reports" element={<ReportsPage />} />
            <Route path="/notifications" element={<NotificationsPage />} />
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  </React.StrictMode>
);
