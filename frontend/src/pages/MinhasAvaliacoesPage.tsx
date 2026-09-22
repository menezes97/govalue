import { Badge, Button, Card, Group, Progress, SimpleGrid, Stack, Text } from '@mantine/core'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { api } from '../api/client'
import type { ResumoAvaliacao } from '../api/types'
import { Consulta } from '../components/Consulta'
import { PageHeader } from '../components/PageHeader'
import { formatarData } from '../lib/format'

export function MinhasAvaliacoesPage() {
  const query = useQuery({
    queryKey: ['minhas-avaliacoes'],
    queryFn: () => api.get<ResumoAvaliacao[]>('/api/minhas-avaliacoes'),
  })

  return (
    <>
      <PageHeader titulo="Minhas avaliações" descricao="Avaliações vinculadas a você. Responda dentro do período de vigência." />
      <Consulta query={query}>
        {(avaliacoes) =>
          avaliacoes.length === 0 ? (
            <Text c="dimmed">Nenhuma avaliação foi vinculada a você ainda.</Text>
          ) : (
            <SimpleGrid cols={{ base: 1, md: 2 }}>
              {avaliacoes.map((a) => (
                <Card key={a.avaliacaoId} withBorder radius="md">
                  <Stack gap="xs">
                    <Group justify="space-between" wrap="nowrap">
                      <Text fw={600}>{a.descricao}</Text>
                      <Badge color={a.aberta ? 'teal' : 'gray'}>{a.aberta ? 'Aberta' : 'Encerrada'}</Badge>
                    </Group>
                    <Text size="sm" c="dimmed">
                      {a.tipo} · até {formatarData(a.dataFimVigencia)}
                    </Text>
                    <Progress
                      value={a.totalPerguntas === 0 ? 0 : (a.respondidas / a.totalPerguntas) * 100}
                      aria-label={`${a.respondidas} de ${a.totalPerguntas} respondidas`}
                    />
                    <Group justify="space-between">
                      <Text size="sm">
                        {a.respondidas} de {a.totalPerguntas} respondidas
                      </Text>
                      <Button component={Link} to={`/minhas-avaliacoes/${a.avaliacaoId}`} size="xs" variant={a.aberta ? 'filled' : 'light'}>
                        {a.aberta ? 'Responder' : 'Ver respostas'}
                      </Button>
                    </Group>
                  </Stack>
                </Card>
              ))}
            </SimpleGrid>
          )
        }
      </Consulta>
    </>
  )
}
