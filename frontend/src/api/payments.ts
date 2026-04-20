import client from './client'
import type { PaymentResponse } from '@/types/payment'

export async function fetchPaymentByOrderId(orderId: string): Promise<PaymentResponse> {
  const { data } = await client.get<PaymentResponse>(`/payments/order/${orderId}`)
  return data
}
