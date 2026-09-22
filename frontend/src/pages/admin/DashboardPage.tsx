import { BarChart } from '@mantine/charts'
import { Badge, Card, Group, Progress, Select, SimpleGrid, Stack, Table, Text } from '@mantine/core'
import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { api } from '../../api/client'
import type { Avaliacao, Conclusao, ResultadoAvaliacao, Visao } from '../../api/types'
import { Consulta } from '../../components/Consulta'
import { PageHeader } from '../../components/PageHeader'
import { formatarPercentual } from '../../lib/format'

function Estatistica({ rotulo, valor }: { rotulo: string; valor: string }) {
  return (
    <Card withBorder radius="md" padding="md">
      <Text size="xs" c="dimmed" tt="uppercase" fw={600}>
        {rotulo}
      </Text>
      <Text size="xl" fw={700}>
        {valor}
      </Text>
    </Card>
  )
}

function ConclusaoSecao({ avaliacaoId }: { avaliacaoId: number }) {
  const query = useQuery({
    queryKey: ['dashboard-conclusao', avaliacaoId],
    queryFn: () => api.get<Conclusao>(`/api/admin/dashboard/avaliacoes/${avaliacaoId}/conclusao`),
  })

  return (
    <Consulta query={query}>
      {(dados) => (
        <Stack>
          <SimpleGrid cols={{ base: 1, sm: 3 }}>
            <Estatistica rotulo="Funcionários vinculados" valor={String(dados.totalFuncionarios)} />
            <Estatistica rotulo="Concluíram 100%" valor={String(dados.concluidos)} />
            <Estatistica rotulo="Conclusão geral" valor={formatarPercentual(dados.percentualGeral)} />
          </SimpleGrid>

          {dados.funcionarios.length > 0 && (
            <Card withBorder radius="md">
              <Text fw={600} mb="sm">
                Conclusão por funcionário
              </Text>
              <BarChart
                h={Math.max(180, dados.funcionarios.length * 36)}
                data={dados.funcionarios.map((f) => ({ nome: f.nome, Conclusão: f.percentual }))}
                dataKey="nome"
                series={[{ name: 'Conclusão', color: 'teal.6' }]}
                orientation="vertical"
                yAxisProps={{ width: 110 }}
                valueFormatter={(v) => `${v}%`}
                withLegend={false}
              />

              <Table.ScrollContainer minWidth={480} mt="md">
                <Table verticalSpacing="xs">
                  <Table.Thead>
                    <Table.Tr>
                      <Table.Th>Funcionário</Table.Th>
                      <Table.Th>Como funcionário</Table.Th>
                      <Table.Th>Como gestor avaliou</Table.Th>
                    </Table.Tr>
                  </Table.Thead>
                  <Table.Tbody>
                    {dados.funcionarios.map((f) => (
                      <Table.Tr key={f.funcionarioId}>
                        <Table.Td>{f.nome}</Table.Td>
                        <Table.Td w={200}>
                          <Group gap="xs" wrap="nowrap">
                            <Progress value={f.percentual} flex={1} aria-label={`${f.respondidas} de ${f.totalPerguntas}`} />
                            <Text size="xs" w={70}>
                              {f.respondidas}/{f.totalPerguntas}
                            </Text>
                          </Group>
                        </Table.Td>
                        <Table.Td w={200}>
                          <Group gap="xs" wrap="nowrap">
                            <Progress value={f.percentualGestor} flex={1} color="grape" aria-label={`${f.respondidasPeloGestor} de ${f.totalPerguntas}`} />
                            <Text size="xs" w={70}>
                              {f.respondidasPeloGestor}/{f.totalPerguntas}
                            </Text>
                          </Group>
                        </Table.Td>
                      </Table.Tr>
                    ))}
                  </Table.Tbody>
                </Table>
              </Table.ScrollContainer>
            </Card>
          )}
        </Stack>
      )}
    </Consulta>
  )
}

function ResultadosSecao({ avaliacaoId, visao }: { avaliacaoId: number; visao: Visao }) {
  const query = useQuery({
    queryKey: ['dashboard-resultados', avaliacaoId, visao],
    queryFn: () => api.get<ResultadoAvaliacao>(`/api/admin/dashboard/avaliacoes/${avaliacaoId}/resultados?visao=${visao}`),
  })

  return (
    <Consulta query={query}>
      {(dados) =>
        dados.perguntas.length === 0 ? (
          <Text c="dimmed">Esta avaliação ainda não tem perguntas.</Text>
        ) : (
          <Stack>
            {dados.perguntas.map((pergunta, indice) => {
              const dadosGrafico = [
                ...pergunta.opcoes.map((o) => ({ opcao: o.descricao, Respostas: o.percentual })),
                ...(pergunta.semResposta > 0 ? [{ opcao: 'Sem resposta', Respostas: pergunta.semRespostaPercentual }] : []),
              ]
              return (
                <Card key={pergunta.perguntaId} withBorder radius="md">
                  <Group justify="space-between" mb="xs">
                    <Text fw={600}>
                      {indice + 1}. {pergunta.descricao}
                    </Text>
                    <Badge variant="light">{pergunta.respondidas} de {pergunta.totalVinculados} respondidas</Badge>
                  </Group>
                  {pergunta.totalVinculados === 0 ? (
                    <Text c="dimmed" size="sm">
                      Ninguém foi vinculado a esta avaliação ainda.
                    </Text>
                  ) : (
                    <BarChart
                      h={Math.max(140, dadosGrafico.length * 34)}
                      data={dadosGrafico}
                      dataKey="opcao"
                      series={[{ name: 'Respostas', color: 'blue.6' }]}
                      orientation="vertical"
                      yAxisProps={{ width: 160 }}
                      valueFormatter={(v) => `${v}%`}
                      withLegend={false}
                    />
                  )}
                </Card>
              )
            })}
          </Stack>
        )
      }
    </Consulta>
  )
}

export function DashboardPage() {
  const [avaliacaoId, setAvaliacaoId] = useState<string | null>(null)
  const [visao, setVisao] = useState<Visao>('FUNCIONARIO')

  const avaliacoes = useQuery({ queryKey: ['avaliacoes'], queryFn: () => api.get<Avaliacao[]>('/api/admin/avaliacoes') })
  const selecionada = avaliacaoId ?? (avaliacoes.data?.[0] ? String(avaliacoes.data[0].id) : null)

  return (
    <>
      <PageHeader titulo="Dashboard" descricao="Conclusão e distribuição de respostas por avaliação." />

      <Consulta query={avaliacoes}>
        {(lista) =>
          lista.length === 0 ? (
            <Text c="dimmed">Nenhuma avaliação cadastrada ainda.</Text>
          ) : (
            <Stack>
              <Group>
                <Select
                  label="Avaliação"
                  data={lista.map((a) => ({ value: String(a.id), label: a.descricao }))}
                  value={selecionada}
                  onChange={setAvaliacaoId}
                  allowDeselect={false}
                  w={320}
                />
                <Select
                  label="Ver respostas de"
                  data={[
                    { value: 'FUNCIONARIO', label: 'Funcionários' },
                    { value: 'GESTOR', label: 'Gestores' },
                  ]}
                  value={visao}
                  onChange={(v) => setVisao((v as Visao) ?? 'FUNCIONARIO')}
                  allowDeselect={false}
                  w={220}
                />
              </Group>

              {selecionada && (
                <>
                  <ConclusaoSecao avaliacaoId={Number(selecionada)} />
                  <ResultadosSecao avaliacaoId={Number(selecionada)} visao={visao} />
                </>
              )}
            </Stack>
          )
        }
      </Consulta>
    </>
  )
}
