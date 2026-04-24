import { screen } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { server } from '@/mocks/server'
import { mockOrder } from '@/mocks/handlers'
import { renderWithProviders } from '@/test-utils'
import { DashboardPage } from './DashboardPage'

describe('DashboardPage', () => {
  it('mostra spinner enquanto carrega', () => {
    renderWithProviders(<DashboardPage />)
    expect(document.querySelector('.ant-spin')).toBeInTheDocument()
  })

  it('mostra a tabela de pedidos após carregar', async () => {
    renderWithProviders(<DashboardPage />)
    expect(await screen.findByText('Pedidos')).toBeInTheDocument()
    expect(screen.getByText('Ver detalhe')).toBeInTheDocument()
  })

  it('mostra o status do pedido na tabela', async () => {
    renderWithProviders(<DashboardPage />)
    // mockOrder.status = 'CONFIRMED'
    expect(await screen.findByText('Confirmado')).toBeInTheDocument()
  })

  it('mostra mensagem de erro quando a API falha', async () => {
    server.use(
      http.get('/api/orders', () => HttpResponse.json({ message: 'Internal Server Error' }, { status: 500 })),
    )
    renderWithProviders(<DashboardPage />)
    expect(await screen.findByText('Erro')).toBeInTheDocument()
  })

  it('mostra tabela vazia quando não há pedidos', async () => {
    server.use(
      http.get('/api/orders', () => HttpResponse.json([])),
    )
    renderWithProviders(<DashboardPage />)
    expect(await screen.findByText('Pedidos')).toBeInTheDocument()
    expect(screen.getAllByText('No data').length).toBeGreaterThan(0)
  })

  it('mostra o ID truncado do pedido', async () => {
    renderWithProviders(<DashboardPage />)
    expect(await screen.findByText(`${mockOrder.id.slice(0, 8)}…`)).toBeInTheDocument()
  })
})
