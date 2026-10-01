# Notas técnicas — GoValue

Diferente do [RioClubs Stats](../rioclubs-api) (onde as notas fazem ponte NestJS↔Spring), aqui o foco é outro: **decisões técnicas não óbvias e os bugs reais encontrados** durante a reescrita — material pra lembrar o "porquê" de cada escolha. Cada seção: o que é, como fica no código, por que foi feito assim.

**Índice:** [1. Camadas](#1-arquitetura-em-camadas) · [2. Erros centralizados](#2-tratamento-de-erro-centralizado) · [3. JWT stateless](#3-autenticação-jwt-stateless) · [4. Bug: 403 virando 401](#4-bug-real-403-sendo-convertido-em-401) · [5. Gestor derivado](#5-perfil-gestor-derivado-não-armazenado) · [6. CPF](#6-cpf-com-dígito-verificador) · [7. Clock + bug de vigência](#7-clock-injetável-e-o-bug-real-de-vigência) · [8. Testcontainers](#8-testcontainers-postgres-real-em-teste) · [9. Flyway](#9-flyway-schema-versionado)

---

## 1. Arquitetura em camadas

**O que é:** cada camada tem uma responsabilidade e só conhece a camada logo abaixo — controller nunca fala direto com o banco, service nunca lida com `HttpServletRequest`.

```
web (controller)  → recebe a requisição, valida DTO, chama o service
service           → regra de negócio, transações
repository        → acesso a dados (Spring Data JPA)
domain            → entidades JPA
```

**Por que assim:** cada camada fica testável isoladamente (dá pra testar `service` com repository mockado, sem precisar subir um servidor HTTP), e trocar um detalhe de uma camada (ex: trocar REST por GraphQL no futuro) não exigiria tocar a regra de negócio.

---

## 2. Tratamento de erro centralizado

**O que é:** em vez de cada controller fazer `try/catch` e decidir o status HTTP manualmente, uma classe só (`@RestControllerAdvice`) intercepta exceções de qualquer controller e converte pro formato de resposta certo.

**Como fica no código** (`web/error/ApiExceptionHandler.java`):

```java
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(NegocioException.class)
    ResponseEntity<ApiError> negocio(NegocioException e) {
        return resposta(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage()); // 422
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    ResponseEntity<ApiError> naoEncontrado(RecursoNaoEncontradoException e) {
        return resposta(HttpStatus.NOT_FOUND, e.getMessage()); // 404
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> integridade(DataIntegrityViolationException e) {
        return resposta(HttpStatus.CONFLICT, "...registro duplicado ou em uso"); // 409
    }
}
```

**Por que assim:** cada exceção de domínio (`NegocioException`, `RecursoNaoEncontradoException`, `CredenciaisInvalidasException`) carrega sua própria semântica — quem lança só descreve *o que* deu errado, nunca precisa saber *qual status HTTP* isso vira. Isso também garante que toda a API responde erro no mesmo formato, sem exceção escapando crua pro cliente.

---

## 3. Autenticação JWT stateless

**O que é:** nenhuma sessão guardada no servidor — cada requisição carrega o token, e um filtro (`OncePerRequestFilter`) decodifica e valida antes da requisição chegar no controller.

**Como fica no código** (`security/JwtAuthenticationFilter.java`):

```java
@Override
protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) {
    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (header != null && header.startsWith("Bearer ")) {
        jwtService.validar(header.substring(7)).ifPresent(usuario -> {
            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + usuario.perfil().name()));
            SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, authorities));
        });
    }
    chain.doFilter(request, response); // segue o fluxo mesmo sem token — quem barra é o authorizeHttpRequests
}
```

**Por que assim:** sem estado no servidor, a API escala horizontalmente sem precisar de sessão compartilhada entre instâncias (sticky session, Redis de sessão, etc.) — qualquer instância valida o token sozinha.

---

## 4. Bug real: 403 sendo convertido em 401

**O problema:** um usuário autenticado, mas sem permissão (ex: não-admin acessando `/api/admin/**`), devia receber **403** (Forbidden) — mas recebia **401** (Unauthorized), a mensagem errada (parecia que o login tinha falhado, quando na real o problema era permissão).

**A causa:** o Spring Security, ao rejeitar por falta de permissão, fazia um *error dispatch* interno pra rota `/error` — mas essa rota também passava pela cadeia de segurança, que (sem saber que já era um erro sendo tratado) rejeitava de novo, dessa vez como 401.

**A correção** (`config/SecurityConfig.java`):

```java
.exceptionHandling(e -> e
    .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
    // Grava o 403 direto (sem sendError), senão o error dispatch para /error o transforma em 401.
    .accessDeniedHandler((request, response, ex) -> {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"status\":403,\"mensagem\":\"Acesso negado\"}");
    }))
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/error").permitAll() // libera a rota de erro em si
    ...
```

O `accessDeniedHandler` customizado escreve a resposta **diretamente** (`response.getWriter().write(...)`) em vez de usar `response.sendError(...)` — isso evita o redirecionamento interno pra `/error` que causava o problema. E `/error` ficou liberado de qualquer forma, como segunda camada de proteção.

**Por que isso importa:** não é um bug óbvio de lógica de negócio — é entender como o pipeline de tratamento de erro do Spring Security interage com o dispatcher de erro do próprio Spring MVC. Esse tipo de interação entre duas camadas de framework é exatamente o que separa quem só usa Spring de quem entende como ele funciona por dentro.

---

## 5. Perfil "Gestor" derivado, não armazenado

**O que é:** "Gestor" não é um valor salvo no banco — é calculado na hora, checando se existe algum funcionário apontando pra essa pessoa como `gestor_id`.

**Como fica no código** (`repository/FuncionarioRepository.java`):

```java
boolean existsByGestorId(Long gestorId);
```

**Por que assim:** se fosse um campo fixo (`is_gestor = true`), bastaria alguém parar de ter subordinados (por desligamento, reorganização) pra esse campo ficar **desatualizado e mentindo** — e alguém teria que lembrar de sincronizar isso manualmente toda vez que a hierarquia mudasse. Derivando na consulta, o dado nunca pode ficar inconsistente com a realidade — é sempre a verdade atual.

---

## 6. CPF com dígito verificador

**O que é:** validação real do algoritmo de dígito verificador de CPF (não só formato/tamanho) — requisito que existia no TCC original (RNF003) mas nunca foi implementado de fato.

**Como fica no código** (`validation/Cpfs.java`):

```java
public static boolean valido(String cpf) {
    String d = normalizar(cpf);
    if (d == null || d.length() != 11 || d.chars().distinct().count() == 1) {
        return false; // rejeita tamanho errado e sequências tipo "11111111111"
    }
    return digito(d, 9) == d.charAt(9) - '0' && digito(d, 10) == d.charAt(10) - '0';
}
```

**Por que assim:** CPFs com todos os dígitos iguais (`111.111.111-11`) passariam numa checagem ingênua de "11 dígitos numéricos", mas são inválidos de verdade — o algoritmo do dígito verificador pega isso. É aplicado via uma anotação de validação customizada (`@Cpf`), reaproveitável em qualquer DTO sem repetir a lógica.

---

## 7. `Clock` injetável e o bug real de vigência

**O que é:** em vez de chamar `LocalDate.now()` direto (que usa o fuso horário padrão da JVM, imprevisível entre ambientes), o projeto injeta um `Clock` configurado explicitamente.

**Como fica no código** (`config/ClockConfig.java`):

```java
@Bean
Clock clock() {
    return Clock.system(ZoneId.of("America/Sao_Paulo")); // fuso fixo, não o da máquina
}
```

**O bug real que isso pegou:** um teste de integração começou a falhar de forma intermitente. A causa: `FuncionarioService` usava `LocalDate.now()` (zona da JVM) pra definir a data de início de vigência de um novo usuário, em vez do `Clock` da aplicação (zona de São Paulo). Perto da virada do dia, as duas zonas podiam divergir por algumas horas — gerando uma vigência começando um dia "no futuro", o que fazia o login falhar logo em seguida (fora da janela de vigência). A correção foi trocar `LocalDate.now()` por `LocalDate.now(clock)`, usando o `Clock` injetado.

**Por que isso importa pra testar:** em teste, um `MutableClock` substitui o `Clock` real (`@Primary` no contexto de teste) — dá pra "viajar no tempo" dentro do teste e verificar regras de vigência de forma determinística, sem depender do relógio real da máquina que roda o teste.

---

## 8. Testcontainers: Postgres real em teste

**O que é:** os testes de integração sobem um Postgres de verdade, **dentro de um container Docker descartável**, em vez de usar um banco em memória (H2) ou mocks.

**Como fica no código** (`support/IntegrationTestBase.java`):

```java
static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");
static { POSTGRES.start(); }

@DynamicPropertySource
static void datasource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
}
```

**Por que assim:** H2 (banco em memória, comum em testes Spring) não se comporta 100% igual ao Postgres em detalhes finos (tipos, constraints, comportamento de certas queries) — um teste verde no H2 não garante nada sobre o banco real de produção. Com Testcontainers, o teste roda contra o **mesmo motor de banco** usado em produção, então um teste que passa aqui é uma garantia de verdade, não uma aproximação.

---

## 9. Flyway: schema versionado

**O que é:** toda mudança de estrutura do banco (criar tabela, adicionar coluna) vira um arquivo de migration numerado (`V1__schema_inicial.sql`, `V2__dados_de_referencia.sql`), aplicado automaticamente na inicialização, em vez de deixar o Hibernate inferir/criar o schema sozinho (`ddl-auto: update`).

**Por que assim:** o sistema original em PHP não tinha nenhum DDL documentado — ninguém conseguiria recriar o schema do zero com confiança. Com Flyway, o histórico de mudanças de schema fica no controle de versão, junto do código, e qualquer ambiente novo (dev, CI, produção) chega no mesmo estado de banco de forma determinística e auditável.
