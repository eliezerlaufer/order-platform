import axios from 'axios'

const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api',
})

export function setAuthToken(token: string): void {
  client.defaults.headers.common['Authorization'] = `Bearer ${token}`
}

export function clearAuthToken(): void {
  delete client.defaults.headers.common['Authorization']
}

export default client
