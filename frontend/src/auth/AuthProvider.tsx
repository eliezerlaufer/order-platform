import { useEffect, useState, type ReactNode } from 'react'
import keycloak from './keycloak'
import { setAuthToken, clearAuthToken } from '@/api/client'
import { AuthContext } from './AuthContext'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [isAuthenticated, setIsAuthenticated] = useState(false)
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    keycloak
      .init({ onLoad: 'login-required', pkceMethod: 'S256' })
      .then((authenticated) => {
        setIsAuthenticated(authenticated)
        if (authenticated && keycloak.token) {
          setAuthToken(keycloak.token)
        }
      })
      .finally(() => setIsLoading(false))

    keycloak.onTokenExpired = () => {
      keycloak
        .updateToken(30)
        .then((refreshed) => {
          if (refreshed && keycloak.token) {
            setAuthToken(keycloak.token)
          }
        })
        .catch(() => {
          clearAuthToken()
          setIsAuthenticated(false)
        })
    }
  }, [])

  function logout() {
    clearAuthToken()
    keycloak.logout()
  }

  return (
    <AuthContext.Provider
      value={{
        isAuthenticated,
        isLoading,
        userId: keycloak.subject,
        username: keycloak.tokenParsed?.preferred_username as string | undefined,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  )
}
