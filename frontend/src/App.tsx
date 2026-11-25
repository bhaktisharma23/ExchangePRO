import { Navigate, Route, Routes } from 'react-router-dom';
import { AppShell } from '@/layouts/AppShell';
import { GuestOnlyRoute, ProtectedRoute } from '@/components/auth/ProtectedRoute';
import { useSessionBootstrap } from '@/hooks/useSessionBootstrap';
import { SystemStatusPage } from '@/pages/SystemStatusPage';
import { LoginPage } from '@/pages/LoginPage';
import { RegisterPage } from '@/pages/RegisterPage';
import { AccountPage } from '@/pages/AccountPage';
import { NotFoundPage } from '@/pages/NotFoundPage';

export function App() {
  useSessionBootstrap();

  return (
    <Routes>
      <Route element={<AppShell />}>
        <Route index element={<Navigate to="/account" replace />} />
        <Route path="login" element={<GuestOnlyRoute><LoginPage /></GuestOnlyRoute>} />
        <Route path="register" element={<GuestOnlyRoute><RegisterPage /></GuestOnlyRoute>} />
        <Route path="account" element={<ProtectedRoute><AccountPage /></ProtectedRoute>} />
        <Route path="system" element={<SystemStatusPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}
