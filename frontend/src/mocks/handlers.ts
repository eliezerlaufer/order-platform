import { http, HttpResponse } from 'msw'
import type { OrderResponse } from '@/types/order'
import type { PaymentResponse } from '@/types/payment'
import type { ProductResponse } from '@/types/inventory'
import type { NotificationResponse } from '@/types/notification'

const BASE = '/api'

export const mockOrder: OrderResponse = {
  id: 'aaaaaaaa-0000-0000-0000-000000000001',
  customerId: 'cccccccc-0000-0000-0000-000000000001',
  status: 'CONFIRMED',
  totalAmount: 179.98,
  currency: 'EUR',
  items: [
    {
      id: 'iiiiiiii-0000-0000-0000-000000000001',
      productId: 'pppppppp-0000-0000-0000-000000000001',
      productName: 'Teclado Mecânico',
      quantity: 2,
      unitPrice: 89.99,
    },
  ],
  createdAt: '2026-04-20T10:00:00+01:00',
  updatedAt: '2026-04-20T10:01:00+01:00',
}

export const mockProduct: ProductResponse = {
  id: 'pppppppp-0000-0000-0000-000000000001',
  name: 'Teclado Mecânico',
  sku: 'KB-001',
  availableQuantity: 10,
  reservedQuantity: 2,
  createdAt: '2026-04-20T09:00:00+01:00',
  updatedAt: '2026-04-20T09:00:00+01:00',
}

export const mockPayment: PaymentResponse = {
  id: 'eeeeeeee-0000-0000-0000-000000000001',
  orderId: mockOrder.id,
  amount: 179.98,
  status: 'PROCESSED',
  createdAt: '2026-04-20T10:02:00+01:00',
  updatedAt: '2026-04-20T10:02:00+01:00',
}

export const mockNotification: NotificationResponse = {
  id: 'ffffffff-0000-0000-0000-000000000001',
  orderId: mockOrder.id,
  customerId: mockOrder.customerId,
  type: 'ORDER_CREATED',
  channel: 'email',
  message: 'O teu pedido foi recebido.',
  createdAt: '2026-04-20T10:00:30+01:00',
}

export const handlers = [
  http.get(`${BASE}/orders`, () => HttpResponse.json([mockOrder])),
  http.get(`${BASE}/orders/:id`, () => HttpResponse.json(mockOrder)),
  http.post(`${BASE}/orders`, () => HttpResponse.json(mockOrder, { status: 201 })),
  http.get(`${BASE}/products`, () => HttpResponse.json([mockProduct])),
  http.get(`${BASE}/payments/order/:orderId`, () => HttpResponse.json(mockPayment)),
  http.get(`${BASE}/notifications/order/:orderId`, () => HttpResponse.json([mockNotification])),
]
