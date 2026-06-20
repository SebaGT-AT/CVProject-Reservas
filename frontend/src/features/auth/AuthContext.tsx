import { useEffect, useMemo, useState, type ReactNode } from 'react'
import { api } from '../../lib/api'
import type { AuthResponse } from './types'
import { AuthContext, type AuthContextValue } from './auth-context'

let bootstrapPromise: Promise<AuthResponse> | null = null

function restoreSession() {
  bootstrapPromise ??= api<AuthResponse>('/api/v1/auth/refresh', { method: 'POST' })
  return bootstrapPromise
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<AuthResponse | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let active = true
    restoreSession()
      .then((next) => { if (active) setSession(next) })
      .catch(() => { if (active) setSession(null) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [])

  const value = useMemo<AuthContextValue>(() => ({
    user: session?.user ?? null,
    token: session?.accessToken ?? null,
    loading,
    authenticate(next) {
      setSession(next)
      setLoading(false)
    },
    async logout() {
      try {
        await api<void>('/api/v1/auth/logout', { method: 'POST' })
      } finally {
        setSession(null)
      }
    },
  }), [loading, session])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
