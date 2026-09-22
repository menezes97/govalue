import { Badge, Button, Group, Modal, Select, SimpleGrid, Stack, Table, Text, TextInput } from '@mantine/core'
import { useForm } from '@mantine/form'
import { useDebouncedValue } from '@mantine/hooks'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { ApiError, api } from '../../api/client'
import type { AtualizarFuncionario, CriarFuncionario, Funcionario, StatusFuncionario } from '../../api/types'
import { Consulta } from '../../components/Consulta'
import { PageHeader } from '../../components/PageHeader'
import { formatarCpf, notificarErro, notificarSucesso } from '../../lib/format'

interface Valores {
  nome: string
  cpf: string
  email: string
  senhaInicial: string
  funcao: string
  matricula: string
  area: string
  gestorId: string | null
  status: StatusFuncionario
}

function valoresIniciais(f: Funcionario | null): Valores {
  return {
    nome: f?.nome ?? '',
    cpf: f ? formatarCpf(f.cpf) : '',
    email: f?.email ?? '',
    senhaInicial: '',
    funcao: f?.funcao ?? '',
    matricula: f?.matricula ?? '',
    area: f?.area ?? '',
    gestorId: f?.gestorId ? String(f.gestorId) : null,
    status: f?.status ?? 'ATIVO',
  }
}

interface FormProps {
  editando: Funcionario | null
  todos: Funcionario[]
  onConcluir: () => void
}

