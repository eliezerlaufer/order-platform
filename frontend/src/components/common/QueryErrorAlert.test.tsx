import { render, screen } from '@testing-library/react'
import { QueryErrorAlert } from './QueryErrorAlert'

describe('QueryErrorAlert', () => {
  it('mostra mensagem genérica para erro desconhecido', () => {
    render(<QueryErrorAlert error={new Error('algo correu mal')} />)
    expect(screen.getByText('algo correu mal')).toBeInTheDocument()
  })

  it('mostra mensagem genérica quando o erro é null', () => {
    render(<QueryErrorAlert error={null} />)
    expect(screen.getByText('Ocorreu um erro inesperado.')).toBeInTheDocument()
  })

  it('mostra mensagem da resposta da API quando disponível', () => {
    const axiosLikeError = {
      response: { data: { message: 'Order not found' } },
    }
    render(<QueryErrorAlert error={axiosLikeError} />)
    expect(screen.getByText('Order not found')).toBeInTheDocument()
  })

  it('renderiza o título "Erro"', () => {
    render(<QueryErrorAlert error={new Error('test')} />)
    expect(screen.getByText('Erro')).toBeInTheDocument()
  })
})
