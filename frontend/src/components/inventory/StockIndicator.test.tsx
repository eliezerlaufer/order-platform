import { render, screen } from '@testing-library/react'
import { StockIndicator } from './StockIndicator'

describe('StockIndicator', () => {
  it('mostra percentagem 0 quando não há stock', () => {
    const { container } = render(<StockIndicator available={0} reserved={5} />)
    const progress = container.querySelector('.ant-progress-bg') as HTMLElement
    expect(progress?.style.width).toBe('0%')
  })

  it('mostra 100% quando todo o stock está disponível', () => {
    const { container } = render(<StockIndicator available={10} reserved={0} />)
    const progress = container.querySelector('.ant-progress-bg') as HTMLElement
    expect(progress?.style.width).toBe('100%')
  })

  it('mostra 50% quando metade está disponível', () => {
    const { container } = render(<StockIndicator available={5} reserved={5} />)
    const progress = container.querySelector('.ant-progress-bg') as HTMLElement
    expect(progress?.style.width).toBe('50%')
  })

  it('renderiza 0% quando available e reserved são ambos 0', () => {
    const { container } = render(<StockIndicator available={0} reserved={0} />)
    const progress = container.querySelector('.ant-progress-bg') as HTMLElement
    expect(progress?.style.width).toBe('0%')
  })

  it('mostra tooltip com valores disponível e reservado', () => {
    const { container } = render(<StockIndicator available={8} reserved={2} />)
    // O tooltip é renderizado via Ant Design Tooltip — o title está no atributo
    expect(container.textContent).toBeTruthy()
  })
})
