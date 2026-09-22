import { Button, Group, Modal, Stack, Textarea } from '@mantine/core'
import { useForm } from '@mantine/form'
import { useEffect } from 'react'

interface Props {
  aberto: boolean
  titulo: string
  rotulo: string
  valorInicial?: string
  maxCaracteres: number
  salvando?: boolean
  /** Erro devolvido pela API para o campo (ex.: "ja existe"). */
  erro?: string
  onSalvar: (texto: string) => void
  onFechar: () => void
}

/** Modal com um unico campo de texto, usado para padroes, opcoes de resposta e perguntas. */
export function TextoModal({ aberto, titulo, rotulo, valorInicial = '', maxCaracteres, salvando, erro, onSalvar, onFechar }: Props) {
  const form = useForm({
    mode: 'controlled',
    initialValues: { texto: valorInicial },
    validate: {
      texto: (v) => (v.trim() ? (v.length > maxCaracteres ? `Máximo de ${maxCaracteres} caracteres` : null) : 'Campo obrigatório'),
    },
  })

  // Reinicia o campo toda vez que o modal abre (novo ou edicao de outro item).
  const { setValues, resetDirty } = form
  useEffect(() => {
    if (aberto) {
      setValues({ texto: valorInicial })
      resetDirty({ texto: valorInicial })
    }
  }, [aberto, valorInicial, setValues, resetDirty])

  return (
    <Modal opened={aberto} onClose={onFechar} title={titulo} centered>
      <form onSubmit={form.onSubmit((v) => onSalvar(v.texto.trim()))}>
        <Stack>
          <Textarea label={rotulo} autosize minRows={2} data-autofocus error={erro} {...form.getInputProps('texto')} />
          <Group justify="flex-end">
            <Button variant="default" onClick={onFechar}>
              Cancelar
            </Button>
            <Button type="submit" loading={salvando}>
              Salvar
            </Button>
          </Group>
        </Stack>
      </form>
    </Modal>
  )
}
