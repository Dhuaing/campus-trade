// API 请求封装
// 开发环境 baseURL 为空，走 Vite 代理；生产环境读环境变量
const baseURL = import.meta.env.VITE_API_BASE_URL ?? ''

export interface Product {
  id: number
  title: string
  description: string
  price: number
  originalPrice?: number
  coverImage?: string
  category?: string
  status: string
  createdAt: string
  creator?: { id: number; nickname?: string } | null
}

export interface UserInfo {
  id: number
  username: string
  nickname: string
  studentId: string
}

export interface Message {
  id: number
  fromUser: { id: number; nickname: string }
  productId: number | null
  productTitle: string | null
  content: string
  isRead: boolean
  createdAt: string
}

export interface ProductInput {
  title: string
  description?: string
  price: number
  originalPrice?: number
  category?: string
}

function authHeaders(): Record<string, string> {
  const token = localStorage.getItem('token')
  return token ? { Authorization: `Bearer ${token}` } : {}
}

export async function getProducts(): Promise<Product[]> {
  const res = await fetch(`${baseURL}/api/products`)
  if (!res.ok) throw new Error(`请求失败: ${res.status}`)
  return res.json()
}

export async function getProduct(id: number): Promise<Product> {
  const res = await fetch(`${baseURL}/api/products/${id}`)
  if (!res.ok) throw new Error(`请求失败: ${res.status}`)
  return res.json()
}

/** 发布商品 */
export async function createProduct(input: ProductInput): Promise<Product> {
  const res = await fetch(`${baseURL}/api/products`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify(input)
  })
  if (res.status === 401) throw new Error('请先登录')
  if (!res.ok) throw new Error(`发布失败: ${res.status}`)
  return res.json()
}

/** 注册 */
export async function register(input: {
  username: string
  password: string
  nickname?: string
  studentId?: string
}): Promise<{ id: number; username: string }> {
  const res = await fetch(`${baseURL}/api/auth/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input)
  })
  if (res.status === 409) throw new Error('用户名已存在')
  if (!res.ok) throw new Error(`注册失败: ${res.status}`)
  return res.json()
}

/** 登录 */
export async function login(input: { username: string; password: string }): Promise<{
  token: string
  user: UserInfo
}> {
  const res = await fetch(`${baseURL}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input)
  })
  if (!res.ok) throw new Error('用户名或密码错误')
  return res.json()
}

/** 当前用户 */
export async function getMe(): Promise<UserInfo> {
  const res = await fetch(`${baseURL}/api/auth/me`, { headers: authHeaders() })
  if (!res.ok) throw new Error('未登录')
  return res.json()
}

/** 收件箱 */
export async function getMessages(): Promise<{ messages: Message[]; unread: number }> {
  const res = await fetch(`${baseURL}/api/messages`, { headers: authHeaders() })
  if (!res.ok) throw new Error(`获取消息失败: ${res.status}`)
  return res.json()
}

/** 标记消息已读 */
export async function markMessageRead(id: number): Promise<{ id: number; isRead: boolean }> {
  const res = await fetch(`${baseURL}/api/messages/${id}/read`, {
    method: 'POST',
    headers: authHeaders()
  })
  if (!res.ok) throw new Error(`操作失败: ${res.status}`)
  return res.json()
}

/** 发送消息 */
export async function sendMessage(input: {
  toUserId: number
  productId?: number
  content: string
}): Promise<{ id: number }> {
  const res = await fetch(`${baseURL}/api/messages`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify(input)
  })
  if (res.status === 401) throw new Error('请先登录')
  if (!res.ok) throw new Error(`发送失败: ${res.status}`)
  return res.json()
}
