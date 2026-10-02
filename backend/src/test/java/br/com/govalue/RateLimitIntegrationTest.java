package br.com.govalue;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.govalue.support.IntegrationTestBase;
import br.com.govalue.support.TestData;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class RateLimitIntegrationTest extends IntegrationTestBase {

    private static final String LOGIN = "/api/auth/login";

    private String credenciais(String email, String senha) {
        return """
                {"email":"%s","senha":"%s"}""".formatted(email, senha);
    }

    @Test
    void cincoSenhasErradasBloqueiamASextaTentativaMesmoComASenhaCerta() throws Exception {
        data.admin("admin@teste.com");

        for (int i = 0; i < 5; i++) {
            postComo(null, LOGIN, credenciais("admin@teste.com", "errada"))
                    .andExpect(status().isUnauthorized());
        }

        postComo(null, LOGIN, credenciais("admin@teste.com", TestData.SENHA))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.mensagem").value("Muitas tentativas. Tente novamente mais tarde."));
    }

    @Test
    void bloqueioLiberaSozinhoDepoisDaJanelaDeQuinzeMinutos() throws Exception {
        data.admin("admin@teste.com");

        for (int i = 0; i < 5; i++) {
            postComo(null, LOGIN, credenciais("admin@teste.com", "errada"))
                    .andExpect(status().isUnauthorized());
        }
        postComo(null, LOGIN, credenciais("admin@teste.com", TestData.SENHA))
                .andExpect(status().isTooManyRequests());

        clock.avancar(Duration.ofMinutes(16));

        postComo(null, LOGIN, credenciais("admin@teste.com", TestData.SENHA))
                .andExpect(status().isOk());
    }

    @Test
    void loginCertoNoMeioDoCaminhoLimpaOContador() throws Exception {
        data.admin("admin@teste.com");

        for (int i = 0; i < 3; i++) {
            postComo(null, LOGIN, credenciais("admin@teste.com", "errada"))
                    .andExpect(status().isUnauthorized());
        }

        postComo(null, LOGIN, credenciais("admin@teste.com", TestData.SENHA)).andExpect(status().isOk());

        // depois do login certo, precisa de mais 5 falhas de novo pra bloquear — nao soma com as 3 antigas
        for (int i = 0; i < 4; i++) {
            postComo(null, LOGIN, credenciais("admin@teste.com", "errada"))
                    .andExpect(status().isUnauthorized());
        }
        postComo(null, LOGIN, credenciais("admin@teste.com", TestData.SENHA)).andExpect(status().isOk());
    }

    @Test
    void bloqueioEPorEmailNaoAfetaOutrosUsuarios() throws Exception {
        data.admin("admin@teste.com");
        data.funcionario("Carlos", "carlos@teste.com", "11144477735", null);

        for (int i = 0; i < 5; i++) {
            postComo(null, LOGIN, credenciais("admin@teste.com", "errada"))
                    .andExpect(status().isUnauthorized());
        }
        postComo(null, LOGIN, credenciais("admin@teste.com", TestData.SENHA))
                .andExpect(status().isTooManyRequests());

        postComo(null, LOGIN, credenciais("carlos@teste.com", TestData.SENHA)).andExpect(status().isOk());
    }

    @Test
    void bloqueioTambemValeParaEmailQueNaoExiste() throws Exception {
        // nao vazar se o e-mail existe: bloquear mesmo sem conta nenhuma cadastrada
        for (int i = 0; i < 5; i++) {
            postComo(null, LOGIN, credenciais("ninguem@teste.com", "qualquer"))
                    .andExpect(status().isUnauthorized());
        }
        postComo(null, LOGIN, credenciais("ninguem@teste.com", "qualquer"))
                .andExpect(status().isTooManyRequests());
    }
}
