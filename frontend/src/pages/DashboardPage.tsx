import { Typography } from 'antd'
import { useOrders } from '@/hooks/useOrders'
import { OrderTable } from '@/components/orders/OrderTable'
import { LoadingSpinner } from '@/components/common/LoadingSpinner'
import { QueryErrorAlert } from '@/components/common/QueryErrorAlert'

export function DashboardPage() {
  const { data: orders, isLoading, error } = useOrders()

  if (isLoading) return <LoadingSpinner />
  if (error) return <QueryErrorAlert error={error} />

  return (
    <>
      <Typography.Title level={3}>Pedidos</Typography.Title>
      <OrderTable orders={orders ?? []} loading={isLoading} />
    </>
  )
}
