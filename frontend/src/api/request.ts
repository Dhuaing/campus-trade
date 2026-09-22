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
  auditRemark?: string | null
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

export interface ConversationMessage {
  id: number
  fromUserId: number
  fromUserNickname: string
  toUserId: number
  content: string
  isRead: boolean
  createdAt: string
}

export interface OrderItem {
  id: number
  product: { id: number; title: string; price: number; coverImage?: string } | null
  buyer: { id: number; nickname: string }
  seller: { id: number; nickname: string }
  status: string
  createdAt: string
}

export interface ProductInput {
  title: string
  description?: string
  price: number
  originalPrice?: number
  category?: string
  coverImage?: string
}

export interface ProductPage {
  content: Product[]
  totalElements: number
  page: number
  size: number
  hasMore: boolean
}

function authHeaders(): Record<string, string> {
  const token = localStorage.getItem('token')
  return token ? { Authorization: `Bearer ${token}` } : {}
}

export async function getProducts(q?: string, page = 0, size = 20): Promise<ProductPage> {
  const params = new URLSearchParams()
  if (q) params.set('q', q)
  params.set('page', String(page))
  params.set('size', String(size))
  const res = await fetch(`${baseURL}/api/products?${params.toString()}`)
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

/** 我发布的商品 */
export async function getMyProducts(): Promise<Product[]> {
  const res = await fetch(`${baseURL}/api/products/mine`, { headers: authHeaders() })
  if (res.status === 401) throw new Error('请先登录')
  if (!res.ok) throw new Error(`获取失败: ${res.status}`)
  return res.json()
}

/** 下架（删除）自己的商品 */
export async function deleteProduct(id: number): Promise<void> {
  const res = await fetch(`${baseURL}/api/products/${id}`, {
    method: 'DELETE',
    headers: authHeaders()
  })
  if (res.status === 401) throw new Error('请先登录')
  if (res.status === 403) throw new Error('无权操作他人商品')
  if (!res.ok) throw new Error(`删除失败: ${res.status}`)
}

/** 标记商品已售 */
export async function markProductSold(id: number): Promise<{ id: number; status: string }> {
  const res = await fetch(`${baseURL}/api/products/${id}/sold`, {
    method: 'PUT',
    headers: authHeaders()
  })
  if (res.status === 401) throw new Error('请先登录')
  if (res.status === 403) throw new Error('无权操作他人商品')
  if (!res.ok) throw new Error(`操作失败: ${res.status}`)
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

/** 买家下单 */
export async function createOrder(productId: number): Promise<OrderItem> {
  const res = await fetch(`${baseURL}/api/orders`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify({ productId })
  })
  if (res.status === 401) throw new Error('请先登录')
  if (res.status === 403) throw new Error('不能购买自己发布的商品')
  if (res.status === 409) throw new Error('商品当前不可购买')
  if (!res.ok) throw new Error(`下单失败: ${res.status}`)
  return res.json()
}

/** 我的订单：bought 我买到的 / sold 我卖出的 */
export async function getMyOrders(): Promise<{ bought: OrderItem[]; sold: OrderItem[] }> {
  const res = await fetch(`${baseURL}/api/orders/mine`, { headers: authHeaders() })
  if (res.status === 401) throw new Error('请先登录')
  if (!res.ok) throw new Error(`获取订单失败: ${res.status}`)
  return res.json()
}

/** 卖家确认完成订单 */
export async function completeOrder(id: number): Promise<OrderItem> {
  const res = await fetch(`${baseURL}/api/orders/${id}/complete`, {
    method: 'PUT',
    headers: authHeaders()
  })
  if (res.status === 401) throw new Error('请先登录')
  if (res.status === 403) throw new Error('仅卖家可确认完成')
  if (res.status === 409) throw new Error('订单已处理，无法重复操作')
  if (!res.ok) throw new Error(`操作失败: ${res.status}`)
  return res.json()
}

/** 取消订单（买家或卖家），商品恢复在售 */
export async function cancelOrder(id: number): Promise<OrderItem> {
  const res = await fetch(`${baseURL}/api/orders/${id}/cancel`, {
    method: 'PUT',
    headers: authHeaders()
  })
  if (res.status === 401) throw new Error('请先登录')
  if (res.status === 403) throw new Error('无权操作该订单')
  if (res.status === 409) throw new Error('订单已处理，无法重复操作')
  if (!res.ok) throw new Error(`操作失败: ${res.status}`)
  return res.json()
}

/** 获取与指定用户的会话消息列表 */
export async function getConversation(userId: number): Promise<ConversationMessage[]> {
  const res = await fetch(`${baseURL}/api/messages/conversation/${userId}`, { headers: authHeaders() })
  if (res.status === 401) throw new Error('请先登录')
  if (!res.ok) throw new Error(`加载会话失败: ${res.status}`)
  const data = await res.json()
  return data.messages
}
