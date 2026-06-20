import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { api } from '../../lib/api'
import { AuthShell } from './LoginPage'
import { useAuth } from './auth-context'
import type { AuthResponse, Role } from './types'

type FormValues = { name: string; email: string; password: string; role: Exclude<Role, 'ADMIN'> }

export function RegisterPage() {
  const { authenticate, user } = useAuth()
  const navigate = useNavigate()
  const [error, setError] = useState('')
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormValues>({ defaultValues: { role: 'PROFESSIONAL' } })

  if (user) return <Navigate to="/dashboard" replace />

  async function submit(values: FormValues) {
    setError('')
    try {
      const session = await api<AuthResponse>('/api/v1/auth/register', { method: 'POST', body: JSON.stringify(values) })
      authenticate(session)
      navigate('/dashboard')
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'No fue posible crear la cuenta')
    }
  }

  return (
    <AuthShell title="Crea tu espacio" subtitle="Parte con lo esencial; afinaremos tu agenda después.">
      <form onSubmit={handleSubmit(submit)} noValidate>
        {error && <div className="alert alert-danger" role="alert">{error}</div>}
        <div className="mb-3"><label className="form-label" htmlFor="name">Nombre</label>
          <input id="name" autoComplete="name" className="form-control" {...register('name', { required: 'Ingresa tu nombre' })} />
          {errors.name && <div className="invalid-feedback d-block">{errors.name.message}</div>}</div>
        <div className="mb-3"><label className="form-label" htmlFor="email">Correo</label>
          <input id="email" type="email" autoComplete="email" className="form-control" {...register('email', { required: 'Ingresa tu correo' })} />
          {errors.email && <div className="invalid-feedback d-block">{errors.email.message}</div>}</div>
        <div className="mb-3"><label className="form-label" htmlFor="password">Contraseña</label>
          <input id="password" type="password" autoComplete="new-password" className="form-control"
            {...register('password', { required: 'Crea una contraseña', minLength: { value: 10, message: 'Usa al menos 10 caracteres' } })} />
          {errors.password && <div className="invalid-feedback d-block">{errors.password.message}</div>}</div>
        <fieldset className="mb-4"><legend className="form-label">Quiero usar Reservas como</legend>
          <div className="d-flex gap-4"><label><input type="radio" value="PROFESSIONAL" {...register('role')} /> Profesional</label>
          <label><input type="radio" value="CUSTOMER" {...register('role')} /> Cliente</label></div></fieldset>
        <button className="btn btn-primary w-100 py-2" disabled={isSubmitting}>{isSubmitting ? 'Creando…' : 'Crear cuenta'}</button>
        <p className="text-center text-secondary mt-4 mb-0">¿Ya tienes cuenta? <Link to="/login">Ingresar</Link></p>
      </form>
    </AuthShell>
  )
}
