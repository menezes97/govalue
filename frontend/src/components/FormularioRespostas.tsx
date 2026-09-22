import { Alert, Badge, Button, Card, Group, Progress, Radio, Stack, Text } from '@mantine/core'
import { useState } from 'react'
import type { DetalheAvaliacao, ItemResposta } from '../api/types'
import { formatarData } from '../lib/format'

interface Props {
  detalhe: DetalheAvaliacao
  /** FUNCIONARIO: responde a propria avaliacao. GESTOR: responde sobre o subordinado. */
  modo: 'FUNCIONARIO' | 'GESTOR'
  salvando: boolean
  onSalvar: (itens: ItemResposta[]) => void
}

export function FormularioRespostas({ detalhe, modo, salvando, onSalvar }: Props) {
  const doGestor = modo === 'GESTOR'
  const idInicial = (p: DetalheAvaliacao['perguntas'][number]) =>
    (doGestor ? p.respostaGestorId : p.respostaFuncionarioId)?.toString() ?? ''

  const [valores, setValores] = useState<Record<number, string>>(() =>
    Object.fromEntries(detalhe.perguntas.map((p) => [p.perguntaId, idInicial(p)])),
  )

  const total = detalhe.perguntas.length
  const respondidas = Object.values(valores).filter(Boolean).length
  const alterado = detalhe.perguntas.some((p) => valores[p.perguntaId] !== idInicial(p))

  function salvar() {
    const itens = detalhe.perguntas
      .filter((p) => valores[p.perguntaId])
      .map((p) => ({ perguntaId: p.perguntaId, respostaId: Number(valores[p.perguntaId]) }))
    onSalvar(itens)
  }

  return (
    <Stack>
      <Group justify="space-between">
        <Group gap="xs">
          <Badge color={detalhe.aberta ? 'teal' : 'gray'}>{detalhe.aberta ? 'Aberta' : 'Encerrada'}</Badge>
          <Text size="sm" c="dimmed">
            Vigência: {formatarData(detalhe.dataInicioVigencia)} a {formatarData(detalhe.dataFimVigencia)}
          </Text>
        </Group>
        <Text size="sm">
          {respondidas} de {total} respondidas
        </Text>
      </Group>
      <Progress value={total === 0 ? 0 : (respondidas / total) * 100} aria-label="Progresso das respostas" />

      {!detalhe.aberta && (
        <Alert color="gray" title="Avaliação encerrada">
          O período de vigência acabou. As respostas estão disponíveis apenas para consulta.
        </Alert>
      )}

      {detalhe.perguntas.map((pergunta, indice) => {
        const respostaDoFuncionario = pergunta.opcoes.find((o) => o.id === pergunta.respostaFuncionarioId)
        return (
          <Card key={pergunta.perguntaId} withBorder radius="md">
            <Radio.Group
              label={`${indice + 1}. ${pergunta.descricao}`}
              value={valores[pergunta.perguntaId] ?? ''}
              onChange={(v) => setValores((atual) => ({ ...atual, [pergunta.perguntaId]: v }))}
            >
              <Stack gap="xs" mt="sm">
                {pergunta.opcoes.map((opcao) => (
                  <Radio key={opcao.id} value={String(opcao.id)} label={opcao.descricao} disabled={!detalhe.aberta} />
                ))}
              </Stack>
            </Radio.Group>
            {doGestor && (
              <Text size="xs" c="dimmed" mt="sm">
                Resposta de {detalhe.funcionarioNome}: {respostaDoFuncionario?.descricao ?? 'ainda não respondeu'}
              </Text>
            )}
          </Card>
        )
      })}

      {detalhe.aberta && (
        <Group justify="flex-end">
          <Button onClick={salvar} loading={salvando} disabled={!alterado || respondidas === 0}>
            Salvar respostas
          </Button>
        </Group>
      )}
    </Stack>
  )
}
