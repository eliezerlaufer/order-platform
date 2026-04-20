import { Select } from 'antd'
import type { ProductResponse } from '@/types/inventory'

interface Props {
  products: ProductResponse[]
  value?: string
  onChange?: (productId: string, product: ProductResponse) => void
  disabled?: boolean
}

export function ProductSelector({ products, value, onChange, disabled }: Props) {
  function handleChange(productId: string) {
    const product = products.find((p) => p.id === productId)
    if (product && onChange) {
      onChange(productId, product)
    }
  }

  return (
    <Select
      placeholder="Selecionar produto"
      value={value}
      onChange={handleChange}
      disabled={disabled}
      style={{ width: '100%' }}
      options={products.map((p) => ({
        value: p.id,
        label: `${p.name} (SKU: ${p.sku}) — ${p.availableQuantity} em stock`,
        disabled: p.availableQuantity === 0,
      }))}
    />
  )
}
