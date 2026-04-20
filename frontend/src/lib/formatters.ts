import dayjs from 'dayjs'
import type { OrderStatus } from '@/types/order'
import type { PaymentStatus } from '@/types/payment'
import type { NotificationType } from '@/types/notification'

export function formatCurrency(amount: number, currency = 'EUR'): string {
  return new Intl.NumberFormat('pt-PT', { style: 'currency', currency }).format(amount)
}

export function formatDate(iso: string): string {
  return dayjs(iso).format('DD/MM/YYYY HH:mm')
}

export function formatOrderStatus(status: OrderStatus): string {
  const labels: Record<OrderStatus, string> = {
    PENDING: 'Pendente',
    CONFIRMED: 'Confirmado',
    SHIPPED: 'Enviado',
    DELIVERED: 'Entregue',
    CANCELLED: 'Cancelado',
  }
  return labels[status]
}

export function formatPaymentStatus(status: PaymentStatus): string {
  const labels: Record<PaymentStatus, string> = {
    PENDING: 'Pendente',
    PROCESSED: 'Processado',
    FAILED: 'Falhado',
  }
  return labels[status]
}

export function formatNotificationType(type: NotificationType): string {
  const labels: Record<NotificationType, string> = {
    ORDER_CREATED: 'Pedido criado',
    ORDER_CANCELLED: 'Pedido cancelado',
    PAYMENT_PROCESSED: 'Pagamento processado',
    PAYMENT_FAILED: 'Pagamento falhado',
    STOCK_UNAVAILABLE: 'Stock indisponível',
  }
  return labels[type]
}
