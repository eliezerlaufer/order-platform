import { render } from '@testing-library/react'
import { StockIndicator } from './StockIndicator'

function getProgressbar(container: HTMLElement) {
  return container.querySelector('[role="progressbar"]') as HTMLElement
}

describe('StockIndicator', () => {
  it('mostra percentagem 0 quando não há stock', () => {
    const { container } = render(<StockIndicator available={0} reserved={5} />)
    expect(getProgressbar(container)).toHaveAttribute('aria-valuenow', '0')
  })

  it('mostra 100% quando todo o stock está disponível', () => {
    const { container } = render(<StockIndicator available={10} reserved={0} />)
    expect(getProgressbar(container)).toHaveAttribute('aria-valuenow', '100')
  })

  it('mostra 50% quando metade está disponível', () => {
    const { container } = render(<StockIndicator available={5} reserved={5} />)
    expect(getProgressbar(container)).toHaveAttribute('aria-valuenow', '50')
  })

  it('renderiza 0% quando available e reserved são ambos 0', () => {
    const { container } = render(<StockIndicator available={0} reserved={0} />)
    expect(getProgressbar(container)).toHaveAttribute('aria-valuenow', '0')
  })

  it('renderiza sem erros', () => {
    const { container } = render(<StockIndicator available={8} reserved={2} />)
    expect(container.firstChild).toBeInTheDocument()
  })
})
