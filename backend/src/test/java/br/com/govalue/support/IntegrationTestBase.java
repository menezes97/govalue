package br.com.govalue.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base dos testes de integracao: Spring Boot completo contra um PostgreSQL real (Testcontainers),
 * relogio controlavel e banco limpo (exceto dados de referencia da V2) a cada teste.
 */
@SpringBootTest(properties = "govalue.seed.enabled=false")
@AutoConfigureMockMvc
@Import({IntegrationTestBase.TestClockConfig.class, IntegrationTestBase.TestFaceServiceConfig.class, TestData.class})
public abstract class IntegrationTestBase {

    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @TestConfiguration
    static class TestClockConfig {
        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock(ZoneId.of("America/Sao_Paulo"), LocalDate.of(2026, 6, 15));
        }
    }

    /** Substitui o FaceServiceClient real (que chamaria o microsservico Python) por um dublê
     * controlável — os testes de verificação facial nunca dependem do face-service estar no ar. */
    @TestConfiguration
    static class TestFaceServiceConfig {
        @Bean
        @Primary
        FakeFaceServiceClient fakeFaceServiceClient() {
            return new FakeFaceServiceClient();
        }
    }

    @Autowired protected MockMvc mvc;
    @Autowired protected TestData data;
    @Autowired protected MutableClock clock;
    @Autowired protected FakeFaceServiceClient faceServiceClient;
    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void limparBancoEResetarRelogio() {
        jdbc.execute("TRUNCATE avaliacao_funcionario, pergunta, avaliacao, funcionario, usuario RESTART IDENTITY CASCADE");
        clock.definir(LocalDate.of(2026, 6, 15));
        faceServiceClient.resetar();
    }

    /** Faz login (pelo relogio atual) e devolve o JWT. Chame de novo depois de mudar o relogio. */
    protected String token(String email) throws Exception {
        String corpo = """
                {"email":"%s","senha":"%s"}""".formatted(email, TestData.SENHA);
        String resposta = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(resposta, "$.token");
    }

    /** Requisicao autenticada como o usuario informado (login feito na hora). */
    protected ResultActions getComo(String como, String url) throws Exception {
        return mvc.perform(autenticar(get(url), como));
    }

    protected ResultActions postComo(String como, String url, String json) throws Exception {
        return mvc.perform(autenticar(post(url), como).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    protected ResultActions putComo(String como, String url, String json) throws Exception {
        return mvc.perform(autenticar(put(url), como).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    protected ResultActions postVazioComo(String como, String url) throws Exception {
        return mvc.perform(autenticar(post(url), como));
    }

    private MockHttpServletRequestBuilder autenticar(MockHttpServletRequestBuilder req, String email) throws Exception {
        return email == null ? req : req.header("Authorization", "Bearer " + token(email));
    }
}
