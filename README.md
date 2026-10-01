# GoValue

Sistema de gestão de pessoas (autoavaliação, pesquisa de engajamento) — reescrita completa de um sistema original feito em **PHP procedural + MySQL** (projeto de conclusão de curso, 2021), agora em **Spring Boot + React**, com arquitetura, segurança e testes de nível profissional.

## Por quê

O projeto original resolvia um problema real (avaliação de desempenho e clima organizacional em empresas), mas carregava os problemas típicos de um sistema sem arquitetura formal: sem versionamento de schema, autenticação frágil, autorização só por flag de página, SQL vulnerável a injeção. Esta reescrita parte do mesmo domínio e das mesmas regras de negócio, mas resolve cada um desses problemas com as ferramentas certas — e serve como prova prática de arquitetura backend em Spring Boot.

## Stack

**Backend:** Java 21, Spring Boot 4, Spring Data JPA, Spring Security (JWT), Flyway, PostgreSQL, springdoc-openapi (Swagger), JUnit 5 + Mockito + Testcontainers.

**Frontend:** React 19, TypeScript, Vite, Mantine, TanStack Query, React Router.

**Infra:** Docker Compose (Postgres + API).

## Arquitetura

Camadas clássicas, uma responsabilidade por camada:

```
web/          → controllers (recebe requisição, valida entrada via DTO)
service/      → regra de negócio
repository/   → acesso a dados (Spring Data JPA)
domain/       → entidades
security/     → JWT (emissão/validação), filtro de autenticação
config/       → segurança, beans de infraestrutura (ex: Clock)
validation/   → validações customizadas (CPF com dígito verificador)
```

Erros são tratados de forma centralizada (`@RestControllerAdvice`), com exceções de domínio (`NegocioException`, `RecursoNaoEncontradoException`, `CredenciaisInvalidasException`) convertidas automaticamente no status HTTP e formato de resposta certos — nenhum controller trata erro manualmente.

## Decisões de design (e porquês)

- **Flyway em vez de `ddl-auto`** — todo schema é versionado em migrations (`V1__schema_inicial.sql`, `V2__dados_de_referencia.sql`), reproduzível em qualquer ambiente, igual acontece em times de verdade. O original não tinha DDL nenhum documentado.
- **Perfil "Gestor" derivado, não armazenado** — em vez de guardar um papel fixo no banco, o sistema deriva se alguém é gestor checando se existe algum funcionário com `gestor_id` apontando pra ele (`existsByGestorId`). Evita um campo que poderia ficar dessincronizado da realidade.
- **CPF com dígito verificador de verdade** — o requisito não-funcional existia no TCC original mas nunca foi implementado; aqui tem o algoritmo completo (`Cpfs.java`) com testes unitários dedicados.
- **`Clock` injetável em vez de `LocalDate.now()` direto** — permite testar regras sensíveis a tempo (janela de vigência de login, de avaliação) de forma determinística, sem esperar o relógio real. Foi isso que pegou um bug real durante o desenvolvimento (ver abaixo).
- **Testes de integração com Postgres real (Testcontainers), não H2/mock** — garante que o comportamento testado é o mesmo do banco de produção. H2 mascarou, em outros projetos, diferenças de comportamento SQL que só apareceriam em produção.
- **Resposta do gestor nunca exposta ao funcionário** — a API nunca retorna `respostaGestor` nas rotas que o próprio funcionário acessa, mesmo que ele tente inspecionar a resposta bruta — é reforçado na camada de serviço, não só escondido na UI.

## Bug real encontrado durante o desenvolvimento

Um teste de integração (`criarFuncionarioTambemCriaOLoginQueFunciona`) começou a falhar de forma intermitente. A causa: o campo `dataInicioVigencia` do usuário usava `LocalDate.now()` (fuso horário padrão da JVM) em vez do `Clock` da aplicação (fuso de São Paulo). Perto da virada do dia, isso podia gerar uma data de vigência um dia à frente do esperado, fazendo o login falhar por estar "fora da janela de vigência". Corrigido injetando `Clock` explicitamente em `FuncionarioService` e no seed de dados — e documentado com teste que reproduz o cenário.

## Comparação com o sistema original (PHP)

| Original (PHP) | Aqui |
|---|---|
| Queries SQL com interpolação de string | JPA/Spring Data (parametrizado, sem injeção) |
| Senha em MD5 sem salt | BCrypt |
| ID gerado por `MAX(id) + 1` | `IDENTITY` (sequence do banco) |
| Sem schema versionado | Flyway (migrations versionadas) |
| Autorização por flag de página | Spring Security por perfil/rota |
| `conexao.php` com credenciais hardcoded, arquivo ausente no repo | Variáveis de ambiente (`.env`, nunca versionado) |

## Como rodar

```bash
cp .env.example .env   # edite DB_PASSWORD e JWT_SECRET
docker compose up -d
```

API em `http://localhost:8080`, documentação interativa em `http://localhost:8080/swagger-ui/index.html`.

Login de demonstração (dados fictícios, seed automático): `admin@govalue.dev` / `Demo@1234`.

Frontend:
```bash
cd frontend
npm install
npm run dev
```

## Testes

```bash
cd backend
./mvnw test
```

63 testes (unitários + integração com Testcontainers), cobrindo autenticação, CRUD administrativo, fluxo completo de avaliação (vincular em massa, responder como funcionário e como gestor) e dashboard — todos passando contra um Postgres real, não mock.
