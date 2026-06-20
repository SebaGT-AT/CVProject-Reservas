import { Navigate, Route, Routes } from 'react-router-dom'
import { LoginPage } from '../features/auth/LoginPage'
import { RegisterPage } from '../features/auth/RegisterPage'
import { useAuth } from '../features/auth/auth-context'
import { DashboardPage } from '../features/dashboard/DashboardPage'
import { VerifyEmailPage } from '../features/auth/VerifyEmailPage'
import { ForgotPasswordPage } from '../features/auth/ForgotPasswordPage'
import { ResetPasswordPage } from '../features/auth/ResetPasswordPage'
import { ResendVerificationPage } from '../features/auth/ResendVerificationPage'

function ProtectedRoute() {
  const { user, loading } = useAuth()
  if (loading) return <main className="min-vh-100 d-flex align-items-center justify-content-center"><div className="spinner-border text-success" role="status"><span className="visually-hidden">Cargando</span></div></main>
  return user ? <DashboardPage /> : <Navigate to="/login" replace />
}

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/registro" element={<RegisterPage />} />
      <Route path="/verificar-correo" element={<VerifyEmailPage />} />
      <Route path="/olvide-contrasena" element={<ForgotPasswordPage />} />
      <Route path="/restablecer-contrasena" element={<ResetPasswordPage />} />
      <Route path="/reenviar-verificacion" element={<ResendVerificationPage />} />
      <Route path="/dashboard" element={<ProtectedRoute />} />
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  )
}
