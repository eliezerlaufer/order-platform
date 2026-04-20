import { Table, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useProducts } from '@/hooks/useProducts'
import { StockIndicator } from '@/components/inventory/StockIndicator'
import { LoadingSpinner } from '@/components/common/LoadingSpinner'
import { QueryErrorAlert } from '@/components/common/QueryErrorAlert'
import type { ProductResponse } from '@/types/inventory'

const columns: ColumnsType<ProductResponse> = [
  { title: 'Nome', dataIndex: 'name', key: 'name' },
  { title: 'SKU', dataIndex: 'sku', key: 'sku' },
  { title: 'Disponível', dataIndex: 'availableQuantity', key: 'available' },
  { title: 'Reservado', dataIndex: 'reservedQuantity', key: 'reserved' },
  {
    title: 'Nível de stock',
    key: 'stock',
    render: (_, r) => <StockIndicator available={r.availableQuantity} reserved={r.reservedQuantity} />,
    width: 200,
  },
]

export function InventoryPage() {
  const { data: products, isLoading, error } = useProducts()

  if (isLoading) return <LoadingSpinner />
  if (error) return <QueryErrorAlert error={error} />

  return (
    <>
      <Typography.Title level={3}>Inventário</Typography.Title>
      <Table rowKey="id" columns={columns} dataSource={products ?? []} pagination={false} />
    </>
  )
}
