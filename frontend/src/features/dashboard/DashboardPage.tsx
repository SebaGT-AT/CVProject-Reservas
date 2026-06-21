import { useAuth } from '../auth/auth-context'
import { Link } from 'react-router-dom'

export function DashboardPage() {
  const { user, logout } = useAuth()
  return (
    <main className="container py-5">
      <nav className="d-flex justify-content-between align-items-center mb-5">
        <span className="brand">Reservas</span>
        <button className="btn btn-outline-secondary btn-sm" onClick={logout}>Cerrar sesión</button>
      </nav>
      <p className="eyebrow">PANEL DE CONTROL</p>
      <h1 className="display-5 mb-2">Hola, {user?.name}</h1>
      <p className="text-secondary mb-5">Esta es la fundación. Las métricas se activarán al implementar la agenda.</p>
      {user?.role === 'PROFESSIONAL' && <div className="d-flex gap-2 mb-5"><Link className="btn btn-primary" to="/perfil-profesional">Configurar perfil y servicios</Link><Link className="btn btn-outline-success" to="/configurar-agenda">Configurar agenda</Link></div>}
      <div className="row g-4">
        {[['Reservas de hoy', '0'], ['Clientes nuevos', '0'], ['Horas disponibles', '—']].map(([label, value]) => (
          <div className="col-md-4" key={label}><section className="metric-card"><span>{label}</span><strong>{value}</strong></section></div>
        ))}
      </div>
    </main>
  )
}
