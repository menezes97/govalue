import { notifications } from '@mantine/notifications'
import { ApiError } from '../api/client'

/** "2026-06-15" -> "15/06/2026" (sem passar por Date/UTC, que deslocaria o dia). */
export function formatarData(iso: string): string {
  const [ano, mes, dia] = iso.split('-')
  return `${dia}/${mes}/${ano}`
}

/** "39053344705" -> "390.533.447-05" */
export function formatarCpf(cpf: string): string {
  return cpf.replace(/^(\d{3})(\d{3})(\d{3})(\d{2})$/, '$1.$2.$3-$4')
}

/** 66.7 -> "66,7%" */
export function formatarPercentual(valor: number): string {
  return `${valor.toLocaleString('pt-BR', { minimumFractionDigits: 1, maximumFractionDigits: 1 })}%`
}

export function notificarSucesso(mensagem: string) {
  notifications.show({ color: 'teal', title: 'Pronto', message: mensagem })
}

export function notificarErro(erro: unknown) {
  const mensagem = erro instanceof ApiError ? erro.message : 'Algo deu errado. Tente novamente.'
  notifications.show({ color: 'red', title: 'Não foi possível concluir', message: mensagem })
}
