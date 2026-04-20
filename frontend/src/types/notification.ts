export type NotificationType =
  | 'ORDER_CREATED'
  | 'ORDER_CANCELLED'
  | 'PAYMENT_PROCESSED'
  | 'PAYMENT_FAILED'
  | 'STOCK_UNAVAILABLE'

export interface NotificationResponse {
  id: string
  orderId: string
  customerId: string
  type: NotificationType
  channel: string
  message: string
  createdAt: string
}
