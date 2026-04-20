import client from './client'
import type { CreateOrderRequest, OrderResponse } from '@/types/order'

export async function fetchOrders(): Promise<OrderResponse[]> {
  const { data } = await client.get<OrderResponse[]>('/orders')
  return data
}

export async function fetchOrderById(id: string): Promise<OrderResponse> {
  const { data } = await client.get<OrderResponse>(`/orders/${id}`)
  return data
}

export async function createOrder(request: CreateOrderRequest): Promise<OrderResponse> {
  const { data } = await client.post<OrderResponse>('/orders', request)
  return data
}
