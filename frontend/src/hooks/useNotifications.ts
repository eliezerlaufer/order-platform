import { useQuery } from '@tanstack/react-query'
import { fetchNotificationsByOrderId } from '@/api/notifications'

export function useNotifications(orderId: string) {
  return useQuery({
    queryKey: ['notifications', orderId],
    queryFn: () => fetchNotificationsByOrderId(orderId),
    enabled: !!orderId,
  })
}
