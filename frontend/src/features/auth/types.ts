export type Role = 'CUSTOMER' | 'PROFESSIONAL' | 'ADMIN'

export type AuthUser = {
  id: string
  name: string
  email: string
  role: Role
}

export type AuthResponse = {
  accessToken: string
  expiresInSeconds: number
  user: AuthUser
}

