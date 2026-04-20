export type PaymentStatus = 'PENDING' | 'PROCESSED' | 'FAILED'

export interface PaymentResponse {
  id: string
  orderId: string
  amount: number
  status: PaymentStatus
  createdAt: string
  updatedAt: string
}
