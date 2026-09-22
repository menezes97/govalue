import { Button, Group, Modal, Text } from '@mantine/core'

interface Props {
  aberto: boolean
  titulo: string
  mensagem: string
  rotuloConfirmar?: string
  perigo?: boolean
  carregando?: boolean
  onConfirmar: () => void
  onCancelar: () => void
}

export function ConfirmModal({
  aberto,
  titulo,
  mensagem,
  rotuloConfirmar = 'Confirmar',
  perigo = false,
  carregando = false,
  onConfirmar,
  onCancelar,
}: Props) {
  return (
    <Modal opened={aberto} onClose={onCancelar} title={titulo} centered>
      <Text size="sm">{mensagem}</Text>
      <Group justify="flex-end" mt="lg">
        <Button variant="default" onClick={onCancelar}>
          Cancelar
        </Button>
        <Button color={perigo ? 'red' : undefined} loading={carregando} onClick={onConfirmar}>
          {rotuloConfirmar}
        </Button>
      </Group>
    </Modal>
  )
}
