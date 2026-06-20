import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link } from 'react-router-dom'
import { api } from '../../lib/api'
import { AuthShell } from './LoginPage'

type FormValues = { email: string }

export function ResendVerificationPage() {
  const [message, setMessage] = useState('')
  const { register, handleSubmit, formState: { isSubmitting } } = useForm<FormValues>()
  async function submit(values: FormValues) {
    const result = await api<{ message: string }>('/api/v1/auth/verify-email/resend', {
      method: 'POST', body: JSON.stringify(values),
    })
    setMessage(result.message)
  }
  return <AuthShell title="Nuevo enlace" subtitle="Si tu cuenta aún no está verificada, enviaremos un enlace nuevo.">
    {message ? <div className="alert alert-success">{message}</div> : <form onSubmit={handleSubmit(submit)}>
      <label className="form-label" htmlFor="email">Correo</label>
      <input id="email" type="email" className="form-control mb-4" required {...register('email')} />
      <button className="btn btn-primary w-100 py-2" disabled={isSubmitting}>{isSubmitting ? 'Enviando…' : 'Reenviar verificación'}</button>
    </form>}
    <p className="mt-4 mb-0"><Link to="/login">Volver al ingreso</Link></p>
  </AuthShell>
}