function FuncionarioForm({ editando, todos, onConcluir }: FormProps) {
  const queryClient = useQueryClient()
  const criando = editando === null

  const form = useForm<Valores>({
    mode: 'controlled',
    initialValues: valoresIniciais(editando),
    validate: {
      nome: (v) => (v.trim() ? null : 'Informe o nome'),
      cpf: (v) => (v.replace(/\D/g, '').length === 11 ? null : 'Informe um CPF com 11 dígitos'),
      email: (v) => (/^\S+@\S+\.\S+$/.test(v) ? null : 'Informe um e-mail válido'),
      senhaInicial: (v) => (criando && (v.length < 8 || v.length > 72) ? 'A senha deve ter de 8 a 72 caracteres' : null),
    },
  })

  const salvar = useMutation({
    mutationFn: (v: Valores) => {
      const gestorId = v.gestorId ? Number(v.gestorId) : null
      const base = { nome: v.nome.trim(), cpf: v.cpf, email: v.email.trim(), funcao: v.funcao, matricula: v.matricula, area: v.area, gestorId }
      return criando
        ? api.post<Funcionario>('/api/admin/funcionarios', { ...base, senhaInicial: v.senhaInicial } satisfies CriarFuncionario)
        : api.put<Funcionario>(`/api/admin/funcionarios/${editando.id}`, { ...base, status: v.status } satisfies AtualizarFuncionario)
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['funcionarios'] })
      notificarSucesso(criando ? 'Funcionário cadastrado.' : 'Funcionário atualizado.')
      onConcluir()
    },
    onError: (erro) => {
      // Erros de validacao do servidor voltam por campo; mostra ao lado do input certo.
      if (erro instanceof ApiError && Object.keys(erro.campos).length > 0) form.setErrors(erro.campos)
      else notificarErro(erro)
    },
  })

  const opcoesGestor = todos
    .filter((f) => f.status === 'ATIVO' && f.id !== editando?.id)
    .map((f) => ({ value: String(f.id), label: f.nome }))

  return (
    <form onSubmit={form.onSubmit((v) => salvar.mutate(v))}>
      <Stack>
        <TextInput label="Nome" required {...form.getInputProps('nome')} />
        <SimpleGrid cols={{ base: 1, sm: 2 }}>
          <TextInput label="CPF" placeholder="000.000.000-00" required {...form.getInputProps('cpf')} />
          <TextInput label="E-mail" required {...form.getInputProps('email')} />
        </SimpleGrid>
        {criando && <TextInput label="Senha inicial" type="password" required autoComplete="new-password" {...form.getInputProps('senhaInicial')} />}
        <SimpleGrid cols={{ base: 1, sm: 3 }}>
          <TextInput label="Função" {...form.getInputProps('funcao')} />
          <TextInput label="Matrícula" {...form.getInputProps('matricula')} />
          <TextInput label="Área" {...form.getInputProps('area')} />
        </SimpleGrid>
        <SimpleGrid cols={{ base: 1, sm: 2 }}>
          <Select
            label="Gestor direto"
            placeholder="Sem gestor"
            data={opcoesGestor}
            searchable
            clearable
            {...form.getInputProps('gestorId')}
          />
          {!criando && (
            <Select
              label="Situação"
              data={[
                { value: 'ATIVO', label: 'Ativo' },
                { value: 'INATIVO', label: 'Inativo' },
              ]}
              allowDeselect={false}
              {...form.getInputProps('status')}
            />
          )}
        </SimpleGrid>
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

export function FuncionariosPage() {
  const [busca, setBusca] = useState('')
  const [buscaAtrasada] = useDebouncedValue(busca, 300)
  // undefined = modal fechado, null = novo, Funcionario = edicao
  const [modal, setModal] = useState<Funcionario | null | undefined>(undefined)

  const query = useQuery({
    queryKey: ['funcionarios', buscaAtrasada],
    queryFn: () => api.get<Funcionario[]>(`/api/admin/funcionarios?busca=${encodeURIComponent(buscaAtrasada)}`),
  })
  // Lista completa (sem filtro) para alimentar o seletor de gestor.
  const todos = useQuery({
    queryKey: ['funcionarios', ''],
    queryFn: () => api.get<Funcionario[]>('/api/admin/funcionarios'),
  })

  return (
    <>
      <PageHeader
        titulo="Funcionários"
        descricao="Cadastro de funcionários e seus acessos. O CPF é validado pelos dígitos verificadores."
        acoes={<Button onClick={() => setModal(null)}>Novo funcionário</Button>}
      />

      <TextInput
        placeholder="Buscar por nome"
        value={busca}
        onChange={(e) => setBusca(e.currentTarget.value)}
        mb="md"
        maw={360}
        aria-label="Buscar funcionário por nome"
      />

      <Consulta query={query}>
        {(funcionarios) =>
          funcionarios.length === 0 ? (
            <Text c="dimmed">Nenhum funcionário encontrado.</Text>
          ) : (
            <Table.ScrollContainer minWidth={760}>
              <Table verticalSpacing="sm" highlightOnHover>
                <Table.Thead>
                  <Table.Tr>
                    <Table.Th>Nome</Table.Th>
                    <Table.Th>CPF</Table.Th>
                    <Table.Th>E-mail</Table.Th>
                    <Table.Th>Função / Área</Table.Th>
                    <Table.Th>Gestor</Table.Th>
                    <Table.Th>Situação</Table.Th>
                    <Table.Th />
                  </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                  {funcionarios.map((f) => (
                    <Table.Tr key={f.id}>
                      <Table.Td>{f.nome}</Table.Td>
                      <Table.Td>{formatarCpf(f.cpf)}</Table.Td>
                      <Table.Td>{f.email}</Table.Td>
                      <Table.Td>{[f.funcao, f.area].filter(Boolean).join(' · ') || '—'}</Table.Td>
                      <Table.Td>{f.gestorNome ?? '—'}</Table.Td>
                      <Table.Td>
                        <Badge color={f.status === 'ATIVO' ? 'teal' : 'gray'}>{f.status === 'ATIVO' ? 'Ativo' : 'Inativo'}</Badge>
                      </Table.Td>
                      <Table.Td>
                        <Button size="xs" variant="light" onClick={() => setModal(f)}>
                          Editar
                        </Button>
                      </Table.Td>
                    </Table.Tr>
                  ))}
                </Table.Tbody>
              </Table>
            </Table.ScrollContainer>
          )
        }
      </Consulta>

      <Modal
        opened={modal !== undefined}
        onClose={() => setModal(undefined)}
        title={modal === null ? 'Novo funcionário' : 'Editar funcionário'}
        size="lg"
        centered
      >
        {modal !== undefined && (
          <FuncionarioForm
            key={modal?.id ?? 'novo'}
            editando={modal}
            todos={todos.data ?? []}
            onConcluir={() => setModal(undefined)}
          />
        )}
      </Modal>
    </>
  )
}
