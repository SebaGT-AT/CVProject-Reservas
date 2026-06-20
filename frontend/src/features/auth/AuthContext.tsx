import { useMemo, useState, type ReactNode } from 'react'
import type { AuthResponse } from './types'
import { AuthContext, type AuthContextValue } from './auth-context'

const STORAGE_KEY = 'reservas.session'
function readSession(): AuthResponse | null {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY)
    return raw ? (JSON.parse(raw) as AuthResponse) : null
  } catch {
    sessionStorage.removeItem(STORAGE_KEY)
    return null
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<AuthResponse | null>(readSession)

  const value = useMemo<AuthContextValue>(() => ({
    user: session?.user ?? null,
    token: session?.accessToken ?? null,
    authenticate(next) {
      sessionStorage.setItem(STORAGE_KEY, JSON.stringify(next))
      setSession(next)
    },
    logout() {
      sessionStorage.removeItem(STORAGE_KEY)
      setSession(null)
    },
  }), [session])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
