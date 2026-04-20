import { formatCurrency, formatOrderStatus, formatPaymentStatus, formatNotificationType } from './formatters'

describe('formatters', () => {
  it('formats currency', () => {
    const result = formatCurrency(179.98, 'EUR')
    expect(result).toContain('179')
  })

  it('formats all order statuses', () => {
    expect(formatOrderStatus('PENDING')).toBe('Pendente')
    expect(formatOrderStatus('CONFIRMED')).toBe('Confirmado')
    expect(formatOrderStatus('CANCELLED')).toBe('Cancelado')
    expect(formatOrderStatus('SHIPPED')).toBe('Enviado')
    expect(formatOrderStatus('DELIVERED')).toBe('Entregue')
  })

  it('formats all payment statuses', () => {
    expect(formatPaymentStatus('PENDING')).toBe('Pendente')
    expect(formatPaymentStatus('PROCESSED')).toBe('Processado')
    expect(formatPaymentStatus('FAILED')).toBe('Falhado')
  })

  it('formats all notification types', () => {
    expect(formatNotificationType('ORDER_CREATED')).toBe('Pedido criado')
    expect(formatNotificationType('PAYMENT_PROCESSED')).toBe('Pagamento processado')
    expect(formatNotificationType('STOCK_UNAVAILABLE')).toBe('Stock indisponível')
  })
})
