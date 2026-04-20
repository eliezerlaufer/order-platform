import { useQuery } from '@tanstack/react-query'
import { fetchProducts } from '@/api/inventory'

export function useProducts() {
  return useQuery({
    queryKey: ['products'],
    queryFn: fetchProducts,
  })
}
