import { createContext, useContext } from 'react'
import type { AuthResponse, AuthUser } from './types'

export type AuthContextValue = {
  user: AuthUser | null
  token: string | null
  loading: boolean
  authenticate: (session: AuthResponse) => void
  logout: () => Promise<void>
}

export const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth debe usarse dentro de AuthProvider')
  return context
}
