import client from './client'
import type { ProductResponse } from '@/types/inventory'

export async function fetchProducts(): Promise<ProductResponse[]> {
  const { data } = await client.get<ProductResponse[]>('/products')
  return data
}

export async function fetchProductById(id: string): Promise<ProductResponse> {
  const { data } = await client.get<ProductResponse>(`/products/${id}`)
  return data
}
