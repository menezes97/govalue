import { Anchor } from '@mantine/core'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import { api } from '../api/client'
import type { DetalheAvaliacao, ItemResposta } from '../api/types'
import { Consulta } from '../components/Consulta'
import { FormularioRespostas } from '../components/FormularioRespostas'
import { PageHeader } from '../components/PageHeader'
import { notificarErro, notificarSucesso } from '../lib/format'

interface Props {
  modo: 'FUNCIONARIO' | 'GESTOR'
}

/** Tela de responder uma avaliacao: a propria (funcionario) ou a de um subordinado (gestor). */
export function ResponderPage({ modo }: Props) {
  const { avaliacaoId, funcionarioId } = useParams()
  const queryClient = useQueryClient()
  const doGestor = modo === 'GESTOR'

  const url = doGestor
    ? `/api/gestor/avaliacoes/${avaliacaoId}/funcionarios/${funcionarioId}`
    : `/api/minhas-avaliacoes/${avaliacaoId}`
  const chave = doGestor ? ['gestor-avaliacao', avaliacaoId, funcionarioId] : ['minha-avaliacao', avaliacaoId]
  const listaChave = doGestor ? ['gestor-avaliacoes'] : ['minhas-avaliacoes']
  const voltar = doGestor ? '/gestor' : '/minhas-avaliacoes'

  const query = useQuery({ queryKey: chave, queryFn: () => api.get<DetalheAvaliacao>(url) })

  const salvar = useMutation({
    mutationFn: (respostas: ItemResposta[]) => api.put<DetalheAvaliacao>(`${url}/respostas`, { respostas }),
    onSuccess: (detalhe) => {
      queryClient.setQueryData(chave, detalhe)
      void queryClient.invalidateQueries({ queryKey: listaChave })
      notificarSucesso('Respostas salvas.')
    },
    onError: notificarErro,
  })

  return (
    <>
      <Anchor component={Link} to={voltar} size="sm">
        ← Voltar
      </Anchor>
      <Consulta query={query}>
        {(detalhe) => (
          <>
            <PageHeader
              titulo={detalhe.descricao}
              descricao={doGestor ? `Sua avaliação sobre ${detalhe.funcionarioNome} · ${detalhe.tipo}` : detalhe.tipo}
            />
            {/* key: ao salvar, o formulario reinicia com os dados confirmados pelo servidor */}
            <FormularioRespostas
              key={query.dataUpdatedAt}
              detalhe={detalhe}
              modo={modo}
              salvando={salvar.isPending}
              onSalvar={(itens) => salvar.mutate(itens)}
            />
          </>
        )}
      </Consulta>
    </>
  )
}
