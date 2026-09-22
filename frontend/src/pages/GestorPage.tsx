import { Badge, Button, Progress, Table, Text } from '@mantine/core'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { api } from '../api/client'
import type { PendenciaGestor } from '../api/types'
import { Consulta } from '../components/Consulta'
import { PageHeader } from '../components/PageHeader'
import { formatarData } from '../lib/format'

export function GestorPage() {
  const query = useQuery({
    queryKey: ['gestor-avaliacoes'],
    queryFn: () => api.get<PendenciaGestor[]>('/api/gestor/avaliacoes'),
  })

  return (
    <>
      <PageHeader titulo="Pesquisas a avaliar" descricao="Avaliações dos seus subordinados diretos que você pode responder." />
      <Consulta query={query}>
        {(pendencias) =>
          pendencias.length === 0 ? (
            <Text c="dimmed">Nenhuma avaliação dos seus subordinados foi vinculada ainda.</Text>
          ) : (
            <Table.ScrollContainer minWidth={640}>
              <Table verticalSpacing="sm" highlightOnHover>
                <Table.Thead>
                  <Table.Tr>
                    <Table.Th>Funcionário</Table.Th>
                    <Table.Th>Avaliação</Table.Th>
                    <Table.Th>Fim da vigência</Table.Th>
                    <Table.Th>Sua resposta</Table.Th>
                    <Table.Th />
                  </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                  {pendencias.map((p) => (
                    <Table.Tr key={`${p.funcionarioId}-${p.avaliacaoId}`}>
                      <Table.Td>{p.funcionarioNome}</Table.Td>
                      <Table.Td>{p.descricao}</Table.Td>
                      <Table.Td>
                        {formatarData(p.dataFimVigencia)}{' '}
                        <Badge size="xs" color={p.aberta ? 'teal' : 'gray'}>
                          {p.aberta ? 'Aberta' : 'Encerrada'}
                        </Badge>
                      </Table.Td>
                      <Table.Td w={180}>
                        <Progress
                          value={(p.respondidasPeloGestor / p.totalPerguntas) * 100}
                          aria-label={`${p.respondidasPeloGestor} de ${p.totalPerguntas}`}
                        />
                        <Text size="xs" c="dimmed">
                          {p.respondidasPeloGestor} de {p.totalPerguntas}
                        </Text>
                      </Table.Td>
                      <Table.Td>
                        <Button
                          component={Link}
                          to={`/gestor/${p.avaliacaoId}/${p.funcionarioId}`}
                          size="xs"
                          variant={p.aberta ? 'filled' : 'light'}
                        >
                          {p.aberta ? 'Avaliar' : 'Ver'}
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
    </>
  )
}
