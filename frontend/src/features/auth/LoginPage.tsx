import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { api } from '../../lib/api'
import { useAuth } from './auth-context'
import type { AuthResponse } from './types'

type FormValues = { email: string; password: string }

export function LoginPage() {
  const { authenticate, user } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [error, setError] = useState('')
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormValues>()

  if (user) return <Navigate to="/dashboard" replace />

  async function submit(values: FormValues) {
    setError('')
    try {
      const session = await api<AuthResponse>('/api/v1/auth/login', {
        method: 'POST', body: JSON.stringify(values),
      })
      authenticate(session)
      navigate('/dashboard')
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'No fue posible iniciar sesión')
    }
  }

  return (
    <AuthShell title="Bienvenido de vuelta" subtitle="Tu agenda, tus clientes y tu día en un solo lugar.">
      <form onSubmit={handleSubmit(submit)} noValidate>
        {typeof location.state?.notice === 'string' && <div className="alert alert-success" role="status">{location.state.notice}</div>}
        {error && <div className="alert alert-danger" role="alert">{error}</div>}
        <div className="mb-3">
          <label className="form-label" htmlFor="email">Correo</label>
          <input id="email" type="email" autoComplete="email" className="form-control"
            {...register('email', { required: 'Ingresa tu correo' })} />
          {errors.email && <div className="invalid-feedback d-block">{errors.email.message}</div>}
        </div>
        <div className="mb-4">
          <label className="form-label" htmlFor="password">Contraseña</label>
          <input id="password" type="password" autoComplete="current-password" className="form-control"
            {...register('password', { required: 'Ingresa tu contraseña' })} />
          {errors.password && <div className="invalid-feedback d-block">{errors.password.message}</div>}
        </div>
        <button className="btn btn-primary w-100 py-2" disabled={isSubmitting}>
          {isSubmitting ? 'Ingresando…' : 'Ingresar'}
        </button>
        <div className="d-flex justify-content-between mt-4"><Link to="/registro">Crear cuenta</Link><Link to="/olvide-contrasena">Olvidé mi contraseña</Link></div>
        <p className="text-center mt-3 mb-0"><Link to="/reenviar-verificacion">Reenviar verificación</Link></p>
      </form>
    </AuthShell>
  )
}

export function AuthShell({ title, subtitle, children }: { title: string; subtitle: string; children: React.ReactNode }) {
  return (
    <main className="auth-page container-fluid min-vh-100">
      <div className="row min-vh-100 align-items-stretch">
        <section className="col-lg-6 d-none d-lg-flex auth-hero p-5 flex-column justify-content-between">
          <span className="brand">Reservas</span>
          <div><p className="eyebrow">MENOS ADMINISTRACIÓN. MÁS OFICIO.</p><h1>Tu tiempo merece una agenda que trabaje contigo.</h1></div>
          <small>Plataforma para profesionales independientes.</small>
        </section>
        <section className="col-lg-6 d-flex align-items-center justify-content-center p-4">
          <div className="auth-card w-100">
            <span className="brand d-lg-none">Reservas</span>
            <h2 className="mt-4 mt-lg-0">{title}</h2><p className="text-secondary mb-4">{subtitle}</p>{children}
          </div>
        </section>
      </div>
    </main>
  )
}
