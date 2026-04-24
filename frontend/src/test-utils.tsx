import { render, type RenderOptions } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import type { ReactElement } from 'react'

// Wrapper com todos os providers necessários para renderizar páginas e componentes
// que dependem de React Router e TanStack Query.
function createWrapper(initialEntries: string[] = ['/'], routePath?: string) {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,      // sem retry nos testes — falha rápida
        gcTime: 0,         // sem cache entre testes
      },
    },
  })

  function Wrapper({ children }: { children: React.ReactNode }) {
    return (
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={initialEntries}>
          {routePath ? (
            <Routes>
              <Route path={routePath} element={children} />
            </Routes>
          ) : (
            children
          )}
        </MemoryRouter>
      </QueryClientProvider>
    )
  }

  return Wrapper
}

export function renderWithProviders(
  ui: ReactElement,
  options?: Omit<RenderOptions, 'wrapper'> & {
    initialEntries?: string[]
    routePath?: string
  },
) {
  const { initialEntries, routePath, ...renderOptions } = options ?? {}
  return render(ui, {
    wrapper: createWrapper(initialEntries, routePath),
    ...renderOptions,
  })
}
