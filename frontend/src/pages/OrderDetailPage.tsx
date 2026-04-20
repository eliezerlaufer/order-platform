import { useParams, useNavigate } from 'react-router-dom'
import { Card, Descriptions, Timeline, Typography, Button, Flex, Tag } from 'antd'
import { ArrowLeftOutlined } from '@ant-design/icons'
import { useOrder } from '@/hooks/useOrder'
import { usePayment } from '@/hooks/usePayment'
import { useNotifications } from '@/hooks/useNotifications'
import { OrderStatusBadge } from '@/components/orders/OrderStatusBadge'
import { OrderItemsTable } from '@/components/orders/OrderItemsTable'
import { LoadingSpinner } from '@/components/common/LoadingSpinner'
import { QueryErrorAlert } from '@/components/common/QueryErrorAlert'
import { formatCurrency, formatDate, formatPaymentStatus, formatNotificationType } from '@/lib/formatters'
import type { PaymentStatus } from '@/types/payment'

const paymentStatusColor: Record<PaymentStatus, string> = {
  PENDING: 'orange',
  PROCESSED: 'green',
  FAILED: 'red',
}

export function OrderDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()

  const { data: order, isLoading: orderLoading, error: orderError } = useOrder(id!)
  const { data: payment } = usePayment(id!)
  const { data: notifications } = useNotifications(id!)

  if (orderLoading) return <LoadingSpinner />
  if (orderError) return <QueryErrorAlert error={orderError} />
  if (!order) return null

  return (
    <Flex vertical gap={24}>
      <Flex align="center" gap={12}>
        <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/')}>
          Voltar
        </Button>
        <Typography.Title level={3} style={{ margin: 0 }}>
          Pedido {order.id.slice(0, 8)}…
        </Typography.Title>
        <OrderStatusBadge status={order.status} />
      </Flex>

      <Card title="Informação do pedido">
        <Descriptions column={2}>
          <Descriptions.Item label="ID">{order.id}</Descriptions.Item>
          <Descriptions.Item label="Cliente">{order.customerId}</Descriptions.Item>
          <Descriptions.Item label="Total">{formatCurrency(order.totalAmount, order.currency)}</Descriptions.Item>
          <Descriptions.Item label="Criado em">{formatDate(order.createdAt)}</Descriptions.Item>
        </Descriptions>
        <Typography.Title level={5}>Itens</Typography.Title>
        <OrderItemsTable items={order.items} />
      </Card>

      {payment && (
        <Card title="Pagamento">
          <Descriptions column={2}>
            <Descriptions.Item label="Status">
              <Tag color={paymentStatusColor[payment.status]}>{formatPaymentStatus(payment.status)}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="Valor">{formatCurrency(payment.amount)}</Descriptions.Item>
            <Descriptions.Item label="Processado em">{formatDate(payment.updatedAt)}</Descriptions.Item>
          </Descriptions>
        </Card>
      )}

      {notifications && notifications.length > 0 && (
        <Card title="Linha do tempo">
          <Timeline
            items={notifications.map((n) => ({
              key: n.id,
              children: (
                <>
                  <Typography.Text strong>{formatNotificationType(n.type)}</Typography.Text>
                  <br />
                  <Typography.Text type="secondary">{n.message}</Typography.Text>
                  <br />
                  <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                    {formatDate(n.createdAt)} · {n.channel}
                  </Typography.Text>
                </>
              ),
            }))}
          />
        </Card>
      )}
    </Flex>
  )
}
