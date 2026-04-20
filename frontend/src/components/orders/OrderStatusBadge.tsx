import { Tag } from 'antd'
import type { OrderStatus } from '@/types/order'
import { formatOrderStatus } from '@/lib/formatters'

const colorMap: Record<OrderStatus, string> = {
  PENDING: 'orange',
  CONFIRMED: 'green',
  SHIPPED: 'blue',
  DELIVERED: 'cyan',
  CANCELLED: 'red',
}

interface Props {
  status: OrderStatus
}

export function OrderStatusBadge({ status }: Props) {
  return <Tag color={colorMap[status]}>{formatOrderStatus(status)}</Tag>
}
