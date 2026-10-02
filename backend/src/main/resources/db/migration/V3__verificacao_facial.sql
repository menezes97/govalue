-- Segundo fator opcional (2FA) por reconhecimento facial. O embedding e um array JSON
-- de floats, opaco para o Java: nunca e usado em query, so repassado ao microsservico
-- Python que faz a comparacao. Nenhuma foto crua e persistida em nenhum momento.
ALTER TABLE usuario
    ADD COLUMN verificacao_facial_habilitada        BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN verificacao_facial_embedding         TEXT,
    ADD COLUMN verificacao_facial_consentimento_em  TIMESTAMPTZ;

-- Garante que a flag e o embedding ficam sempre coerentes, sem depender de disciplina no service.
ALTER TABLE usuario
    ADD CONSTRAINT ck_usuario_verificacao_facial_consistente
    CHECK (verificacao_facial_habilitada = (verificacao_facial_embedding IS NOT NULL));
