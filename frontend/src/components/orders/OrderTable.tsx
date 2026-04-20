import { Table, Button } from 'antd'
import { useNavigate } from 'react-router-dom'
import type { ColumnsType } from 'antd/es/table'
import type { OrderResponse } from '@/types/order'
import { OrderStatusBadge } from './OrderStatusBadge'
import { formatCurrency, formatDate } from '@/lib/formatters'

interface Props {
  orders: OrderResponse[]
  loading?: boolean
}

export function OrderTable({ orders, loading }: Props) {
  const navigate = useNavigate()

  const columns: ColumnsType<OrderResponse> = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      render: (id: string) => id.slice(0, 8) + '…',
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (status) => <OrderStatusBadge status={status} />,
    },
    {
      title: 'Total',
      dataIndex: 'totalAmount',
      key: 'totalAmount',
      render: (amount, record) => formatCurrency(amount, record.currency),
    },
    {
      title: 'Data',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: (date: string) => formatDate(date),
    },
    {
      title: 'Ação',
      key: 'action',
      render: (_, record) => (
        <Button type="link" onClick={() => navigate(`/orders/${record.id}`)}>
          Ver detalhe
        </Button>
      ),
    },
  ]

  return (
    <Table
      rowKey="id"
      columns={columns}
      dataSource={orders}
      loading={loading}
      pagination={{ pageSize: 20 }}
    />
  )
}
