import { Component, type ErrorInfo, type ReactNode } from 'react'

type Props = { children: ReactNode }
type State = { failed: boolean }

export class ErrorBoundary extends Component<Props, State> {
  state: State = { failed: false }

  static getDerivedStateFromError(): State {
    return { failed: true }
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('Unexpected application error', error, info.componentStack)
  }

  render() {
    if (this.state.failed) {
      return <main className="container min-vh-100 d-flex align-items-center justify-content-center">
        <section className="workspace-card text-center" role="alert">
          <p className="eyebrow text-uppercase">Algo salió mal</p>
          <h1>No pudimos mostrar esta pantalla</h1>
          <p>Recarga la aplicación. Si el problema continúa, informa el código de solicitud al soporte.</p>
          <button className="btn btn-primary" type="button" onClick={() => window.location.reload()}>
            Recargar
          </button>
        </section>
      </main>
    }
    return this.props.children
  }
}
