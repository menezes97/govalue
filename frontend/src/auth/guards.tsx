import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from './AuthContext'

/** Exige login; redireciona para /login guardando de onde a pessoa veio. */
export function RequireAuth() {
  const { usuario } = useAuth()
  const location = useLocation()
  if (!usuario) return <Navigate to="/login" replace state={{ from: location.pathname }} />
  return <Outlet />
}

export function RequireAdmin() {
  const { usuario } = useAuth()
  if (usuario?.perfil !== 'ADMIN') return <Navigate to="/" replace />
  return <Outlet />
}

/** "Gestor" nao e um perfil: e quem tem subordinados diretos (flag vinda da API). */
export function RequireGestor() {
  const { usuario } = useAuth()
  if (!usuario?.gestor) return <Navigate to="/" replace />
  return <Outlet />
}

export function HomeRedirect() {
  const { usuario } = useAuth()
  if (!usuario) return <Navigate to="/login" replace />
  return <Navigate to={usuario.perfil === 'ADMIN' ? '/admin/dashboard' : '/minhas-avaliacoes'} replace />
}
