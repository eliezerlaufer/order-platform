import { useQuery } from '@tanstack/react-query'
import { fetchPaymentByOrderId } from '@/api/payments'

export function usePayment(orderId: string) {
  return useQuery({
    queryKey: ['payment', orderId],
    queryFn: () => fetchPaymentByOrderId(orderId),
    enabled: !!orderId,
  })
}
