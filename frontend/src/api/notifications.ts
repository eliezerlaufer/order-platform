import client from './client'
import type { NotificationResponse } from '@/types/notification'

export async function fetchNotificationsByOrderId(orderId: string): Promise<NotificationResponse[]> {
  const { data } = await client.get<NotificationResponse[]>(`/notifications/order/${orderId}`)
  return data
}
