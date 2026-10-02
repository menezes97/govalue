import base64
import io
import os
import sys

import numpy as np
from fastapi.testclient import TestClient
from PIL import Image

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

from main import app  # noqa: E402

client = TestClient(app)


def _imagem_em_branco_base64() -> str:
    """Imagem sólida sem nenhum rosto — usada só pra testar o caminho de erro."""
    imagem = Image.new("RGB", (200, 200), color=(120, 120, 120))
    buffer = io.BytesIO()
    imagem.save(buffer, format="JPEG")
    return base64.b64encode(buffer.getvalue()).decode("ascii")


def test_health():
    resposta = client.get("/health")
    assert resposta.status_code == 200
    assert resposta.json() == {"status": "ok"}


def test_embeddings_sem_rosto_retorna_422():
    resposta = client.post("/embeddings", json={"imagem_base64": _imagem_em_branco_base64()})
    assert resposta.status_code == 422
    assert "rosto" in resposta.json()["detail"].lower()


def test_verify_sem_rosto_na_foto_enviada_retorna_422():
    embedding_fake = np.zeros(128).tolist()
    resposta = client.post(
        "/verify",
        json={"imagem_base64": _imagem_em_branco_base64(), "embedding_referencia": embedding_fake},
    )
    assert resposta.status_code == 422
