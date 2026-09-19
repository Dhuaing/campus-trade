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

export interface ProductInput {
  title: string
  description?: string
  price: number
  originalPrice?: number
  category?: string
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

/** 发布商品 */
export async function createProduct(input: ProductInput): Promise<Product> {
  const res = await fetch(`${baseURL}/api/products`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input)
  })
  if (!res.ok) {
    throw new Error(`发布失败: ${res.status}`)
  }
  return res.json()
}
