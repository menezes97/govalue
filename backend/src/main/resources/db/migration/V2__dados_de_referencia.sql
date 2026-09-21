-- Dados de referencia (nao sao dados pessoais): tipos de avaliacao e escala de resposta.

INSERT INTO tipo_avaliacao (descricao) VALUES
    ('Autoavaliação'),
    ('Pesquisa de engajamento');

INSERT INTO padrao_resposta (descricao) VALUES ('Escala de concordância');

INSERT INTO resposta (descricao, padrao_resposta_id)
SELECT d, (SELECT id FROM padrao_resposta WHERE descricao = 'Escala de concordância')
FROM (VALUES
    ('Discordo totalmente'),
    ('Discordo'),
    ('Neutro'),
    ('Concordo'),
    ('Concordo totalmente')
) AS t(d);
