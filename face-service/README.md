# GoValue Face Service

Microsserviço Python (FastAPI + `face_recognition`/dlib) responsável só pela parte de visão computacional da verificação facial em dois fatores do GoValue: extrair um embedding de 128 dimensões de uma foto, e comparar uma nova foto contra um embedding já salvo. Toda a decisão de negócio (quem pode logar, tokens, etc.) fica no backend Java — este serviço não sabe nada sobre usuários, só recebe e devolve imagens/vetores.

## Rodando

Só containerizado — `dlib` é penoso de compilar no Windows, então não há suporte a rodar nativo fora do Docker:

```bash
docker build -t govalue-face-service .
docker run -p 8000:8000 govalue-face-service
```

Docs interativas em `http://localhost:8000/docs`.

## Endpoints

| Método | Rota | Request | Resposta 200 |
|---|---|---|---|
| GET | `/health` | – | `{"status": "ok"}` |
| POST | `/embeddings` | `{"imagem_base64": "..."}` | `{"embedding": [128 floats]}` |
| POST | `/verify` | `{"imagem_base64": "...", "embedding_referencia": [floats]}` | `{"corresponde": bool, "distancia": float}` |

Erros (ambos os endpoints de imagem): `422` se nenhum rosto for detectado, ou mais de um rosto no caso de `/embeddings`.

Limiar de correspondência configurável via env var `FACE_MATCH_THRESHOLD` (default `0.6`).

## Testes

```bash
pip install -r requirements.txt
pytest
```
