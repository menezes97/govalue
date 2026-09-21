package br.com.govalue;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.govalue.domain.Avaliacao;
import br.com.govalue.domain.Funcionario;
import br.com.govalue.support.IntegrationTestBase;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Regras do fluxo central: vinculo em massa, resposta do funcionario, resposta do gestor e janela de vigencia. */
class FluxoAvaliacaoIntegrationTest extends IntegrationTestBase {

    private static final String ADMIN = "admin@teste.com";
    private static final String MARINA = "marina@teste.com";
    private static final String CARLOS = "carlos@teste.com";
    private static final String ANA = "ana@teste.com";

    private Funcionario marina;
    private Funcionario carlos;
    private Funcionario ana;
    private Avaliacao avaliacao;

    @BeforeEach
    void cenario() {
        data.admin(ADMIN);
        marina = data.funcionario("Marina", MARINA, "52998224725", null);
        carlos = data.funcionario("Carlos", CARLOS, "11144477735", marina);
        ana = data.funcionario("Ana", ANA, "12345678909", marina);
        // relogio em 15/06/2026: avaliacao aberta de 01/06 a 30/06, com 3 perguntas
        avaliacao = data.avaliacao("Autoavaliacao 2026", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), 3);
    }

    private String vincularUrl() {
        return "/api/admin/avaliacoes/" + avaliacao.getId() + "/vincular";
    }

    private String minhaUrl() {
        return "/api/minhas-avaliacoes/" + avaliacao.getId();
    }

    private String gestorUrl(Funcionario f) {
        return "/api/gestor/avaliacoes/" + avaliacao.getId() + "/funcionarios/" + f.getId();
    }

    /** Ids (perguntaId, opcaoId) lidos da tela do funcionario, sem depender de ids fixos. */
    private record Tela(List<Integer> perguntas, List<Integer> opcoes) {}

    private Tela tela(String como) throws Exception {
        String corpo = getComo(como, minhaUrl()).andReturn().getResponse().getContentAsString();
        return new Tela(
                JsonPath.read(corpo, "$.perguntas[*].perguntaId"),
                JsonPath.read(corpo, "$.perguntas[0].opcoes[*].id"));
    }

    private String respostas(Tela t, int... indicesDePerguntaEOpcao) {
        StringBuilder itens = new StringBuilder();
        for (int i = 0; i < indicesDePerguntaEOpcao.length; i += 2) {
            if (i > 0) {
                itens.append(',');
            }
            itens.append("{\"perguntaId\":%d,\"respostaId\":%d}"
                    .formatted(t.perguntas().get(indicesDePerguntaEOpcao[i]), t.opcoes().get(indicesDePerguntaEOpcao[i + 1])));
        }
        return "{\"respostas\":[" + itens + "]}";
    }

    // ---------- vinculo ----------

    @Test
    void vincularCriaUmaLinhaPorFuncionarioEPerguntaEIgnoraQuemJaTem() throws Exception {
        postVazioComo(ADMIN, vincularUrl())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPerguntas").value(3))
                .andExpect(jsonPath("$.funcionariosVinculados").value(3))
                .andExpect(jsonPath("$.funcionariosJaVinculados").value(0));

        postVazioComo(ADMIN, vincularUrl())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.funcionariosVinculados").value(0))
                .andExpect(jsonPath("$.funcionariosJaVinculados").value(3));
    }

    @Test
    void vincularAceitaListaEspecificaDeFuncionarios() throws Exception {
        postComo(ADMIN, vincularUrl(), "{\"funcionarioIds\":[%d]}".formatted(carlos.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.funcionariosVinculados").value(1));

        getComo(ANA, "/api/minhas-avaliacoes").andExpect(jsonPath("$.length()").value(0));
        getComo(CARLOS, "/api/minhas-avaliacoes").andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void vincularFuncionarioInexistenteRetorna422() throws Exception {
        postComo(ADMIN, vincularUrl(), "{\"funcionarioIds\":[9999]}").andExpect(status().isUnprocessableContent());
    }

    @Test
    void naoVinculaAvaliacaoSemPerguntas() throws Exception {
        Avaliacao vazia = data.avaliacao("Vazia", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), 0);

        postVazioComo(ADMIN, "/api/admin/avaliacoes/" + vazia.getId() + "/vincular")
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void naoVinculaAvaliacaoComVigenciaEncerrada() throws Exception {
        clock.definir(LocalDate.of(2026, 7, 10));

        postVazioComo(ADMIN, vincularUrl()).andExpect(status().isUnprocessableContent());
    }

    @Test
    void excluirAvaliacaoJaVinculadaEBloqueado() throws Exception {
        postVazioComo(ADMIN, vincularUrl()).andExpect(status().isOk());

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/admin/avaliacoes/" + avaliacao.getId())
                        .header("Authorization", "Bearer " + token(ADMIN)))
                .andExpect(status().isUnprocessableContent());
    }

    // ---------- funcionario responde ----------

    @Test
    void funcionarioRespondeParcialmenteEProgressoEPersistido() throws Exception {
        postVazioComo(ADMIN, vincularUrl());
        Tela t = tela(CARLOS);

        putComo(CARLOS, minhaUrl() + "/respostas", respostas(t, 0, 4, 1, 3))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perguntas[0].respostaFuncionarioId").value(t.opcoes().get(4)))
                .andExpect(jsonPath("$.perguntas[1].respostaFuncionarioId").value(t.opcoes().get(3)))
                .andExpect(jsonPath("$.perguntas[2].respostaFuncionarioId").doesNotExist());

        getComo(CARLOS, "/api/minhas-avaliacoes")
                .andExpect(jsonPath("$[0].respondidas").value(2))
                .andExpect(jsonPath("$[0].totalPerguntas").value(3))
                .andExpect(jsonPath("$[0].aberta").value(true));
    }

    @Test
    void funcionarioPodeAlterarRespostaEnquantoAVigenciaEstaAberta() throws Exception {
        postVazioComo(ADMIN, vincularUrl());
        Tela t = tela(CARLOS);
        putComo(CARLOS, minhaUrl() + "/respostas", respostas(t, 0, 0)).andExpect(status().isOk());

        putComo(CARLOS, minhaUrl() + "/respostas", respostas(t, 0, 4))
                .andExpect(jsonPath("$.perguntas[0].respostaFuncionarioId").value(t.opcoes().get(4)));
    }

    @Test
    void naoPermiteResponderDepoisDoFimDaVigencia() throws Exception {
        postVazioComo(ADMIN, vincularUrl());
        Tela t = tela(CARLOS);

        clock.definir(LocalDate.of(2026, 7, 1));

        putComo(CARLOS, minhaUrl() + "/respostas", respostas(t, 0, 4)).andExpect(status().isUnprocessableContent());
        getComo(CARLOS, minhaUrl()).andExpect(jsonPath("$.aberta").value(false));
    }

    @Test
    void naoPermiteResponderAntesDoInicioDaVigencia() throws Exception {
        postVazioComo(ADMIN, vincularUrl());
        Tela t = tela(CARLOS);

        clock.definir(LocalDate.of(2026, 5, 20));

        putComo(CARLOS, minhaUrl() + "/respostas", respostas(t, 0, 4)).andExpect(status().isUnprocessableContent());
    }

    @Test
    void rejeitaPerguntaDeOutraAvaliacaoEOpcaoInexistente() throws Exception {
        postVazioComo(ADMIN, vincularUrl());
        Tela t = tela(CARLOS);

        putComo(CARLOS, minhaUrl() + "/respostas", "{\"respostas\":[{\"perguntaId\":9999,\"respostaId\":%d}]}"
                        .formatted(t.opcoes().get(0)))
                .andExpect(status().isUnprocessableContent());
        putComo(CARLOS, minhaUrl() + "/respostas", "{\"respostas\":[{\"perguntaId\":%d,\"respostaId\":9999}]}"
                        .formatted(t.perguntas().get(0)))
                .andExpect(status().isNotFound());
    }

    @Test
    void gravacaoEAtomicaUmItemInvalidoNaoSalvaOsValidos() throws Exception {
        postVazioComo(ADMIN, vincularUrl());
        Tela t = tela(CARLOS);

        String corpo = "{\"respostas\":[{\"perguntaId\":%d,\"respostaId\":%d},{\"perguntaId\":9999,\"respostaId\":%d}]}"
                .formatted(t.perguntas().get(0), t.opcoes().get(4), t.opcoes().get(4));
        putComo(CARLOS, minhaUrl() + "/respostas", corpo).andExpect(status().isUnprocessableContent());

        getComo(CARLOS, minhaUrl()).andExpect(jsonPath("$.perguntas[0].respostaFuncionarioId").doesNotExist());
    }

    @Test
    void funcionarioNaoAcessaAvaliacaoQueNaoFoiVinculadaAele() throws Exception {
        postComo(ADMIN, vincularUrl(), "{\"funcionarioIds\":[%d]}".formatted(carlos.getId()));

        getComo(ANA, minhaUrl()).andExpect(status().isNotFound());
    }

    @Test
    void adminSemCadastroDeFuncionarioRecebe422() throws Exception {
        getComo(ADMIN, "/api/minhas-avaliacoes").andExpect(status().isUnprocessableContent());
    }

    // ---------- gestor ----------

    @Test
    void gestorListaApenasAvaliacoesDosSubordinadosDiretos() throws Exception {
        postVazioComo(ADMIN, vincularUrl());

        getComo(MARINA, "/api/gestor/avaliacoes")
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].funcionarioNome").value(org.hamcrest.Matchers.containsInAnyOrder("Ana", "Carlos")));
    }

    @Test
    void quemNaoTemSubordinadosNaoAcessaAAreaDoGestor() throws Exception {
        postVazioComo(ADMIN, vincularUrl());

        getComo(CARLOS, "/api/gestor/avaliacoes").andExpect(status().isForbidden());
        putComo(CARLOS, gestorUrl(ana) + "/respostas", "{\"respostas\":[{\"perguntaId\":1,\"respostaId\":1}]}")
                .andExpect(status().isForbidden());
    }

    @Test
    void gestorVeARespostaDoSubordinadoERespondeSobreEle() throws Exception {
        postVazioComo(ADMIN, vincularUrl());
        Tela t = tela(CARLOS);
        putComo(CARLOS, minhaUrl() + "/respostas", respostas(t, 0, 4));

        getComo(MARINA, gestorUrl(carlos))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.funcionarioNome").value("Carlos"))
                .andExpect(jsonPath("$.perguntas[0].respostaFuncionarioId").value(t.opcoes().get(4)))
                .andExpect(jsonPath("$.perguntas[0].respostaGestorId").doesNotExist());

        putComo(MARINA, gestorUrl(carlos) + "/respostas", respostas(t, 0, 3))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perguntas[0].respostaFuncionarioId").value(t.opcoes().get(4)))
                .andExpect(jsonPath("$.perguntas[0].respostaGestorId").value(t.opcoes().get(3)));
    }

    @Test
    void aOpiniaoDoGestorNaoVazaParaOFuncionario() throws Exception {
        postVazioComo(ADMIN, vincularUrl());
        Tela t = tela(CARLOS);
        putComo(MARINA, gestorUrl(carlos) + "/respostas", respostas(t, 0, 3)).andExpect(status().isOk());

        getComo(CARLOS, minhaUrl())
                .andExpect(jsonPath("$.perguntas[0].respostaGestorId").doesNotExist())
                .andExpect(jsonPath("$.perguntas[0].respostaFuncionarioId").doesNotExist());
    }

    @Test
    void gestorNaoRespondePorQuemNaoESeuSubordinadoDireto() throws Exception {
        postVazioComo(ADMIN, vincularUrl());
        Tela t = tela(CARLOS);

        // Marina nao e subordinada de si mesma
        putComo(MARINA, gestorUrl(marina) + "/respostas", respostas(t, 0, 3)).andExpect(status().isForbidden());
        // Carlos e gerido por Marina; um terceiro gestor nao pode responder por ele
        Funcionario outroGestor = data.funcionario("Outro", "outro@teste.com", "39053344705", null);
        data.funcionario("Subordinado do outro", "sub@teste.com", "45317828791", outroGestor);
        putComo("outro@teste.com", gestorUrl(carlos) + "/respostas", respostas(t, 0, 3)).andExpect(status().isForbidden());
    }

    @Test
    void gestorTambemNaoRespondeForaDaVigencia() throws Exception {
        postVazioComo(ADMIN, vincularUrl());
        Tela t = tela(CARLOS);

        clock.definir(LocalDate.of(2026, 7, 1));

        putComo(MARINA, gestorUrl(carlos) + "/respostas", respostas(t, 0, 3)).andExpect(status().isUnprocessableContent());
    }
}
