import base64
import io
import os
import sys

import numpy as np
from PIL import Image, ImageOps

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

import face  # noqa: E402


def _foto_com_orientacao_exif(orientacao: int) -> bytes:
    """JPEG com a tag EXIF Orientation setada — os pixels continuam "deitados" como
    gravados pelo sensor, é a tag quem diz como girar na hora de exibir/processar."""
    imagem = Image.new("RGB", (120, 60), color=(200, 50, 50))
    exif = imagem.getexif()
    exif[0x0112] = orientacao  # 0x0112 = tag padrão "Orientation"
    buffer = io.BytesIO()
    imagem.save(buffer, format="JPEG", exif=exif)
    return buffer.getvalue()


def test_decodificar_imagem_aplica_a_rotacao_da_exif():
    """Pegadinha real: fotos de câmera de celular (ao contrário de capturas via canvas no
    navegador) trazem a rotação como metadado EXIF, não nos pixels — sem aplicar essa tag,
    o detector de rosto recebe a imagem de lado/de cabeça pra baixo. Esse teste garante que
    _decodificar_imagem sempre aplica a correção, não que a correção em si está certa
    (isso já é responsabilidade testada da própria Pillow)."""
    bytes_originais = _foto_com_orientacao_exif(orientacao=6)  # 6 = precisa girar 90°
    imagem_base64 = base64.b64encode(bytes_originais).decode("ascii")

    resultado = face._decodificar_imagem(imagem_base64)

    esperado = np.array(ImageOps.exif_transpose(Image.open(io.BytesIO(bytes_originais))).convert("RGB"))
    assert np.array_equal(resultado, esperado)

    # a imagem original é 120x60 (largura x altura); com a orientação 6 aplicada, a Pillow
    # troca width/height — confirma que a rotação de fato mudou a geometria, não só os bytes.
    sem_correcao = np.array(Image.open(io.BytesIO(bytes_originais)).convert("RGB"))
    assert resultado.shape != sem_correcao.shape


def test_decodificar_imagem_sem_exif_fica_inalterada():
    """Imagem sem metadado de orientação (ex.: PNG vindo de canvas no navegador) não deve
    ser afetada pelo exif_transpose — comportamento de antes da correção, preservado."""
    imagem = Image.new("RGB", (120, 60), color=(50, 200, 50))
    buffer = io.BytesIO()
    imagem.save(buffer, format="JPEG")
    imagem_base64 = base64.b64encode(buffer.getvalue()).decode("ascii")

    resultado = face._decodificar_imagem(imagem_base64)

    assert resultado.shape == (60, 120, 3)
