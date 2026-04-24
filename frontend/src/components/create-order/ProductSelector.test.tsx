import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ProductSelector } from './ProductSelector'
import type { ProductResponse } from '@/types/inventory'

const mockProducts: ProductResponse[] = [
  {
    id: 'prod-001',
    name: 'Teclado Mecânico',
    sku: 'KB-001',
    availableQuantity: 10,
    reservedQuantity: 2,
    createdAt: '2026-04-20T09:00:00+01:00',
    updatedAt: '2026-04-20T09:00:00+01:00',
  },
  {
    id: 'prod-002',
    name: 'Rato Gaming',
    sku: 'MO-001',
    availableQuantity: 0,
    reservedQuantity: 5,
    createdAt: '2026-04-20T09:00:00+01:00',
    updatedAt: '2026-04-20T09:00:00+01:00',
  },
]

describe('ProductSelector', () => {
  it('renderiza o placeholder', () => {
    render(<ProductSelector products={mockProducts} />)
    expect(screen.getByText('Selecionar produto')).toBeInTheDocument()
  })

  it('mostra os produtos ao abrir o dropdown', async () => {
    render(<ProductSelector products={mockProducts} />)
    const user = userEvent.setup()
    await user.click(screen.getByRole('combobox'))
    expect(await screen.findByText(/Teclado Mecânico/)).toBeInTheDocument()
  })

  it('chama onChange com produto correcto ao selecionar', async () => {
    const onChange = vi.fn()
    render(<ProductSelector products={mockProducts} onChange={onChange} />)
    const user = userEvent.setup()
    await user.click(screen.getByRole('combobox'))
    await user.click(await screen.findByText(/Teclado Mecânico/))
    expect(onChange).toHaveBeenCalledWith('prod-001', mockProducts[0])
  })

  it('desabilita o select quando disabled=true', () => {
    render(<ProductSelector products={mockProducts} disabled />)
    expect(screen.getByRole('combobox')).toBeDisabled()
  })
})
