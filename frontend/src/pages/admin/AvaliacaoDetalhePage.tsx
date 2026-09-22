import { Alert, Anchor, Button, Card, Group, Select, Stack, Table, Text, Textarea, Tooltip } from '@mantine/core'
import { useForm } from '@mantine/form'
import { Modal } from '@mantine/core'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ApiError, api } from '../../api/client'
import type { Avaliacao, PadraoResposta, Pergunta, PerguntaRequest, VinculoResultado } from '../../api/types'
import { ConfirmModal } from '../../components/ConfirmModal'
import { Consulta } from '../../components/Consulta'
import { PageHeader } from '../../components/PageHeader'
import { formatarData, notificarErro, notificarSucesso } from '../../lib/format'

interface PerguntaFormProps {
  avaliacaoId: string
  editando: Pergunta | null
  padroes: PadraoResposta[]
  onConcluir: () => void
}

function PerguntaForm({ avaliacaoId, editando, padroes, onConcluir }: PerguntaFormProps) {
  const queryClient = useQueryClient()
  const form = useForm({
    mode: 'controlled',
    initialValues: {
      descricao: editando?.descricao ?? '',
      padraoRespostaId: editando ? String(editando.padraoRespostaId) : (padroes[0] ? String(padroes[0].id) : null),
    },
    validate: {
      descricao: (v) => (v.trim() ? (v.length > 500 ? 'Máximo de 500 caracteres' : null) : 'Informe a pergunta'),
      padraoRespostaId: (v) => (v ? null : 'Selecione o padrão de resposta'),
    },
  })

  const salvar = useMutation({
    mutationFn: (v: { descricao: string; padraoRespostaId: string | null }) => {
      const corpo: PerguntaRequest = { descricao: v.descricao.trim(), padraoRespostaId: Number(v.padraoRespostaId) }
      return editando
        ? api.put<Pergunta>(`/api/admin/perguntas/${editando.id}`, corpo)
        : api.post<Pergunta>(`/api/admin/avaliacoes/${avaliacaoId}/perguntas`, corpo)
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['perguntas', avaliacaoId] })
      notificarSucesso(editando ? 'Pergunta atualizada.' : 'Pergunta adicionada.')
      onConcluir()
    },
    onError: (erro) => {
      if (erro instanceof ApiError && Object.keys(erro.campos).length > 0) form.setErrors(erro.campos)
      else notificarErro(erro)
    },
  })

  return (
    <form onSubmit={form.onSubmit((v) => salvar.mutate(v))}>
      <Stack>
        <Textarea label="Pergunta" autosize minRows={2} required data-autofocus {...form.getInputProps('descricao')} />
        <Select
          label="Padrão de resposta"
          required
          data={padroes.map((p) => ({ value: String(p.id), label: p.descricao }))}
          allowDeselect={false}
          {...form.getInputProps('padraoRespostaId')}
        />
        <Group justify="flex-end">
          <Button variant="default" onClick={onConcluir}>
            Cancelar
          </Button>
          <Button type="submit" loading={salvar.isPending}>
            Salvar
          </Button>
        </Group>
      </Stack>
    </form>
  )
}

