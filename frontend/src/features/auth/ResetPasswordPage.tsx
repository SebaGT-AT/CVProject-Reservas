import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useSearchParams } from 'react-router-dom'
import { api } from '../../lib/api'
import { AuthShell } from './LoginPage'

type FormValues = { password: string }

export function ResetPasswordPage() {
  const [params] = useSearchParams()
  const token = params.get('token') ?? ''
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormValues>()
  async function submit(values: FormValues) {
    setError('')
    try {
      const result = await api<{ message: string }>('/api/v1/auth/password/reset', {
        method: 'POST', body: JSON.stringify({ token, newPassword: values.password }),
      })
      setMessage(result.message)
    } catch (caught) { setError(caught instanceof Error ? caught.message : 'No fue posible cambiar la contraseña') }
  }
  return <AuthShell title="Nueva contraseña" subtitle="El enlace vence pronto y solo puede utilizarse una vez.">
    {message ? <><div className="alert alert-success">{message}</div><Link className="btn btn-primary w-100" to="/login">Ingresar</Link></> : <form onSubmit={handleSubmit(submit)}>
      {error && <div className="alert alert-danger">{error}</div>}
      <label className="form-label" htmlFor="password">Nueva contraseña</label>
      <input id="password" type="password" className="form-control" autoComplete="new-password"
        {...register('password', { required: 'Ingresa una contraseña', minLength: { value: 10, message: 'Usa al menos 10 caracteres' } })} />
      {errors.password && <div className="invalid-feedback d-block mb-3">{errors.password.message}</div>}
      <button className="btn btn-primary w-100 py-2 mt-4" disabled={!token || isSubmitting}>{isSubmitting ? 'Guardando…' : 'Guardar contraseña'}</button>
    </form>}
  </AuthShell>
}

