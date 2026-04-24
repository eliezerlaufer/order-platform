import { screen } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { server } from '@/mocks/server'
import { mockOrder, mockNotification } from '@/mocks/handlers'
import { renderWithProviders } from '@/test-utils'
import { OrderDetailPage } from './OrderDetailPage'

// A página usa useParams({ id }) — routePath define o pattern para que o React
// Router extraia o parâmetro :id do URL passado em initialEntries.
function renderDetailPage(orderId = mockOrder.id) {
  return renderWithProviders(<OrderDetailPage />, {
    initialEntries: [`/orders/${orderId}`],
    routePath: '/orders/:id',
  })
}

describe('OrderDetailPage', () => {
  it('mostra spinner enquanto carrega', () => {
    renderDetailPage()
    expect(document.querySelector('.ant-spin')).toBeInTheDocument()
  })

  it('mostra informação do pedido após carregar', async () => {
    renderDetailPage()
    expect(await screen.findByText('Informação do pedido')).toBeInTheDocument()
    expect(screen.getByText(mockOrder.id)).toBeInTheDocument()
    expect(screen.getByText(mockOrder.customerId)).toBeInTheDocument()
  })

  it('mostra o badge de status do pedido', async () => {
    renderDetailPage()
    // mockOrder.status = 'CONFIRMED'
    expect(await screen.findByText('Confirmado')).toBeInTheDocument()
  })

  it('mostra o cartão de pagamento quando disponível', async () => {
    renderDetailPage()
    expect(await screen.findByText('Pagamento')).toBeInTheDocument()
    // mockPayment.status = 'PROCESSED'
    expect(screen.getByText('Processado')).toBeInTheDocument()
  })

  it('mostra a linha do tempo de notificações', async () => {
    renderDetailPage()
    expect(await screen.findByText('Linha do tempo')).toBeInTheDocument()
    expect(screen.getByText(mockNotification.message)).toBeInTheDocument()
  })

  it('mostra erro quando a API do pedido falha', async () => {
    server.use(
      http.get('/api/orders/:id', () =>
        HttpResponse.json({ message: 'Order not found' }, { status: 404 }),
      ),
    )
    renderDetailPage()
    expect(await screen.findByText('Erro')).toBeInTheDocument()
  })

  it('não mostra cartão de pagamento quando não há pagamento', async () => {
    server.use(
      http.get('/api/payments/order/:orderId', () => HttpResponse.json(null, { status: 404 })),
    )
    renderDetailPage()
    expect(await screen.findByText('Informação do pedido')).toBeInTheDocument()
    expect(screen.queryByText('Pagamento')).not.toBeInTheDocument()
  })

  it('mostra botão de voltar', async () => {
    renderDetailPage()
    expect(await screen.findByText('Voltar')).toBeInTheDocument()
  })
})
