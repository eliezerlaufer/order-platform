export type OrderStatus =
  | 'PENDING'
  | 'CONFIRMED'
  | 'SHIPPED'
  | 'DELIVERED'
  | 'CANCELLED'

export interface OrderItemResponse {
  id: string
  productId: string
  productName: string
  quantity: number
  unitPrice: number
}

export interface OrderResponse {
  id: string
  customerId: string
  status: OrderStatus
  totalAmount: number
  currency: string
  items: OrderItemResponse[]
  createdAt: string
  updatedAt: string
}

export interface OrderItemRequest {
  productId: string
  productName: string
  quantity: number
  unitPrice: number
}

export interface CreateOrderRequest {
  items: OrderItemRequest[]
}
