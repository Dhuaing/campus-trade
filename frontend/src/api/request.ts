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
}

export async function getProducts(): Promise<Product[]> {
  const res = await fetch(`${baseURL}/api/products`)
  if (!res.ok) {
    throw new Error(`请求失败: ${res.status}`)
  }
  return res.json()
}

export async function getProduct(id: number): Promise<Product> {
  const res = await fetch(`${baseURL}/api/products/${id}`)
  if (!res.ok) {
    throw new Error(`请求失败: ${res.status}`)
  }
  return res.json()
}