export function AvaliacaoDetalhePage() {
  const { avaliacaoId = '' } = useParams()
  const queryClient = useQueryClient()
  const [modal, setModal] = useState<Pergunta | null | undefined>(undefined)
  const [excluindo, setExcluindo] = useState<Pergunta | null>(null)
  const [confirmandoVinculo, setConfirmandoVinculo] = useState(false)
  const [ultimoVinculo, setUltimoVinculo] = useState<VinculoResultado | null>(null)

  const avaliacao = useQuery({ queryKey: ['avaliacao', avaliacaoId], queryFn: () => api.get<Avaliacao>(`/api/admin/avaliacoes/${avaliacaoId}`) })
  const perguntas = useQuery({ queryKey: ['perguntas', avaliacaoId], queryFn: () => api.get<Pergunta[]>(`/api/admin/avaliacoes/${avaliacaoId}/perguntas`) })
  const padroes = useQuery({ queryKey: ['padroes'], queryFn: () => api.get<PadraoResposta[]>('/api/admin/padroes-resposta') })

  const excluir = useMutation({
    mutationFn: (id: number) => api.delete(`/api/admin/perguntas/${id}`),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['perguntas', avaliacaoId] })
      notificarSucesso('Pergunta excluída.')
      setExcluindo(null)
    },
    onError: (erro) => {
      notificarErro(erro)
      setExcluindo(null)
    },
  })

  const vincular = useMutation({
    mutationFn: () => api.post<VinculoResultado>(`/api/admin/avaliacoes/${avaliacaoId}/vincular`),
    onSuccess: (resultado) => {
      setUltimoVinculo(resultado)
      setConfirmandoVinculo(false)
      notificarSucesso(`${resultado.funcionariosVinculados} funcionário(s) vinculado(s).`)
    },
    onError: (erro) => {
      notificarErro(erro)
      setConfirmandoVinculo(false)
    },
  })

  return (
    <>
      <Anchor component={Link} to="/admin/avaliacoes" size="sm">
        ← Avaliações
      </Anchor>

      <Consulta query={avaliacao}>
        {(a) => {
          const semPerguntas = (perguntas.data?.length ?? 0) === 0
          const botao = (
            <Button onClick={() => setConfirmandoVinculo(true)} disabled={semPerguntas}>
              Vincular a todos os funcionários ativos
            </Button>
          )
          return (
            <PageHeader
              titulo={a.descricao}
              descricao={`${a.tipoAvaliacao} · vigência de ${formatarData(a.dataInicioVigencia)} a ${formatarData(a.dataFimVigencia)}`}
              acoes={
                semPerguntas ? (
                  <Tooltip label="Cadastre ao menos uma pergunta antes de vincular">
                    <span>{botao}</span>
                  </Tooltip>
                ) : (
                  botao
                )
              }
            />
          )
        }}
      </Consulta>

      {ultimoVinculo && (
        <Alert color="teal" title="Vínculo concluído" mb="md" withCloseButton onClose={() => setUltimoVinculo(null)}>
          {ultimoVinculo.funcionariosVinculados} funcionário(s) receberam a avaliação ({ultimoVinculo.totalPerguntas}{' '}
          {ultimoVinculo.totalPerguntas === 1 ? 'pergunta' : 'perguntas'} cada).{' '}
          {ultimoVinculo.funcionariosJaVinculados > 0 && `${ultimoVinculo.funcionariosJaVinculados} já a possuíam e foram mantidos sem alteração.`}
        </Alert>
      )}

      <Card withBorder radius="md">
        <Group justify="space-between" mb="sm">
          <Text fw={600}>Perguntas</Text>
          <Button size="xs" onClick={() => setModal(null)} disabled={!padroes.data?.length}>
            Nova pergunta
          </Button>
        </Group>

        <Consulta query={perguntas}>
          {(lista) =>
            lista.length === 0 ? (
              <Text c="dimmed" size="sm">
                Nenhuma pergunta cadastrada. Adicione ao menos uma antes de vincular a avaliação.
              </Text>
            ) : (
              <Table.ScrollContainer minWidth={560}>
                <Table verticalSpacing="sm">
                  <Table.Thead>
                    <Table.Tr>
                      <Table.Th w={40}>#</Table.Th>
                      <Table.Th>Pergunta</Table.Th>
                      <Table.Th>Padrão de resposta</Table.Th>
                      <Table.Th />
                    </Table.Tr>
                  </Table.Thead>
                  <Table.Tbody>
                    {lista.map((p, i) => (
                      <Table.Tr key={p.id}>
                        <Table.Td>{i + 1}</Table.Td>
                        <Table.Td>{p.descricao}</Table.Td>
                        <Table.Td>{p.padraoResposta}</Table.Td>
                        <Table.Td>
                          <Group gap="xs" justify="flex-end" wrap="nowrap">
                            <Button size="xs" variant="light" onClick={() => setModal(p)}>
                              Editar
                            </Button>
                            <Button size="xs" variant="light" color="red" onClick={() => setExcluindo(p)}>
                              Excluir
                            </Button>
                          </Group>
                        </Table.Td>
                      </Table.Tr>
                    ))}
                  </Table.Tbody>
                </Table>
              </Table.ScrollContainer>
            )
          }
        </Consulta>
      </Card>

      <Modal opened={modal !== undefined} onClose={() => setModal(undefined)} title={modal === null ? 'Nova pergunta' : 'Editar pergunta'} centered>
        {modal !== undefined && (
          <PerguntaForm key={modal?.id ?? 'nova'} avaliacaoId={avaliacaoId} editando={modal} padroes={padroes.data ?? []} onConcluir={() => setModal(undefined)} />
        )}
      </Modal>

      <ConfirmModal
        aberto={excluindo !== null}
        titulo="Excluir pergunta"
        mensagem={`Excluir a pergunta "${excluindo?.descricao}"? Perguntas já vinculadas a funcionários não podem ser excluídas.`}
        rotuloConfirmar="Excluir"
        perigo
        carregando={excluir.isPending}
        onConfirmar={() => excluindo && excluir.mutate(excluindo.id)}
        onCancelar={() => setExcluindo(null)}
      />

      <ConfirmModal
        aberto={confirmandoVinculo}
        titulo="Vincular avaliação"
        mensagem="Todos os funcionários ativos receberão esta avaliação. Quem já a possui não é duplicado."
        rotuloConfirmar="Vincular"
        carregando={vincular.isPending}
        onConfirmar={() => vincular.mutate()}
        onCancelar={() => setConfirmandoVinculo(false)}
      />
    </>
  )
}
