import { Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import type { OrderItemResponse } from '@/types/order'
import { formatCurrency } from '@/lib/formatters'

interface Props {
  items: OrderItemResponse[]
}

export function OrderItemsTable({ items }: Props) {
  const columns: ColumnsType<OrderItemResponse> = [
    { title: 'Produto', dataIndex: 'productName', key: 'productName' },
    { title: 'Qtd', dataIndex: 'quantity', key: 'quantity' },
    {
      title: 'Preço unit.',
      dataIndex: 'unitPrice',
      key: 'unitPrice',
      render: (p: number) => formatCurrency(p),
    },
    {
      title: 'Subtotal',
      key: 'subtotal',
      render: (_, r) => formatCurrency(r.quantity * r.unitPrice),
    },
  ]

  return <Table rowKey="id" columns={columns} dataSource={items} pagination={false} size="small" />
}
