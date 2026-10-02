import { Alert, Anchor, Button, Center, Container, Paper, PasswordInput, Stack, Text, TextInput, Title } from '@mantine/core'
import { useForm } from '@mantine/form'
import { useState } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { ApiError } from '../api/client'
import { ehDesafioFacial } from '../api/types'
import { CapturaFacial } from '../components/CapturaFacial'
import { useAuth } from '../auth/AuthContext'

const SENHA_DEMO = 'Demo@1234'
const USUARIOS_DEMO = [
  { rotulo: 'Administrador', email: 'admin@govalue.dev' },
  { rotulo: 'Gestora (Marina)', email: 'marina@govalue.dev' },
  { rotulo: 'Funcionário (Carlos)', email: 'carlos@govalue.dev' },
]

export function LoginPage() {
  const { usuario, login, loginFace } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [erro, setErro] = useState<string | null>(null)
  const [carregando, setCarregando] = useState(false)
  const [tokenFacePendente, setTokenFacePendente] = useState<string | null>(null)
  const mostrarDemo = import.meta.env.VITE_SHOW_DEMO_HINT !== 'false'

  const form = useForm({
    mode: 'uncontrolled',
    initialValues: { email: '', senha: '' },
    validate: {
      email: (v) => (/^\S+@\S+\.\S+$/.test(v) ? null : 'Informe um e-mail válido'),
      senha: (v) => (v ? null : 'Informe a senha'),
    },
  })

  if (usuario) return <Navigate to="/" replace />

  const origem = (location.state as { from?: string } | null)?.from

  async function entrar(valores: { email: string; senha: string }) {
    setErro(null)
    setCarregando(true)
    try {
      const resultado = await login(valores.email, valores.senha)
      if (ehDesafioFacial(resultado)) {
        setTokenFacePendente(resultado.tokenFacePendente)
        return
      }
      navigate(origem ?? '/', { replace: true })
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : 'Não foi possível entrar. Tente novamente.')
    } finally {
      setCarregando(false)
    }
  }

  async function verificarRosto(imagemBase64: string) {
    if (!tokenFacePendente) return
    setErro(null)
    setCarregando(true)
    try {
      await loginFace(tokenFacePendente, imagemBase64)
      navigate(origem ?? '/', { replace: true })
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : 'Rosto não reconhecido. Tente novamente.')
    } finally {
      setCarregando(false)
    }
  }

  return (
    <Center mih="100vh">
      <Container size={420} w="100%">
        <Title ta="center">GoValue</Title>
        <Text c="dimmed" size="sm" ta="center" mt={5} mb="lg">
          Gestão de pessoas e avaliações de desempenho
        </Text>

        <Paper withBorder shadow="sm" p="xl" radius="md">
          {tokenFacePendente ? (
            <Stack>
              <Text size="sm" ta="center">
                Confirme sua identidade olhando para a câmera.
              </Text>
              {erro && (
                <Alert color="red" role="alert">
                  {erro}
                </Alert>
              )}
              <CapturaFacial onCapturar={verificarRosto} enviando={carregando} textoBotao="Verificar rosto" />
            </Stack>
          ) : (
            <form onSubmit={form.onSubmit(entrar)}>
              <Stack>
                {erro && (
                  <Alert color="red" role="alert">
                    {erro}
                  </Alert>
                )}
                <TextInput
                  label="E-mail"
                  placeholder="voce@empresa.com"
                  autoComplete="username"
                  key={form.key('email')}
                  {...form.getInputProps('email')}
                />
                <PasswordInput
                  label="Senha"
                  autoComplete="current-password"
                  key={form.key('senha')}
                  {...form.getInputProps('senha')}
                />
                <Button type="submit" loading={carregando} fullWidth>
                  Entrar
                </Button>
              </Stack>
            </form>
          )}
        </Paper>

        {!tokenFacePendente && mostrarDemo && (
          <Paper withBorder p="md" mt="md" radius="md">
            <Text size="sm" fw={500} mb={4}>
              Ambiente de demonstração
            </Text>
            <Text size="xs" c="dimmed" mb="xs">
              Dados fictícios. Clique para preencher (senha: {SENHA_DEMO}).
            </Text>
            <Stack gap={4}>
              {USUARIOS_DEMO.map((u) => (
                <Anchor
                  key={u.email}
                  component="button"
                  type="button"
                  size="sm"
                  ta="left"
                  onClick={() => form.setValues({ email: u.email, senha: SENHA_DEMO })}
                >
                  {u.rotulo}: {u.email}
                </Anchor>
              ))}
            </Stack>
          </Paper>
        )}
      </Container>
    </Center>
  )
}
