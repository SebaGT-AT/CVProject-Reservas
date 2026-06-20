import { useAuth } from '../auth/auth-context'

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
      <div className="row g-4">
        {[['Reservas de hoy', '0'], ['Clientes nuevos', '0'], ['Horas disponibles', '—']].map(([label, value]) => (
          <div className="col-md-4" key={label}><section className="metric-card"><span>{label}</span><strong>{value}</strong></section></div>
        ))}
      </div>
    </main>
  )
}
