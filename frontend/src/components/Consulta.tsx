import { Alert, Center, Loader } from '@mantine/core'
import type { UseQueryResult } from '@tanstack/react-query'
import type { ReactNode } from 'react'
import { ApiError } from '../api/client'

interface Props<T> {
  query: UseQueryResult<T>
  children: (dados: T) => ReactNode
}

/** Padroniza carregando / erro / conteudo de qualquer consulta. */
export function Consulta<T>({ query, children }: Props<T>) {
  if (query.isPending) {
    return (
      <Center py="xl">
        <Loader aria-label="Carregando" />
      </Center>
    )
  }
  if (query.isError) {
    const mensagem = query.error instanceof ApiError ? query.error.message : 'Não foi possível carregar os dados.'
    return (
      <Alert color="red" title="Erro ao carregar" role="alert">
        {mensagem}
      </Alert>
    )
  }
  return <>{children(query.data)}</>
}
