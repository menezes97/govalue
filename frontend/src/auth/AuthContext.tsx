import { useQueryClient } from '@tanstack/react-query'
import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { AUTH_STORAGE_KEY, api, onUnauthorized } from '../api/client'
import { ehDesafioFacial, type LoginResponse, type LoginResultado, type Usuario } from '../api/types'

interface AuthContextValue {
  usuario: Usuario | null
  login: (email: string, senha: string) => Promise<LoginResultado>
  loginFace: (tokenFacePendente: string, imagemBase64: string) => Promise<Usuario>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

// Maior atraso aceito por setTimeout (~24,8 dias).
const MAX_TIMEOUT = 2_147_483_647

/** Le a sessao salva e descarta se o token ja expirou. */
function carregarSessao(): LoginResponse | null {
  try {
    const raw = localStorage.getItem(AUTH_STORAGE_KEY)
    if (!raw) return null
    const sessao = JSON.parse(raw) as LoginResponse
    if (new Date(sessao.expiraEm).getTime() <= Date.now()) {
      localStorage.removeItem(AUTH_STORAGE_KEY)
      return null
    }
    return sessao
  } catch {
    return null
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [sessao, setSessao] = useState<LoginResponse | null>(carregarSessao)

  const logout = useCallback(() => {
    localStorage.removeItem(AUTH_STORAGE_KEY)
    setSessao(null)
    queryClient.clear()
  }, [queryClient])

  useEffect(() => {
    onUnauthorized(logout)
  }, [logout])

  // Desloga sozinho quando o token expira, em vez de esperar o proximo 401.
  useEffect(() => {
    if (!sessao) return
    const restante = new Date(sessao.expiraEm).getTime() - Date.now()
    const timer = setTimeout(logout, Math.min(Math.max(restante, 0), MAX_TIMEOUT))
    return () => clearTimeout(timer)
  }, [sessao, logout])

  const login = useCallback(async (email: string, senha: string) => {
    const resultado = await api.post<LoginResultado>('/api/auth/login', { email, senha })
    if (ehDesafioFacial(resultado)) return resultado

    localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(resultado))
    setSessao(resultado)
    return resultado
  }, [])

  // Segundo passo do login, só chamado quando login() devolveu um desafio facial pendente.
  const loginFace = useCallback(async (tokenFacePendente: string, imagemBase64: string) => {
    const resposta = await api.post<LoginResponse>('/api/auth/login/face', { tokenFacePendente, imagemBase64 })
    localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(resposta))
    setSessao(resposta)
    return resposta.usuario
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({ usuario: sessao?.usuario ?? null, login, loginFace, logout }),
    [sessao, login, loginFace, logout],
  )
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth deve ser usado dentro de AuthProvider')
  return ctx
}
