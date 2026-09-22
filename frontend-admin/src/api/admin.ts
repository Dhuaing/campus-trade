// 管理端 API 封装：同源部署（后端静态托管），baseURL 留空；开发环境走 Vite 代理
const baseURL = import.meta.env.VITE_API_BASE_URL ?? ''

import { getToken, clearSession, type AdminUser } from '@/stores/auth'

function authHeaders(): Record<string, string> {
  const token = getToken()
  return token ? { Authorization: `Bearer ${token}` } : {}
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${baseURL}${path}`, {
    ...init,
    headers: { 'Content-Type': 'application/json', ...authHeaders(), ...(init?.headers ?? {}) }
  })
  if (res.status === 401 || res.status === 403) {
    const body = await res.json().catch(() => ({}))
    throw new Error(body.message || '无权限或登录已失效')
  }
  if (!res.ok) {
    const body = await res.json().catch(() => ({}))
    throw new Error(body.message || `请求失败: ${res.status}`)
  }
  return res.json() as Promise<T>
}

export interface PageResult<T> {
  content: T[]
  totalElements: number
  page: number
  size: number
  hasMore: boolean
}

export interface AdminUserRow {
  id: number
  username: string
  nickname: string | null
  studentId: string | null
  status: string
  roles: string[]
  createdAt: string
}

export interface AdminProductRow {
  id: number
  title: string
  description: string | null
  price: number
  originalPrice: number | null
  category: string | null
  status: string
  auditRemark: string | null
  createdAt: string
  creator?: { id: number; nickname: string | null; username: string } | null
}

export interface AuditLogRow {
  id: number
  adminId: number
  adminUsername: string
  action: string
  targetType: string
  targetId: number | null
  detail: string | null
  ip: string | null
  createdAt: string
}

/** 登录 */
export async function login(input: { username: string; password: string }): Promise<{
  token: string
  user: AdminUser
}> {
  const res = await fetch(`${baseURL}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input)
  })
  if (res.status === 401) throw new Error('用户名或密码错误')
  if (res.status === 403) {
    const body = await res.json().catch(() => ({}))
    throw new Error(body.message || '账号已被封禁')
  }
  if (!res.ok) throw new Error(`登录失败: ${res.status}`)
  return res.json()
}

/** 会话失效时清理本地态 */
export function onSessionExpired() {
  clearSession()
  location.hash = '#/login'
}

/** 用户管理 */
export function getUsers(q: string, page: number, size = 20): Promise<PageResult<AdminUserRow>> {
  const params = new URLSearchParams()
  if (q) params.set('q', q)
  params.set('page', String(page))
  params.set('size', String(size))
  return request(`/api/admin/users?${params.toString()}`)
}

export function banUser(id: number): Promise<{ id: number; status: string }> {
  return request(`/api/admin/users/${id}/ban`, { method: 'PUT' })
}

export function unbanUser(id: number): Promise<{ id: number; status: string }> {
  return request(`/api/admin/users/${id}/unban`, { method: 'PUT' })
}

/** 商品审核 */
export function getProducts(status: string, page: number, size = 20): Promise<PageResult<AdminProductRow>> {
  const params = new URLSearchParams()
  if (status) params.set('status', status)
  params.set('page', String(page))
  params.set('size', String(size))
  return request(`/api/admin/products?${params.toString()}`)
}

export function approveProduct(id: number): Promise<{ id: number; status: string }> {
  return request(`/api/admin/products/${id}/approve`, { method: 'PUT' })
}

export function rejectProduct(id: number, reason: string): Promise<{ id: number; status: string }> {
  return request(`/api/admin/products/${id}/reject`, {
    method: 'PUT',
    body: JSON.stringify({ reason })
  })
}

export function removeProduct(id: number, reason: string): Promise<{ id: number; status: string }> {
  return request(`/api/admin/products/${id}/remove`, {
    method: 'PUT',
    body: JSON.stringify({ reason })
  })
}

/** 审计日志 */
export function getAuditLogs(action: string, page: number, size = 20): Promise<PageResult<AuditLogRow>> {
  const params = new URLSearchParams()
  if (action) params.set('action', action)
  params.set('page', String(page))
  params.set('size', String(size))
  return request(`/api/admin/audit-logs?${params.toString()}`)
}
