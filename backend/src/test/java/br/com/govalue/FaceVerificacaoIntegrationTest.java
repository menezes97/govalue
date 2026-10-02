package br.com.govalue;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.govalue.domain.Usuario;
import br.com.govalue.support.IntegrationTestBase;
import br.com.govalue.support.TestData;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class FaceVerificacaoIntegrationTest extends IntegrationTestBase {

    private static final String LOGIN = "/api/auth/login";
    private static final String LOGIN_FACE = "/api/auth/login/face";

    private Usuario usuarioComFaceHabilitada() {
        Usuario usuario = data.admin("admin@teste.com");
        data.habilitarVerificacaoFacial(usuario);
        return usuario;
    }

    private String tokenPendente() throws Exception {
        String resposta = postComo(null, LOGIN, """
                {"email":"admin@teste.com","senha":"%s"}""".formatted(TestData.SENHA))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(resposta, "$.tokenFacePendente");
    }

    @Test
    void loginSemVerificacaoFacialHabilitadaSeguePayloadDeSempre() throws Exception {
        data.admin("admin@teste.com");

        postComo(null, LOGIN, """
                {"email":"admin@teste.com","senha":"%s"}""".formatted(TestData.SENHA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenFacePendente").doesNotExist());
    }

    @Test
    void loginComVerificacaoFacialHabilitadaDevolveDesafioFacialEmVezDeToken() throws Exception {
        usuarioComFaceHabilitada();

        postComo(null, LOGIN, """
                {"email":"admin@teste.com","senha":"%s"}""".formatted(TestData.SENHA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenFacePendente").isNotEmpty())
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.usuario").doesNotExist());
    }

    @Test
    void loginFaceComRostoCorrespondenteCompletaOLoginEEmiteTokenCompleto() throws Exception {
        usuarioComFaceHabilitada();
        String pendente = tokenPendente();
        faceServiceClient.corresponde = true;

        postComo(null, LOGIN_FACE, """
                {"tokenFacePendente":"%s","imagemBase64":"abc"}""".formatted(pendente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.usuario.email").value("admin@teste.com"));
    }

    @Test
    void loginFaceComRostoSemCorrespondenciaERejeitadoComMensagemGenerica() throws Exception {
        usuarioComFaceHabilitada();
        String pendente = tokenPendente();
        faceServiceClient.corresponde = false;

        postComo(null, LOGIN_FACE, """
                {"tokenFacePendente":"%s","imagemBase64":"abc"}""".formatted(pendente))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("E-mail ou senha inválidos"));
    }

    @Test
    void loginFaceComTokenPendenteInvalidoERejeitado() throws Exception {
        postComo(null, LOGIN_FACE, """
                {"tokenFacePendente":"token-lixo","imagemBase64":"abc"}""")
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginFaceFalhaFechadaQuandoServicoFacialIndisponivel() throws Exception {
        usuarioComFaceHabilitada();
        String pendente = tokenPendente();
        faceServiceClient.indisponivel = true;

        postComo(null, LOGIN_FACE, """
                {"tokenFacePendente":"%s","imagemBase64":"abc"}""".formatted(pendente))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void tokenFacePendenteNaoAutenticaRotaProtegida() throws Exception {
        usuarioComFaceHabilitada();
        String pendente = tokenPendente();

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + pendente))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registrarExigeConsentimentoEAtivaAVerificacao() throws Exception {
        data.funcionario("Carlos", "carlos@teste.com", "11144477735", null);
        // Captura o token ANTES de ativar o 2FA: depois de ativado, um login simples (o que o
        // helper token()/postComo() faz por baixo dos panos) passa a devolver so o desafio facial.
        String tokenCarlos = token("carlos@teste.com");

        postComo("carlos@teste.com", "/api/auth/face/registrar", """
                {"imagemBase64":"abc","consentimento":false}""")
                .andExpect(status().isUnprocessableContent());

        mvc.perform(post("/api/auth/face/registrar")
                        .header("Authorization", "Bearer " + tokenCarlos)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"imagemBase64":"abc","consentimento":true}"""))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/auth/face/status").header("Authorization", "Bearer " + tokenCarlos))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.habilitada").value(true));
    }

    @Test
    void registrarComFotoSemRostoERejeitado() throws Exception {
        data.funcionario("Carlos", "carlos@teste.com", "11144477735", null);
        faceServiceClient.semRosto = true;

        postComo("carlos@teste.com", "/api/auth/face/registrar", """
                {"imagemBase64":"abc","consentimento":true}""")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensagem").value("Nenhum rosto detectado na imagem"));
    }

    @Test
    void desativarRemoveAVerificacaoEOLoginVoltaAoFluxoNormal() throws Exception {
        usuarioComFaceHabilitada();
        // Com 2FA ja ligado, so da pra conseguir um token completo completando o segundo fator —
        // um login simples (e o helper token()/postComo() por baixo dos panos) nao serve mais aqui.
        String pendente = tokenPendente();
        faceServiceClient.corresponde = true;
        String resposta = postComo(null, LOGIN_FACE, """
                {"tokenFacePendente":"%s","imagemBase64":"abc"}""".formatted(pendente))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String tokenCompleto = JsonPath.read(resposta, "$.token");

        mvc.perform(put("/api/auth/face/desativar").header("Authorization", "Bearer " + tokenCompleto))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/auth/face/status").header("Authorization", "Bearer " + tokenCompleto))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.habilitada").value(false));

        postComo(null, LOGIN, """
                {"email":"admin@teste.com","senha":"%s"}""".formatted(TestData.SENHA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }
}
