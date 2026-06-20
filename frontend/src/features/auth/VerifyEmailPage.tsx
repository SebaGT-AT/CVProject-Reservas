import { useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { api } from '../../lib/api'
import { AuthShell } from './LoginPage'

type MessageResponse = { message: string }

export function VerifyEmailPage() {
  const [params] = useSearchParams()
  const token = params.get('token') ?? ''
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function verify() {
    setLoading(true); setError('')
    try {
      const result = await api<MessageResponse>(`/api/v1/auth/verify-email?token=${encodeURIComponent(token)}`)
      setMessage(result.message)
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'No fue posible verificar el correo')
    } finally { setLoading(false) }
  }

  return <AuthShell title="Verifica tu correo" subtitle="Confirma que esta dirección te pertenece para proteger tu cuenta.">
    {message ? <><div className="alert alert-success">{message}</div><Link className="btn btn-primary w-100" to="/login">Ingresar</Link></> : <>
      {error && <div className="alert alert-danger">{error}</div>}
      <button className="btn btn-primary w-100 py-2" onClick={verify} disabled={!token || loading}>{loading ? 'Verificando…' : 'Verificar mi correo'}</button>
    </>}
  </AuthShell>
}

