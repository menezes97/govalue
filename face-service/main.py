from fastapi import FastAPI
from fastapi.responses import JSONResponse

import face
from schemas import EmbeddingRequest, EmbeddingResponse, VerifyRequest, VerifyResponse

app = FastAPI(title="GoValue Face Service", version="1.0.0")


@app.exception_handler(face.NenhumRostoDetectadoError)
async def nenhum_rosto_handler(request, exc):
    return JSONResponse(status_code=422, content={"detail": "Nenhum rosto detectado na imagem"})


@app.exception_handler(face.MultiplosRostosDetectadosError)
async def multiplos_rostos_handler(request, exc):
    return JSONResponse(status_code=422, content={"detail": "Mais de um rosto detectado na imagem"})


@app.get("/health")
async def health():
    return {"status": "ok"}


@app.post("/embeddings", response_model=EmbeddingResponse)
async def embeddings(request: EmbeddingRequest):
    embedding = face.extrair_embedding(request.imagem_base64)
    return EmbeddingResponse(embedding=embedding)


@app.post("/verify", response_model=VerifyResponse)
async def verify(request: VerifyRequest):
    corresponde, distancia = face.verificar(request.imagem_base64, request.embedding_referencia)
    return VerifyResponse(corresponde=corresponde, distancia=distancia)
