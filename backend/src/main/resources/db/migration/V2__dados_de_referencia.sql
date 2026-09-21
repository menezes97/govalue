-- Dados de referencia (nao sao dados pessoais): tipos de avaliacao e escala de resposta.

INSERT INTO tipo_avaliacao (descricao) VALUES
    ('Autoavaliacao'),
    ('Pesquisa de engajamento');

INSERT INTO padrao_resposta (descricao) VALUES ('Escala de concordancia');

INSERT INTO resposta (descricao, padrao_resposta_id)
SELECT d, (SELECT id FROM padrao_resposta WHERE descricao = 'Escala de concordancia')
FROM (VALUES
    ('Discordo totalmente'),
    ('Discordo'),
    ('Neutro'),
    ('Concordo'),
    ('Concordo totalmente')
) AS t(d);
