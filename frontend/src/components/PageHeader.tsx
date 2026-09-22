import { Group, Stack, Text, Title } from '@mantine/core'
import type { ReactNode } from 'react'

interface Props {
  titulo: string
  descricao?: string
  acoes?: ReactNode
}

export function PageHeader({ titulo, descricao, acoes }: Props) {
  return (
    <Group justify="space-between" align="flex-start" mb="lg" wrap="wrap">
      <Stack gap={2}>
        <Title order={2}>{titulo}</Title>
        {descricao && (
          <Text c="dimmed" size="sm">
            {descricao}
          </Text>
        )}
      </Stack>
      {acoes}
    </Group>
  )
}
