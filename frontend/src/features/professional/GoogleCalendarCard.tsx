import { useEffect, useState } from 'react'
import type { AuthContextValue } from '../auth/auth-context'

type Status = {
  configured: boolean
  connected: boolean
  reauthorizationRequired: boolean
  connectedAt: string | null
}

type AuthorizationUrl = { authorizationUrl: string }

export function GoogleCalendarCard({ request }: { request: AuthContextValue['request'] }) {
  const [status, setStatus] = useState<Status | null>(null)
  const [notice, setNotice] = useState(() => oauthResult() === 'connected' ? 'Google Calendar quedó conectado.' : '')
  const [error, setError] = useState(() => oauthResult() === 'error' ? 'No fue posible conectar Google Calendar. Inténtalo nuevamente.' : '')
  const [working, setWorking] = useState(false)

  useEffect(() => {
    let active = true
    if (oauthResult()) {
      window.history.replaceState({}, '', window.location.pathname)
    }
    request<Status>('/api/v1/integrations/google-calendar/status')
      .then((next) => { if (active) setStatus(next) })
      .catch((caught) => { if (active) setError(caught instanceof Error ? caught.message : 'No fue posible consultar Google Calendar') })
    return () => { active = false }
  }, [request])

  async function connect() {
    setWorking(true); setError(''); setNotice('')
    try {
      const response = await request<AuthorizationUrl>('/api/v1/integrations/google-calendar/authorization-url')
      window.location.assign(response.authorizationUrl)
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'No fue posible iniciar la conexión')
      setWorking(false)
    }
  }

  async function disconnect() {
    if (!window.confirm('¿Desconectar Google Calendar? Los eventos ya creados permanecerán en el calendario.')) return
    setWorking(true); setError(''); setNotice('')
    try {
      await request<void>('/api/v1/integrations/google-calendar', { method: 'DELETE' })
      setStatus((current) => current ? { ...current, connected: false, reauthorizationRequired: false, connectedAt: null } : current)
      setNotice('Google Calendar fue desconectado.')
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'No fue posible desconectar Google Calendar')
    } finally { setWorking(false) }
  }

  return <section className="workspace-card integration-card mb-4" aria-labelledby="google-calendar-title">
    <div className="d-flex flex-column flex-md-row justify-content-between gap-3">
      <div>
        <p className="eyebrow mb-2">INTEGRACIÓN</p>
        <h2 id="google-calendar-title" className="mb-2">Google Calendar</h2>
        <p className="text-secondary mb-0">Crea y cancela automáticamente los eventos de tus próximas reservas.</p>
      </div>
      <div className="integration-status" aria-live="polite">
        {!status && <span className="text-secondary">Consultando…</span>}
        {status?.connected && <span className="badge text-bg-success">Conectado</span>}
        {status?.reauthorizationRequired && <span className="badge text-bg-warning">Requiere autorización</span>}
        {status && !status.connected && !status.reauthorizationRequired && <span className="badge text-bg-secondary">Desconectado</span>}
      </div>
    </div>
    {notice && <div className="alert alert-success mt-3 mb-0" role="status">{notice}</div>}
    {error && <div className="alert alert-danger mt-3 mb-0" role="alert">{error}</div>}
    {status && <div className="d-flex flex-wrap gap-2 mt-4">
      {!status.connected && <button className="btn btn-primary" type="button" disabled={working || !status.configured} onClick={() => void connect()}>
        {status.reauthorizationRequired ? 'Volver a autorizar' : 'Conectar Google Calendar'}
      </button>}
      {status.connected && <button className="btn btn-outline-danger" type="button" disabled={working} onClick={() => void disconnect()}>
        Desconectar
      </button>}
      {!status.configured && <p className="form-text mb-0 align-self-center">La integración aún no está habilitada por el administrador.</p>}
    </div>}
  </section>
}

function oauthResult() {
  return new URLSearchParams(window.location.search).get('googleCalendar')
}
