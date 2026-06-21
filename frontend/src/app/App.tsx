import { Navigate, Route, Routes } from 'react-router-dom'
import { LoginPage } from '../features/auth/LoginPage'
import { RegisterPage } from '../features/auth/RegisterPage'
import { useAuth } from '../features/auth/auth-context'
import { DashboardPage } from '../features/dashboard/DashboardPage'
import { VerifyEmailPage } from '../features/auth/VerifyEmailPage'
import { ForgotPasswordPage } from '../features/auth/ForgotPasswordPage'
import { ResetPasswordPage } from '../features/auth/ResetPasswordPage'
import { ResendVerificationPage } from '../features/auth/ResendVerificationPage'
import { ProfessionalPage } from '../features/professional/ProfessionalPage'
import { PublicProfessionalPage } from '../features/professional/PublicProfessionalPage'
import type { Role } from '../features/auth/types'
import { SchedulePage } from '../features/scheduling/SchedulePage'
import { AppointmentsPage } from '../features/appointments/AppointmentsPage'

function ProtectedRoute({ children, role, roles }: { children: React.ReactNode; role?: Role; roles?: Role[] }) {
  const { user, loading } = useAuth()
  if (loading) return <main className="min-vh-100 d-flex align-items-center justify-content-center"><div className="spinner-border text-success" role="status"><span className="visually-hidden">Cargando</span></div></main>
  if (!user) return <Navigate to="/login" replace />
  if (role && user.role !== role) return <Navigate to="/dashboard" replace />
  if (roles && !roles.includes(user.role)) return <Navigate to="/dashboard" replace />
  return children
}
export function App() {
  return <>
    <a className="skip-link" href="#main-content">Saltar al contenido</a>
    <div id="main-content" tabIndex={-1}>
    <Routes>
    <Route path="/login" element={<LoginPage />} />
    <Route path="/registro" element={<RegisterPage />} />
    <Route path="/verificar-correo" element={<VerifyEmailPage />} />
    <Route path="/olvide-contrasena" element={<ForgotPasswordPage />} />
    <Route path="/restablecer-contrasena" element={<ResetPasswordPage />} />
    <Route path="/reenviar-verificacion" element={<ResendVerificationPage />} />
    <Route path="/p/:slug" element={<PublicProfessionalPage />} />
    <Route path="/dashboard" element={<ProtectedRoute><DashboardPage /></ProtectedRoute>} />
    <Route path="/perfil-profesional" element={<ProtectedRoute role="PROFESSIONAL"><ProfessionalPage /></ProtectedRoute>} />
    <Route path="/configurar-agenda" element={<ProtectedRoute role="PROFESSIONAL"><SchedulePage /></ProtectedRoute>} />
    <Route path="/citas" element={<ProtectedRoute roles={['CUSTOMER', 'PROFESSIONAL']}><AppointmentsPage /></ProtectedRoute>} />
    <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
    </div>
  </>
}
