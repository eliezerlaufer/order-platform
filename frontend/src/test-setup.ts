import '@testing-library/jest-dom'
import { configure } from '@testing-library/react'
import { server } from './mocks/server'

// Aumenta o timeout para operações assíncronas (MSW + React Query podem precisar de mais tempo).
configure({ asyncUtilTimeout: 5000 })

// Ant Design Select usa ResizeObserver internamente. O jsdom não o implementa.
global.ResizeObserver = class ResizeObserver {
  observe() {}
  unobserve() {}
  disconnect() {}
}

// Ant Design usa window.matchMedia internamente (responsiveObserver).
// O jsdom não implementa matchMedia, por isso é necessário fazer mock aqui.
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: vi.fn().mockImplementation((query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  })),
})

beforeAll(() => server.listen({ onUnhandledRequest: 'warn' }))
afterEach(() => server.resetHandlers())
afterAll(() => server.close())
