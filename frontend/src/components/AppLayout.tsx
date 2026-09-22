import { AppShell, Badge, Burger, Button, Group, Menu, NavLink, Stack, Text, Title } from '@mantine/core'
import { useDisclosure } from '@mantine/hooks'
import { Link, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

interface Item {
  to: string
  label: string
}

export function AppLayout() {
  const { usuario, logout } = useAuth()
  const location = useLocation()
  const [aberto, { toggle, close }] = useDisclosure()

  if (!usuario) return null

  const itens: Item[] =
    usuario.perfil === 'ADMIN'
      ? [
          { to: '/admin/dashboard', label: 'Dashboard' },
          { to: '/admin/funcionarios', label: 'Funcionários' },
          { to: '/admin/avaliacoes', label: 'Avaliações' },
          { to: '/admin/padroes', label: 'Padrões de resposta' },
        ]
      : [
          { to: '/minhas-avaliacoes', label: 'Minhas avaliações' },
          ...(usuario.gestor ? [{ to: '/gestor', label: 'Pesquisas a avaliar' }] : []),
        ]

  return (
    <AppShell
      header={{ height: 60 }}
      navbar={{ width: 260, breakpoint: 'sm', collapsed: { mobile: !aberto } }}
      padding="md"
    >
      <AppShell.Header>
        <Group h="100%" px="md" justify="space-between">
          <Group>
            <Burger opened={aberto} onClick={toggle} hiddenFrom="sm" size="sm" aria-label="Abrir menu" />
            <Title order={3}>GoValue</Title>
          </Group>

          <Menu position="bottom-end" withArrow>
            <Menu.Target>
              <Button variant="subtle" aria-label="Menu do usuário">
                {usuario.nome}
              </Button>
            </Menu.Target>
            <Menu.Dropdown>
              <Menu.Label>{usuario.email}</Menu.Label>
              <Menu.Item component={Link} to="/senha">
                Alterar senha
              </Menu.Item>
              <Menu.Item color="red" onClick={logout}>
                Sair
              </Menu.Item>
            </Menu.Dropdown>
          </Menu>
        </Group>
      </AppShell.Header>

      <AppShell.Navbar p="md">
        <Stack gap="xs">
          <Group gap="xs">
            <Badge variant="light">{usuario.perfil === 'ADMIN' ? 'Administrador' : 'Funcionário'}</Badge>
            {usuario.gestor && <Badge variant="light" color="grape">Gestor</Badge>}
          </Group>
          <Text size="xs" c="dimmed" mb="xs">
            Gestão de pessoas e avaliações
          </Text>
          {itens.map((item) => (
            <NavLink
              key={item.to}
              component={Link}
              to={item.to}
              label={item.label}
              active={location.pathname.startsWith(item.to)}
              onClick={close}
            />
          ))}
        </Stack>
      </AppShell.Navbar>

      <AppShell.Main>
        <Outlet />
      </AppShell.Main>
    </AppShell>
  )
}
