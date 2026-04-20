export interface ProductResponse {
  id: string
  name: string
  sku: string
  availableQuantity: number
  reservedQuantity: number
  createdAt: string
  updatedAt: string
}

export interface CreateProductRequest {
  name: string
  sku: string
  availableQuantity: number
}
