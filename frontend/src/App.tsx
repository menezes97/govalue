import { Navigate, Route, Routes } from 'react-router-dom'
import { HomeRedirect, RequireAdmin, RequireAuth, RequireGestor } from './auth/guards'
import { AppLayout } from './components/AppLayout'
import { AvaliacaoDetalhePage } from './pages/admin/AvaliacaoDetalhePage'
import { AvaliacoesPage } from './pages/admin/AvaliacoesPage'
import { DashboardPage } from './pages/admin/DashboardPage'
import { FuncionariosPage } from './pages/admin/FuncionariosPage'
import { PadroesPage } from './pages/admin/PadroesPage'
import { GestorPage } from './pages/GestorPage'
import { LoginPage } from './pages/LoginPage'
import { MinhasAvaliacoesPage } from './pages/MinhasAvaliacoesPage'
import { ResponderPage } from './pages/ResponderPage'
import { SegurancaPage } from './pages/SegurancaPage'
import { SenhaPage } from './pages/SenhaPage'

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />

      <Route element={<RequireAuth />}>
        <Route element={<AppLayout />}>
          <Route index element={<HomeRedirect />} />
          <Route path="senha" element={<SenhaPage />} />
          <Route path="seguranca" element={<SegurancaPage />} />

          <Route path="minhas-avaliacoes" element={<MinhasAvaliacoesPage />} />
          <Route path="minhas-avaliacoes/:avaliacaoId" element={<ResponderPage modo="FUNCIONARIO" />} />

          <Route element={<RequireGestor />}>
            <Route path="gestor" element={<GestorPage />} />
            <Route path="gestor/:avaliacaoId/:funcionarioId" element={<ResponderPage modo="GESTOR" />} />
          </Route>

          <Route path="admin" element={<RequireAdmin />}>
            <Route path="dashboard" element={<DashboardPage />} />
            <Route path="funcionarios" element={<FuncionariosPage />} />
            <Route path="avaliacoes" element={<AvaliacoesPage />} />
            <Route path="avaliacoes/:avaliacaoId" element={<AvaliacaoDetalhePage />} />
            <Route path="padroes" element={<PadroesPage />} />
          </Route>
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
