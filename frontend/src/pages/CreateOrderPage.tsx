import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button, Card, Flex, Form, InputNumber, Typography, message } from 'antd'
import { PlusOutlined, DeleteOutlined } from '@ant-design/icons'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useProducts } from '@/hooks/useProducts'
import { ProductSelector } from '@/components/create-order/ProductSelector'
import { LoadingSpinner } from '@/components/common/LoadingSpinner'
import { createOrder } from '@/api/orders'
import type { OrderItemRequest } from '@/types/order'
import type { ProductResponse } from '@/types/inventory'
import { formatCurrency } from '@/lib/formatters'

interface ItemDraft {
  key: number
  productId: string
  productName: string
  quantity: number
  unitPrice: number
}

let counter = 0

function emptyItem(): ItemDraft {
  return { key: counter++, productId: '', productName: '', quantity: 1, unitPrice: 0 }
}

export function CreateOrderPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [messageApi, contextHolder] = message.useMessage()
  const { data: products, isLoading: productsLoading } = useProducts()
  const [items, setItems] = useState<ItemDraft[]>([emptyItem()])

  const mutation = useMutation({
    mutationFn: createOrder,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['orders'] })
      void messageApi.success('Pedido criado com sucesso!')
      navigate('/')
    },
    onError: () => {
      void messageApi.error('Erro ao criar pedido. Tenta novamente.')
    },
  })

  if (productsLoading) return <LoadingSpinner />

  function addItem() {
    setItems((prev) => [...prev, emptyItem()])
  }

  function removeItem(key: number) {
    setItems((prev) => prev.filter((i) => i.key !== key))
  }

  function updateItem(key: number, patch: Partial<ItemDraft>) {
    setItems((prev) => prev.map((i) => (i.key === key ? { ...i, ...patch } : i)))
  }

  function handleProductChange(key: number, _productId: string, product: ProductResponse) {
    updateItem(key, {
      productId: product.id,
      productName: product.name,
      unitPrice: 0, // price must be set by user — not stored in inventory service
    })
  }

  function handleSubmit() {
    if (items.some((i) => !i.productId || i.quantity < 1 || i.unitPrice <= 0)) {
      void messageApi.warning('Preenche todos os campos de cada item.')
      return
    }
    const request: { items: OrderItemRequest[] } = {
      items: items.map((i) => ({
        productId: i.productId,
        productName: i.productName,
        quantity: i.quantity,
        unitPrice: i.unitPrice,
      })),
    }
    mutation.mutate(request)
  }

  const total = items.reduce((sum, i) => sum + i.quantity * i.unitPrice, 0)

  return (
    <>
      {contextHolder}
      <Typography.Title level={3}>Novo pedido</Typography.Title>
      <Card>
        <Flex vertical gap={16}>
          {items.map((item, idx) => (
            <Flex key={item.key} gap={12} align="flex-start" wrap="wrap">
              <Form.Item label={idx === 0 ? 'Produto' : undefined} style={{ flex: 2, minWidth: 200, marginBottom: 0 }}>
                <ProductSelector
                  products={products ?? []}
                  value={item.productId || undefined}
                  onChange={(pid, product) => handleProductChange(item.key, pid, product)}
                />
              </Form.Item>
              <Form.Item label={idx === 0 ? 'Preço (€)' : undefined} style={{ width: 120, marginBottom: 0 }}>
                <InputNumber
                  min={0.01}
                  step={0.01}
                  value={item.unitPrice || undefined}
                  onChange={(v) => updateItem(item.key, { unitPrice: v ?? 0 })}
                  style={{ width: '100%' }}
                />
              </Form.Item>
              <Form.Item label={idx === 0 ? 'Qtd' : undefined} style={{ width: 90, marginBottom: 0 }}>
                <InputNumber
                  min={1}
                  value={item.quantity}
                  onChange={(v) => updateItem(item.key, { quantity: v ?? 1 })}
                  style={{ width: '100%' }}
                />
              </Form.Item>
              <Form.Item label={idx === 0 ? 'Subtotal' : undefined} style={{ width: 100, marginBottom: 0 }}>
                <Typography.Text>{formatCurrency(item.quantity * item.unitPrice)}</Typography.Text>
              </Form.Item>
              <Form.Item label={idx === 0 ? ' ' : undefined} style={{ marginBottom: 0 }}>
                <Button
                  danger
                  icon={<DeleteOutlined />}
                  onClick={() => removeItem(item.key)}
                  disabled={items.length === 1}
                />
              </Form.Item>
            </Flex>
          ))}

          <Flex justify="space-between" align="center">
            <Button icon={<PlusOutlined />} onClick={addItem}>
              Adicionar item
            </Button>
            <Typography.Text strong>Total: {formatCurrency(total)}</Typography.Text>
          </Flex>

          <Flex justify="flex-end" gap={12}>
            <Button onClick={() => navigate('/')}>Cancelar</Button>
            <Button type="primary" onClick={handleSubmit} loading={mutation.isPending}>
              Criar pedido
            </Button>
          </Flex>
        </Flex>
      </Card>
    </>
  )
}
