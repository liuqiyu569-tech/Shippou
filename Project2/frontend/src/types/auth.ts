export interface AuthUser {
  id: number
  username: string
}

export interface AuthPayload {
  username: string
  password: string
}

export interface AuthSession {
  token: string
  tokenType: string
  expiresIn: number
  user: AuthUser
}
