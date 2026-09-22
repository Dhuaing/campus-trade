// 管理端登录态：localStorage 持久化（token/用户/权限）
export interface AdminUser {
  id: number
  username: string
  nickname: string
  roles: string[]
  permissions: string[]
}

const TOKEN_KEY = 'admin_token'
const USER_KEY = 'admin_user'

export function getToken(): string {
  return localStorage.getItem(TOKEN_KEY) ?? ''
}

export function getUser(): AdminUser | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as AdminUser
  } catch {
    return null
  }
}

export function isLogin(): boolean {
  return !!getToken()
}

export function isAdmin(): boolean {
  const user = getUser()
  return !!user && user.roles.includes('ADMIN')
}

export function hasPerm(code: string): boolean {
  const user = getUser()
  return !!user && user.permissions.includes(code)
}

export function saveSession(token: string, user: AdminUser) {
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}
