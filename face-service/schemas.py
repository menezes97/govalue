from pydantic import BaseModel


class EmbeddingRequest(BaseModel):
    imagem_base64: str


class EmbeddingResponse(BaseModel):
    embedding: list[float]


class VerifyRequest(BaseModel):
    imagem_base64: str
    embedding_referencia: list[float]


class VerifyResponse(BaseModel):
    corresponde: bool
    distancia: float
