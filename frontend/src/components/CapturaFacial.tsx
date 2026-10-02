import { Alert, Button, Stack, Text } from '@mantine/core'
import { useCallback, useEffect, useRef, useState } from 'react'

interface CapturaFacialProps {
  onCapturar: (imagemBase64: string) => void
  enviando?: boolean
  textoBotao?: string
}

/** Abre a webcam, mostra o feed ao vivo e, ao clicar, captura um frame como JPEG base64
 * (sem o prefixo data URI). Reusado tanto no cadastro quanto no segundo passo do login. */
export function CapturaFacial({ onCapturar, enviando = false, textoBotao = 'Capturar' }: CapturaFacialProps) {
  const videoRef = useRef<HTMLVideoElement>(null)
  const streamRef = useRef<MediaStream | null>(null)
  const [erro, setErro] = useState<string | null>(null)
  const [pronta, setPronta] = useState(false)

  useEffect(() => {
    let cancelado = false

    navigator.mediaDevices
      .getUserMedia({ video: { facingMode: 'user' } })
      .then((stream) => {
        if (cancelado) {
          stream.getTracks().forEach((t) => t.stop())
          return
        }
        streamRef.current = stream
        if (videoRef.current) videoRef.current.srcObject = stream
        setPronta(true)
      })
      .catch(() => setErro('Não foi possível acessar a câmera. Verifique a permissão do navegador.'))

    return () => {
      cancelado = true
      streamRef.current?.getTracks().forEach((t) => t.stop())
    }
  }, [])

  const capturar = useCallback(() => {
    const video = videoRef.current
    if (!video) return

    const canvas = document.createElement('canvas')
    canvas.width = video.videoWidth
    canvas.height = video.videoHeight
    canvas.getContext('2d')?.drawImage(video, 0, 0)

    const dataUrl = canvas.toDataURL('image/jpeg')
    const base64 = dataUrl.slice(dataUrl.indexOf(',') + 1)
    onCapturar(base64)
  }, [onCapturar])

  if (erro) {
    return (
      <Alert color="red" role="alert">
        {erro}
      </Alert>
    )
  }

  return (
    <Stack align="center" gap="sm">
      {/* eslint-disable-next-line jsx-a11y/media-has-caption */}
      <video
        ref={videoRef}
        autoPlay
        playsInline
        muted
        style={{ width: '100%', maxWidth: 360, borderRadius: 8, backgroundColor: '#000' }}
      />
      {!pronta && <Text size="sm" c="dimmed">Abrindo câmera…</Text>}
      <Button onClick={capturar} disabled={!pronta} loading={enviando}>
        {textoBotao}
      </Button>
    </Stack>
  )
}
