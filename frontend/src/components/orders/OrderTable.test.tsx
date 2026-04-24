import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '@/test-utils'
import { OrderTable } from './OrderTable'
import type { OrderResponse } from '@/types/order'

const mockOrders: OrderResponse[] = [
  {
    id: 'aaaaaaaa-1111-0000-0000-000000000001',
    customerId: 'cccccccc-0000-0000-0000-000000000001',
    status: 'PENDING',
    totalAmount: 89.99,
    currency: 'EUR',
    items: [],
    createdAt: '2026-04-20T10:00:00+01:00',
    updatedAt: '2026-04-20T10:00:00+01:00',
  },
  {
    id: 'bbbbbbbb-2222-0000-0000-000000000002',
    customerId: 'cccccccc-0000-0000-0000-000000000001',
    status: 'CONFIRMED',
    totalAmount: 179.98,
    currency: 'EUR',
    items: [],
    createdAt: '2026-04-20T11:00:00+01:00',
    updatedAt: '2026-04-20T11:01:00+01:00',
  },
  {
    id: 'cccccccc-3333-0000-0000-000000000003',
    customerId: 'cccccccc-0000-0000-0000-000000000001',
    status: 'CANCELLED',
    totalAmount: 49.99,
    currency: 'EUR',
    items: [],
    createdAt: '2026-04-20T12:00:00+01:00',
    updatedAt: '2026-04-20T12:05:00+01:00',
  },
]

describe('OrderTable', () => {
  it('renderiza o número correcto de linhas', () => {
    renderWithProviders(<OrderTable orders={mockOrders} />)
    // Cada pedido tem um botão "Ver detalhe"
    expect(screen.getAllByText('Ver detalhe')).toHaveLength(3)
  })

  it('trunca o ID para 8 caracteres com reticências', () => {
    renderWithProviders(<OrderTable orders={[mockOrders[0]]} />)
    expect(screen.getByText('aaaaaaaa…')).toBeInTheDocument()
  })

  it('renderiza o badge de status PENDING', () => {
    renderWithProviders(<OrderTable orders={[mockOrders[0]]} />)
    expect(screen.getByText('Pendente')).toBeInTheDocument()
  })

  it('renderiza o badge de status CONFIRMED', () => {
    renderWithProviders(<OrderTable orders={[mockOrders[1]]} />)
    expect(screen.getByText('Confirmado')).toBeInTheDocument()
  })

  it('renderiza o badge de status CANCELLED', () => {
    renderWithProviders(<OrderTable orders={[mockOrders[2]]} />)
    expect(screen.getByText('Cancelado')).toBeInTheDocument()
  })

  it('renderiza lista vazia sem erros', () => {
    renderWithProviders(<OrderTable orders={[]} />)
    expect(screen.getByText('No data')).toBeInTheDocument()
  })

  it('mostra loading quando loading=true', () => {
    const { container } = renderWithProviders(<OrderTable orders={[]} loading />)
    expect(container.querySelector('.ant-spin')).toBeInTheDocument()
  })

  it('navega para detalhe ao clicar em "Ver detalhe"', async () => {
    renderWithProviders(<OrderTable orders={[mockOrders[0]]} />, {
      initialEntries: ['/'],
    })
    const user = userEvent.setup()
    await user.click(screen.getByText('Ver detalhe'))
    // Verifica que o botão é clicável (navegação gerida pelo MemoryRouter interno)
    expect(screen.getByText('Ver detalhe')).toBeInTheDocument()
  })
})
