import { Button, Group, Modal, Select, Stack, Table, Text, TextInput } from '@mantine/core'
import { useForm } from '@mantine/form'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { ApiError, api } from '../../api/client'
import type { Avaliacao, AvaliacaoRequest, TipoAvaliacao } from '../../api/types'
import { ConfirmModal } from '../../components/ConfirmModal'
import { Consulta } from '../../components/Consulta'
import { PageHeader } from '../../components/PageHeader'
import { formatarData, notificarErro, notificarSucesso } from '../../lib/format'

interface Valores {
  descricao: string
  tipoAvaliacaoId: string | null
  dataInicioVigencia: string
  dataFimVigencia: string
}

interface FormProps {
  editando: Avaliacao | null
  tipos: TipoAvaliacao[]
  onConcluir: () => void
}

export function AvaliacaoForm({ editando, tipos, onConcluir }: FormProps) {
  const queryClient = useQueryClient()
  const form = useForm<Valores>({
    mode: 'controlled',
    initialValues: {
      descricao: editando?.descricao ?? '',
      tipoAvaliacaoId: editando ? String(editando.tipoAvaliacaoId) : null,
      dataInicioVigencia: editando?.dataInicioVigencia ?? '',
      dataFimVigencia: editando?.dataFimVigencia ?? '',
    },
    validate: {
      descricao: (v) => (v.trim() ? null : 'Informe a descrição'),
      tipoAvaliacaoId: (v) => (v ? null : 'Selecione o tipo'),
      dataInicioVigencia: (v) => (v ? null : 'Informe o início da vigência'),
      dataFimVigencia: (v, valores) =>
        !v ? 'Informe o fim da vigência' : valores.dataInicioVigencia && v < valores.dataInicioVigencia ? 'O fim não pode ser anterior ao início' : null,
    },
  })

  const salvar = useMutation({
    mutationFn: (v: Valores) => {
      const corpo: AvaliacaoRequest = {
        descricao: v.descricao.trim(),
        tipoAvaliacaoId: Number(v.tipoAvaliacaoId),
        dataInicioVigencia: v.dataInicioVigencia,
        dataFimVigencia: v.dataFimVigencia,
      }
      return editando ? api.put<Avaliacao>(`/api/admin/avaliacoes/${editando.id}`, corpo) : api.post<Avaliacao>('/api/admin/avaliacoes', corpo)
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['avaliacoes'] })
      notificarSucesso(editando ? 'Avaliação atualizada.' : 'Avaliação criada.')
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
        <TextInput label="Descrição" required {...form.getInputProps('descricao')} />
        <Select
          label="Tipo"
          required
          data={tipos.map((t) => ({ value: String(t.id), label: t.descricao }))}
          allowDeselect={false}
          {...form.getInputProps('tipoAvaliacaoId')}
        />
        <Group grow align="flex-start">
          <TextInput label="Início da vigência" type="date" required {...form.getInputProps('dataInicioVigencia')} />
          <TextInput label="Fim da vigência" type="date" required {...form.getInputProps('dataFimVigencia')} />
        </Group>
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

export function AvaliacoesPage() {
  const queryClient = useQueryClient()
  const [modal, setModal] = useState<Avaliacao | null | undefined>(undefined)
  const [excluindo, setExcluindo] = useState<Avaliacao | null>(null)

  const query = useQuery({ queryKey: ['avaliacoes'], queryFn: () => api.get<Avaliacao[]>('/api/admin/avaliacoes') })
  const tipos = useQuery({ queryKey: ['tipos-avaliacao'], queryFn: () => api.get<TipoAvaliacao[]>('/api/admin/tipos-avaliacao') })

  const excluir = useMutation({
    mutationFn: (id: number) => api.delete(`/api/admin/avaliacoes/${id}`),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['avaliacoes'] })
      notificarSucesso('Avaliação excluída.')
      setExcluindo(null)
    },
    onError: (erro) => {
      notificarErro(erro)
      setExcluindo(null)
    },
  })

  return (
    <>
      <PageHeader
        titulo="Avaliações"
        descricao="Crie avaliações e pesquisas, cadastre as perguntas e vincule aos funcionários."
        acoes={<Button onClick={() => setModal(null)}>Nova avaliação</Button>}
      />

      <Consulta query={query}>
        {(avaliacoes) =>
          avaliacoes.length === 0 ? (
            <Text c="dimmed">Nenhuma avaliação cadastrada.</Text>
          ) : (
            <Table.ScrollContainer minWidth={680}>
              <Table verticalSpacing="sm" highlightOnHover>
                <Table.Thead>
                  <Table.Tr>
                    <Table.Th>Descrição</Table.Th>
                    <Table.Th>Tipo</Table.Th>
                    <Table.Th>Vigência</Table.Th>
                    <Table.Th />
                  </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                  {avaliacoes.map((a) => (
                    <Table.Tr key={a.id}>
                      <Table.Td>{a.descricao}</Table.Td>
                      <Table.Td>{a.tipoAvaliacao}</Table.Td>
                      <Table.Td>
                        {formatarData(a.dataInicioVigencia)} a {formatarData(a.dataFimVigencia)}
                      </Table.Td>
                      <Table.Td>
                        <Group gap="xs" justify="flex-end" wrap="nowrap">
                          <Button component={Link} to={`/admin/avaliacoes/${a.id}`} size="xs">
                            Perguntas e vínculos
                          </Button>
                          <Button size="xs" variant="light" onClick={() => setModal(a)}>
                            Editar
                          </Button>
                          <Button size="xs" variant="light" color="red" onClick={() => setExcluindo(a)}>
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

      <Modal opened={modal !== undefined} onClose={() => setModal(undefined)} title={modal === null ? 'Nova avaliação' : 'Editar avaliação'} centered>
        {modal !== undefined && <AvaliacaoForm key={modal?.id ?? 'nova'} editando={modal} tipos={tipos.data ?? []} onConcluir={() => setModal(undefined)} />}
      </Modal>

      <ConfirmModal
        aberto={excluindo !== null}
        titulo="Excluir avaliação"
        mensagem={`Excluir "${excluindo?.descricao}" e todas as suas perguntas? Avaliações já vinculadas a funcionários não podem ser excluídas.`}
        rotuloConfirmar="Excluir"
        perigo
        carregando={excluir.isPending}
        onConfirmar={() => excluindo && excluir.mutate(excluindo.id)}
        onCancelar={() => setExcluindo(null)}
      />
    </>
  )
}
