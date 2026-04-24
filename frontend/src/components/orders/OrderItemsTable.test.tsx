import { screen } from '@testing-library/react'
import { render } from '@testing-library/react'
import { OrderItemsTable } from './OrderItemsTable'
import type { OrderItemResponse } from '@/types/order'

const mockItems: OrderItemResponse[] = [
  {
    id: 'item-001',
    productId: 'prod-001',
    productName: 'Teclado Mecânico',
    quantity: 2,
    unitPrice: 89.99,
  },
  {
    id: 'item-002',
    productId: 'prod-002',
    productName: 'Rato Gaming',
    quantity: 1,
    unitPrice: 49.99,
  },
]

describe('OrderItemsTable', () => {
  it('renderiza os nomes dos produtos', () => {
    render(<OrderItemsTable items={mockItems} />)
    expect(screen.getByText('Teclado Mecânico')).toBeInTheDocument()
    expect(screen.getByText('Rato Gaming')).toBeInTheDocument()
  })

  it('renderiza as quantidades', () => {
    render(<OrderItemsTable items={mockItems} />)
    expect(screen.getByText('2')).toBeInTheDocument()
    expect(screen.getByText('1')).toBeInTheDocument()
  })

  it('renderiza lista vazia sem erros', () => {
    render(<OrderItemsTable items={[]} />)
    expect(screen.getAllByText('No data').length).toBeGreaterThan(0)
  })

  it('renderiza todos os cabeçalhos das colunas', () => {
    render(<OrderItemsTable items={mockItems} />)
    expect(screen.getByText('Produto')).toBeInTheDocument()
    expect(screen.getByText('Qtd')).toBeInTheDocument()
    expect(screen.getByText('Preço unit.')).toBeInTheDocument()
    expect(screen.getByText('Subtotal')).toBeInTheDocument()
  })
})
