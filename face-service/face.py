import base64
import io
import os

import face_recognition
import numpy as np
from PIL import Image

MATCH_THRESHOLD = float(os.environ.get("FACE_MATCH_THRESHOLD", "0.6"))


class NenhumRostoDetectadoError(Exception):
    pass


class MultiplosRostosDetectadosError(Exception):
    pass


def _decodificar_imagem(imagem_base64: str) -> np.ndarray:
    """Decodifica uma string base64 (sem prefixo data URI) para um array RGB em memória.
    Nunca grava em disco — a imagem só existe durante a vida deste request."""
    bytes_imagem = base64.b64decode(imagem_base64)
    imagem = Image.open(io.BytesIO(bytes_imagem)).convert("RGB")
    return np.array(imagem)


def extrair_embedding(imagem_base64: str) -> list[float]:
    imagem = _decodificar_imagem(imagem_base64)
    localizacoes = face_recognition.face_locations(imagem)

    if len(localizacoes) == 0:
        raise NenhumRostoDetectadoError()
    if len(localizacoes) > 1:
        raise MultiplosRostosDetectadosError()

    encodings = face_recognition.face_encodings(imagem, known_face_locations=localizacoes)
    return encodings[0].tolist()


def verificar(imagem_base64: str, embedding_referencia: list[float]) -> tuple[bool, float]:
    embedding_novo = extrair_embedding(imagem_base64)

    referencia = np.array(embedding_referencia)
    novo = np.array(embedding_novo)
    distancia = float(np.linalg.norm(referencia - novo))

    return distancia <= MATCH_THRESHOLD, distancia
