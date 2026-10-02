import { Badge, Button, Checkbox, Group, Paper, Stack, Text } from '@mantine/core'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { api } from '../api/client'
import type { VerificacaoFacialStatusResponse } from '../api/types'
import { CapturaFacial } from '../components/CapturaFacial'
import { PageHeader } from '../components/PageHeader'
import { notificarErro, notificarSucesso } from '../lib/format'

export function SegurancaPage() {
  const queryClient = useQueryClient()
  const [consentimento, setConsentimento] = useState(false)

  const status = useQuery({
    queryKey: ['verificacao-facial-status'],
    queryFn: () => api.get<VerificacaoFacialStatusResponse>('/api/auth/face/status'),
  })

  const registrar = useMutation({
    mutationFn: (imagemBase64: string) => api.post('/api/auth/face/registrar', { imagemBase64, consentimento }),
    onSuccess: () => {
      notificarSucesso('Verificação facial ativada.')
      setConsentimento(false)
      void queryClient.invalidateQueries({ queryKey: ['verificacao-facial-status'] })
    },
    onError: notificarErro,
  })

  const desativar = useMutation({
    mutationFn: () => api.put('/api/auth/face/desativar'),
    onSuccess: () => {
      notificarSucesso('Verificação facial desativada.')
      void queryClient.invalidateQueries({ queryKey: ['verificacao-facial-status'] })
    },
    onError: notificarErro,
  })

  const habilitada = status.data?.habilitada ?? false

  return (
    <>
      <PageHeader titulo="Segurança" />
      <Paper withBorder p="lg" radius="md" maw={480}>
        <Stack>
          <Group justify="space-between">
            <Text fw={500}>Verificação facial (segundo fator)</Text>
            <Badge color={habilitada ? 'teal' : 'gray'}>{habilitada ? 'Ativada' : 'Desativada'}</Badge>
          </Group>

          {habilitada ? (
            <>
              <Text size="sm" c="dimmed">
                No próximo login, depois da senha, você vai precisar confirmar seu rosto pela câmera.
              </Text>
              <Button color="red" variant="light" onClick={() => desativar.mutate()} loading={desativar.isPending}>
                Desativar
              </Button>
            </>
          ) : (
            <>
              <Text size="sm" c="dimmed">
                Cadastre uma foto do seu rosto para ativar um segundo fator de autenticação no login.
                Isso usa dado biométrico (sensível pela LGPD) — só usado para confirmar sua identidade no login.
              </Text>
              <Checkbox
                label="Autorizo o uso da minha foto para verificação facial no login"
                checked={consentimento}
                onChange={(e) => setConsentimento(e.currentTarget.checked)}
              />
              {consentimento && (
                <CapturaFacial
                  onCapturar={(imagemBase64) => registrar.mutate(imagemBase64)}
                  enviando={registrar.isPending}
                  textoBotao="Cadastrar rosto"
                />
              )}
            </>
          )}
        </Stack>
      </Paper>
    </>
  )
}
