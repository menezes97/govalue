-- Schema inicial do GoValue (reescrita do TCC em PHP/MySQL).
-- Modelo baseado no diagrama de classes do TCC, com IDs gerados pelo banco
-- (no original eram calculados com SELECT MAX(id)+1) e FKs/constraints explicitas.

CREATE TABLE usuario (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome                  VARCHAR(150) NOT NULL,
    email                 VARCHAR(150) NOT NULL,
    senha_hash            VARCHAR(100) NOT NULL,
    perfil                VARCHAR(20)  NOT NULL,
    data_inicio_vigencia  DATE         NOT NULL DEFAULT CURRENT_DATE,
    data_fim_vigencia     DATE,
    CONSTRAINT uk_usuario_email UNIQUE (email),
    CONSTRAINT ck_usuario_perfil CHECK (perfil IN ('ADMIN', 'FUNCIONARIO'))
);

CREATE TABLE funcionario (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cpf         VARCHAR(11)  NOT NULL,
    nome        VARCHAR(150) NOT NULL,
    funcao      VARCHAR(100),
    matricula   VARCHAR(30),
    area        VARCHAR(100),
    status      VARCHAR(10)  NOT NULL DEFAULT 'ATIVO',
    usuario_id  BIGINT       NOT NULL,
    gestor_id   BIGINT,
    CONSTRAINT uk_funcionario_cpf UNIQUE (cpf),
    CONSTRAINT uk_funcionario_usuario UNIQUE (usuario_id),
    CONSTRAINT fk_funcionario_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT fk_funcionario_gestor FOREIGN KEY (gestor_id) REFERENCES funcionario (id),
    CONSTRAINT ck_funcionario_cpf CHECK (cpf ~ '^[0-9]{11}$'),
    CONSTRAINT ck_funcionario_status CHECK (status IN ('ATIVO', 'INATIVO')),
    CONSTRAINT ck_funcionario_gestor_proprio CHECK (gestor_id IS NULL OR gestor_id <> id)
);

CREATE TABLE tipo_avaliacao (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    descricao  VARCHAR(100) NOT NULL,
    CONSTRAINT uk_tipo_avaliacao_descricao UNIQUE (descricao)
);

CREATE TABLE avaliacao (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    descricao             VARCHAR(200) NOT NULL,
    data_inicio_vigencia  DATE         NOT NULL,
    data_fim_vigencia     DATE         NOT NULL,
    tipo_avaliacao_id     BIGINT       NOT NULL,
    CONSTRAINT fk_avaliacao_tipo FOREIGN KEY (tipo_avaliacao_id) REFERENCES tipo_avaliacao (id),
    CONSTRAINT ck_avaliacao_vigencia CHECK (data_fim_vigencia >= data_inicio_vigencia)
);

CREATE TABLE padrao_resposta (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    descricao  VARCHAR(100) NOT NULL,
    CONSTRAINT uk_padrao_resposta_descricao UNIQUE (descricao)
);

CREATE TABLE resposta (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    descricao            VARCHAR(150) NOT NULL,
    padrao_resposta_id   BIGINT       NOT NULL,
    CONSTRAINT fk_resposta_padrao FOREIGN KEY (padrao_resposta_id) REFERENCES padrao_resposta (id)
);

CREATE TABLE pergunta (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    avaliacao_id         BIGINT       NOT NULL,
    descricao            VARCHAR(500) NOT NULL,
    padrao_resposta_id   BIGINT       NOT NULL,
    CONSTRAINT fk_pergunta_avaliacao FOREIGN KEY (avaliacao_id) REFERENCES avaliacao (id),
    CONSTRAINT fk_pergunta_padrao FOREIGN KEY (padrao_resposta_id) REFERENCES padrao_resposta (id)
);

-- Uma linha por funcionario x pergunta vinculada (mesmo desenho do original,
-- que guardava "resposta" e "resposta_gestor" na mesma linha).
CREATE TABLE avaliacao_funcionario (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    funcionario_id      BIGINT NOT NULL,
    avaliacao_id        BIGINT NOT NULL,
    pergunta_id         BIGINT NOT NULL,
    resposta_id         BIGINT,
    resposta_gestor_id  BIGINT,
    CONSTRAINT uk_avaliacao_funcionario UNIQUE (funcionario_id, pergunta_id),
    CONSTRAINT fk_af_funcionario FOREIGN KEY (funcionario_id) REFERENCES funcionario (id),
    CONSTRAINT fk_af_avaliacao FOREIGN KEY (avaliacao_id) REFERENCES avaliacao (id),
    CONSTRAINT fk_af_pergunta FOREIGN KEY (pergunta_id) REFERENCES pergunta (id),
    CONSTRAINT fk_af_resposta FOREIGN KEY (resposta_id) REFERENCES resposta (id),
    CONSTRAINT fk_af_resposta_gestor FOREIGN KEY (resposta_gestor_id) REFERENCES resposta (id)
);

CREATE INDEX idx_pergunta_avaliacao ON pergunta (avaliacao_id);
CREATE INDEX idx_funcionario_gestor ON funcionario (gestor_id);
CREATE INDEX idx_af_avaliacao ON avaliacao_funcionario (avaliacao_id);
CREATE INDEX idx_af_funcionario ON avaliacao_funcionario (funcionario_id);
