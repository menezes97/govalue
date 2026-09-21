package br.com.govalue;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.govalue.domain.Perfil;
import br.com.govalue.support.IntegrationTestBase;
import br.com.govalue.support.TestData;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class AuthIntegrationTest extends IntegrationTestBase {

    private static final String LOGIN = "/api/auth/login";

    private String credenciais(String email, String senha) {
        return """
                {"email":"%s","senha":"%s"}""".formatted(email, senha);
    }

    @Test
    void loginValidoRetornaTokenEPerfil() throws Exception {
        data.admin("admin@teste.com");

        postComo(null, LOGIN, credenciais("admin@teste.com", TestData.SENHA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.usuario.perfil").value("ADMIN"))
                .andExpect(jsonPath("$.usuario.gestor").value(false));
    }

    @Test
    void senhaErradaEEmailInexistenteRecebemAMesmaRespostaParaNaoRevelarContas() throws Exception {
        data.admin("admin@teste.com");

        postComo(null, LOGIN, credenciais("admin@teste.com", "errada"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("E-mail ou senha inválidos"));
        postComo(null, LOGIN, credenciais("ninguem@teste.com", TestData.SENHA))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("E-mail ou senha inválidos"));
    }

    @Test
    void usuarioForaDaVigenciaNaoConsegueLogar() throws Exception {
        data.usuario("Ex-funcionario", "ex@teste.com", Perfil.FUNCIONARIO, LocalDate.of(2026, 1, 1));

        postComo(null, LOGIN, credenciais("ex@teste.com", TestData.SENHA)).andExpect(status().isUnauthorized());
    }

    @Test
    void rotaProtegidaSemTokenRetorna401() throws Exception {
        getComo(null, "/api/auth/me").andExpect(status().isUnauthorized());
    }

    @Test
    void funcionarioNaoAcessaRotasDeAdmin() throws Exception {
        data.funcionario("Carlos", "carlos@teste.com", "11144477735", null);

        getComo("carlos@teste.com", "/api/admin/funcionarios").andExpect(status().isForbidden());
    }

    @Test
    void tokenComPayloadForjadoParaAdminERejeitado() throws Exception {
        data.funcionario("Carlos", "carlos@teste.com", "11144477735", null);
        String[] partes = token("carlos@teste.com").split("\\.");

        String payload = new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8)
                .replace("FUNCIONARIO", "ADMIN");
        String forjado = partes[0] + "."
                + Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8))
                + "." + partes[2];

        mvc.perform(get("/api/admin/funcionarios").header("Authorization", "Bearer " + forjado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenExpiradoERejeitado() throws Exception {
        data.admin("admin@teste.com");
        String token = token("admin@teste.com");

        clock.definir(LocalDate.of(2026, 6, 16));

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void alterarSenhaExigeSenhaAtualEPermiteNovoLogin() throws Exception {
        data.funcionario("Carlos", "carlos@teste.com", "11144477735", null);

        putComo("carlos@teste.com", "/api/auth/senha", """
                {"senhaAtual":"errada","novaSenha":"NovaSenha@99"}""")
                .andExpect(status().isUnprocessableContent());

        putComo("carlos@teste.com", "/api/auth/senha", """
                {"senhaAtual":"%s","novaSenha":"NovaSenha@99"}""".formatted(TestData.SENHA))
                .andExpect(status().isNoContent());

        postComo(null, LOGIN, credenciais("carlos@teste.com", "NovaSenha@99")).andExpect(status().isOk());
        postComo(null, LOGIN, credenciais("carlos@teste.com", TestData.SENHA)).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenNaoTemOsDadosSensiveisNoPayload() throws Exception {
        data.admin("admin@teste.com");
        String payload = new String(
                Base64.getUrlDecoder().decode(token("admin@teste.com").split("\\.")[1]), StandardCharsets.UTF_8);

        assertFalse(payload.toLowerCase().contains("senha"), "o payload do JWT nao deve carregar senha nem hash");
        assertTrue(payload.contains("uid"), "o payload deve identificar o usuario");
    }
}
