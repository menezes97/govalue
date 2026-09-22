import { Button, Paper, PasswordInput, Stack } from '@mantine/core'
import { useForm } from '@mantine/form'
import { useMutation } from '@tanstack/react-query'
import { api } from '../api/client'
import { PageHeader } from '../components/PageHeader'
import { notificarErro, notificarSucesso } from '../lib/format'

export function SenhaPage() {
  const form = useForm({
    mode: 'uncontrolled',
    initialValues: { senhaAtual: '', novaSenha: '', confirmacao: '' },
    validate: {
      senhaAtual: (v) => (v ? null : 'Informe a senha atual'),
      novaSenha: (v) => (v.length >= 8 && v.length <= 72 ? null : 'A nova senha deve ter de 8 a 72 caracteres'),
      confirmacao: (v, valores) => (v === valores.novaSenha ? null : 'As senhas não conferem'),
    },
  })

  const alterar = useMutation({
    mutationFn: (v: { senhaAtual: string; novaSenha: string }) => api.put('/api/auth/senha', v),
    onSuccess: () => {
      notificarSucesso('Senha alterada.')
      form.reset()
    },
    onError: notificarErro,
  })

  return (
    <>
      <PageHeader titulo="Alterar senha" />
      <Paper withBorder p="lg" radius="md" maw={420}>
        <form onSubmit={form.onSubmit((v) => alterar.mutate({ senhaAtual: v.senhaAtual, novaSenha: v.novaSenha }))}>
          <Stack>
            <PasswordInput label="Senha atual" autoComplete="current-password" key={form.key('senhaAtual')} {...form.getInputProps('senhaAtual')} />
            <PasswordInput label="Nova senha" autoComplete="new-password" key={form.key('novaSenha')} {...form.getInputProps('novaSenha')} />
            <PasswordInput label="Confirmar nova senha" autoComplete="new-password" key={form.key('confirmacao')} {...form.getInputProps('confirmacao')} />
            <Button type="submit" loading={alterar.isPending}>
              Alterar senha
            </Button>
          </Stack>
        </form>
      </Paper>
    </>
  )
}
