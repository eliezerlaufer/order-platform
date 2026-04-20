import { render, screen } from '@testing-library/react'
import { OrderStatusBadge } from './OrderStatusBadge'

describe('OrderStatusBadge', () => {
  it('renders PENDING status', () => {
    render(<OrderStatusBadge status="PENDING" />)
    expect(screen.getByText('Pendente')).toBeInTheDocument()
  })

  it('renders CONFIRMED status', () => {
    render(<OrderStatusBadge status="CONFIRMED" />)
    expect(screen.getByText('Confirmado')).toBeInTheDocument()
  })

  it('renders CANCELLED status', () => {
    render(<OrderStatusBadge status="CANCELLED" />)
    expect(screen.getByText('Cancelado')).toBeInTheDocument()
  })
})
