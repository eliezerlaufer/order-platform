import { useQuery } from '@tanstack/react-query'
import { fetchOrderById } from '@/api/orders'

export function useOrder(id: string) {
  return useQuery({
    queryKey: ['orders', id],
    queryFn: () => fetchOrderById(id),
    enabled: !!id,
  })
}
