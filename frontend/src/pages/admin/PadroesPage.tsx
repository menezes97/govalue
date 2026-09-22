import { Button, Card, Group, NavLink, SimpleGrid, Stack, Table, Text } from '@mantine/core'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { ApiError, api } from '../../api/client'
import type { PadraoResposta, Resposta } from '../../api/types'
import { ConfirmModal } from '../../components/ConfirmModal'
import { Consulta } from '../../components/Consulta'
import { PageHeader } from '../../components/PageHeader'
import { TextoModal } from '../../components/TextoModal'
import { notificarErro, notificarSucesso } from '../../lib/format'

type Alvo = 'padrao' | 'opcao'
interface Edicao {
  alvo: Alvo
  item: PadraoResposta | Resposta | null
}

export function PadroesPage() {
  const queryClient = useQueryClient()
  const [selecionadoId, setSelecionadoId] = useState<number | null>(null)
  const [edicao, setEdicao] = useState<Edicao | null>(null)
  const [excluindo, setExcluindo] = useState<Edicao | null>(null)
  const [erroCampo, setErroCampo] = useState<string | undefined>()

  const padroes = useQuery({ queryKey: ['padroes'], queryFn: () => api.get<PadraoResposta[]>('/api/admin/padroes-resposta') })
  const selecionado = padroes.data?.find((p) => p.id === selecionadoId) ?? padroes.data?.[0] ?? null

  const opcoes = useQuery({
    queryKey: ['respostas', selecionado?.id],
    queryFn: () => api.get<Resposta[]>(`/api/admin/padroes-resposta/${selecionado?.id}/respostas`),
    enabled: selecionado !== null,
  })

  function fechar() {
    setEdicao(null)
    setErroCampo(undefined)
  }

  const salvar = useMutation({
    mutationFn: ({ alvo, item, texto }: Edicao & { texto: string }) => {
      const corpo = { descricao: texto }
      if (alvo === 'padrao') {
        return item ? api.put(`/api/admin/padroes-resposta/${item.id}`, corpo) : api.post('/api/admin/padroes-resposta', corpo)
      }
      return item
        ? api.put(`/api/admin/respostas/${item.id}`, corpo)
        : api.post(`/api/admin/padroes-resposta/${selecionado?.id}/respostas`, corpo)
    },
    onSuccess: (_dados, vars) => {
      void queryClient.invalidateQueries({ queryKey: vars.alvo === 'padrao' ? ['padroes'] : ['respostas'] })
      notificarSucesso('Salvo.')
      fechar()
    },
    onError: (erro) => {
      if (erro instanceof ApiError && erro.status === 409) setErroCampo('Já existe um item com esta descrição.')
      else if (erro instanceof ApiError && erro.campos.descricao) setErroCampo(erro.campos.descricao)
      else notificarErro(erro)
    },
  })

  const excluir = useMutation({
    mutationFn: ({ alvo, item }: Edicao) =>
      api.delete(alvo === 'padrao' ? `/api/admin/padroes-resposta/${item?.id}` : `/api/admin/respostas/${item?.id}`),
    onSuccess: (_dados, vars) => {
      void queryClient.invalidateQueries({ queryKey: vars.alvo === 'padrao' ? ['padroes'] : ['respostas'] })
      notificarSucesso('Excluído.')
      setExcluindo(null)
    },
    onError: (erro) => {
      if (erro instanceof ApiError && erro.status === 409) {
        notificarErro(new ApiError(409, 'Não é possível excluir: o item está em uso por perguntas ou respostas.'))
      } else {
        notificarErro(erro)
      }
      setExcluindo(null)
    },
  })

  const tituloModal =
    edicao?.alvo === 'padrao'
      ? edicao.item ? 'Editar padrão de resposta' : 'Novo padrão de resposta'
      : edicao?.item ? 'Editar opção' : 'Nova opção'

  return (
    <>
      <PageHeader titulo="Padrões de resposta" descricao="Escalas reutilizáveis (ex.: concordância) e as opções que cada uma oferece." />

      <Consulta query={padroes}>
        {(lista) => (
          <SimpleGrid cols={{ base: 1, md: 2 }} spacing="lg">
            <Card withBorder radius="md">
              <Group justify="space-between" mb="sm">
                <Text fw={600}>Padrões</Text>
                <Button size="xs" onClick={() => setEdicao({ alvo: 'padrao', item: null })}>
                  Novo padrão
                </Button>
              </Group>
              <Stack gap={4}>
                {lista.map((p) => (
                  <Group key={p.id} justify="space-between" wrap="nowrap">
                    <NavLink label={p.descricao} active={p.id === selecionado?.id} onClick={() => setSelecionadoId(p.id)} style={{ flex: 1 }} />
                    <Group gap={4} wrap="nowrap">
                      <Button size="compact-xs" variant="subtle" onClick={() => setEdicao({ alvo: 'padrao', item: p })}>
                        Editar
                      </Button>
                      <Button size="compact-xs" variant="subtle" color="red" onClick={() => setExcluindo({ alvo: 'padrao', item: p })}>
                        Excluir
                      </Button>
                    </Group>
                  </Group>
                ))}
              </Stack>
            </Card>

            <Card withBorder radius="md">
              <Group justify="space-between" mb="sm">
                <Text fw={600}>{selecionado ? `Opções de "${selecionado.descricao}"` : 'Opções'}</Text>
                <Button size="xs" disabled={!selecionado} onClick={() => setEdicao({ alvo: 'opcao', item: null })}>
                  Nova opção
                </Button>
              </Group>
              {selecionado && (
                <Consulta query={opcoes}>
                  {(itens) =>
                    itens.length === 0 ? (
                      <Text c="dimmed" size="sm">
                        Este padrão ainda não tem opções.
                      </Text>
                    ) : (
                      <Table verticalSpacing="xs">
                        <Table.Tbody>
                          {itens.map((o) => (
                            <Table.Tr key={o.id}>
                              <Table.Td>{o.descricao}</Table.Td>
                              <Table.Td>
                                <Group gap={4} justify="flex-end" wrap="nowrap">
                                  <Button size="compact-xs" variant="subtle" onClick={() => setEdicao({ alvo: 'opcao', item: o })}>
                                    Editar
                                  </Button>
                                  <Button size="compact-xs" variant="subtle" color="red" onClick={() => setExcluindo({ alvo: 'opcao', item: o })}>
                                    Excluir
                                  </Button>
                                </Group>
                              </Table.Td>
                            </Table.Tr>
                          ))}
                        </Table.Tbody>
                      </Table>
                    )
                  }
                </Consulta>
              )}
            </Card>
          </SimpleGrid>
        )}
      </Consulta>

      <TextoModal
        aberto={edicao !== null}
        titulo={tituloModal}
        rotulo="Descrição"
        valorInicial={edicao?.item?.descricao ?? ''}
        maxCaracteres={edicao?.alvo === 'padrao' ? 100 : 150}
        salvando={salvar.isPending}
        erro={erroCampo}
        onSalvar={(texto) => edicao && salvar.mutate({ ...edicao, texto })}
        onFechar={fechar}
      />

      <ConfirmModal
        aberto={excluindo !== null}
        titulo="Confirmar exclusão"
        mensagem={`Excluir "${excluindo?.item?.descricao}"? Itens em uso por perguntas ou respostas não podem ser excluídos.`}
        rotuloConfirmar="Excluir"
        perigo
        carregando={excluir.isPending}
        onConfirmar={() => excluindo && excluir.mutate(excluindo)}
        onCancelar={() => setExcluindo(null)}
      />
    </>
  )
}
