package br.com.govalue;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

/**
 * Cenario (3 funcionarios, 3 perguntas, escala de 5 opcoes):
 * Marina responde 0 de 3; Carlos responde 2 de 3; Ana responde 3 de 3.
 * Na pergunta 1: Carlos e Ana escolhem a opcao 5 (indice 4); Marina nao responde.
 */
class DashboardIntegrationTest extends IntegrationTestBase {

    private static final String ADMIN = "admin@teste.com";

    private Funcionario marina;
    private Funcionario carlos;
    private Funcionario ana;
    private Avaliacao avaliacao;

    @BeforeEach
    void cenario() {
        data.admin(ADMIN);
        marina = data.funcionario("Marina", "marina@teste.com", "52998224725", null);
        carlos = data.funcionario("Carlos", "carlos@teste.com", "11144477735", marina);
        ana = data.funcionario("Ana", "ana@teste.com", "12345678909", marina);
        avaliacao = data.avaliacao("Autoavaliação 2026", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), 3);
        data.vincular(avaliacao, marina, carlos, ana);

        data.responder(carlos, avaliacao, 0, 4);
        data.responder(carlos, avaliacao, 1, 3);
        data.responder(ana, avaliacao, 0, 4);
        data.responder(ana, avaliacao, 1, 0);
        data.responder(ana, avaliacao, 2, 2);
    }

    private String base() {
        return "/api/admin/dashboard/avaliacoes/" + avaliacao.getId();
    }

    @Test
    void conclusaoPorFuncionarioEGeral() throws Exception {
        getComo(ADMIN, base() + "/conclusao")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFuncionarios").value(3))
                .andExpect(jsonPath("$.concluidos").value(1))
                // (0 + 2 + 3) respondidas de 9 = 55,6%
                .andExpect(jsonPath("$.percentualGeral").value(55.6))
                // ordenado por nome: Ana, Carlos, Marina
                .andExpect(jsonPath("$.funcionarios[0].nome").value("Ana"))
                .andExpect(jsonPath("$.funcionarios[0].percentual").value(100))
                .andExpect(jsonPath("$.funcionarios[1].nome").value("Carlos"))
                .andExpect(jsonPath("$.funcionarios[1].respondidas").value(2))
                .andExpect(jsonPath("$.funcionarios[1].percentual").value(67))
                .andExpect(jsonPath("$.funcionarios[2].nome").value("Marina"))
                .andExpect(jsonPath("$.funcionarios[2].percentual").value(0));
    }

    @Test
    void distribuicaoPorPerguntaComOpcoesZeradasEBaldeSemResposta() throws Exception {
        getComo(ADMIN, base() + "/resultados")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visao").value("FUNCIONARIO"))
                .andExpect(jsonPath("$.perguntas.length()").value(3))
                .andExpect(jsonPath("$.perguntas[0].totalVinculados").value(3))
                .andExpect(jsonPath("$.perguntas[0].respondidas").value(2))
                .andExpect(jsonPath("$.perguntas[0].semResposta").value(1))
                .andExpect(jsonPath("$.perguntas[0].semRespostaPercentual").value(33.3))
                // todas as 5 opcoes aparecem, inclusive as sem nenhuma resposta
                .andExpect(jsonPath("$.perguntas[0].opcoes.length()").value(5))
                .andExpect(jsonPath("$.perguntas[0].opcoes[0].quantidade").value(0))
                .andExpect(jsonPath("$.perguntas[0].opcoes[4].quantidade").value(2))
                .andExpect(jsonPath("$.perguntas[0].opcoes[4].percentual").value(66.7));
    }

    @Test
    void osPercentuaisDeUmaPerguntaSomamCemPorCento() throws Exception {
        String corpo = getComo(ADMIN, base() + "/resultados").andReturn().getResponse().getContentAsString();
        for (int i = 0; i < 3; i++) {
            List<Number> percentuais = JsonPath.read(corpo, "$.perguntas[" + i + "].opcoes[*].percentual");
            Number semResposta = JsonPath.read(corpo, "$.perguntas[" + i + "].semRespostaPercentual");
            double soma = percentuais.stream().mapToDouble(Number::doubleValue).sum() + semResposta.doubleValue();
            assertEquals(100.0, soma, 0.3, "pergunta " + i);
        }
    }

    @Test
    void visaoDoGestorUsaSomenteAsRespostasDoGestor() throws Exception {
        data.responderComoGestor(carlos, avaliacao, 0, 1);

        getComo(ADMIN, base() + "/resultados?visao=GESTOR")
                .andExpect(jsonPath("$.visao").value("GESTOR"))
                .andExpect(jsonPath("$.perguntas[0].respondidas").value(1))
                .andExpect(jsonPath("$.perguntas[0].opcoes[1].quantidade").value(1))
                // as respostas dos funcionarios nao entram nesta visao
                .andExpect(jsonPath("$.perguntas[0].opcoes[4].quantidade").value(0));

        getComo(ADMIN, base() + "/conclusao")
                .andExpect(jsonPath("$.funcionarios[1].respondidasPeloGestor").value(1))
                .andExpect(jsonPath("$.funcionarios[1].percentualGestor").value(33));
    }

    @Test
    void avaliacaoSemVinculosNaoQuebraNemDividePorZero() throws Exception {
        Avaliacao vazia = data.avaliacao("Sem vínculos", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), 2);
        String url = "/api/admin/dashboard/avaliacoes/" + vazia.getId();

        getComo(ADMIN, url + "/conclusao")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFuncionarios").value(0))
                .andExpect(jsonPath("$.percentualGeral").value(0.0))
                .andExpect(jsonPath("$.funcionarios.length()").value(0));

        getComo(ADMIN, url + "/resultados")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perguntas.length()").value(2))
                .andExpect(jsonPath("$.perguntas[0].totalVinculados").value(0))
                .andExpect(jsonPath("$.perguntas[0].opcoes[0].percentual").value(0.0))
                .andExpect(jsonPath("$.perguntas[0].semRespostaPercentual").value(0.0));
    }

    @Test
    void avaliacaoInexistenteRetorna404() throws Exception {
        getComo(ADMIN, "/api/admin/dashboard/avaliacoes/9999/conclusao").andExpect(status().isNotFound());
        getComo(ADMIN, "/api/admin/dashboard/avaliacoes/9999/resultados").andExpect(status().isNotFound());
    }

    @Test
    void apenasAdminVeODashboard() throws Exception {
        getComo("carlos@teste.com", base() + "/conclusao").andExpect(status().isForbidden());
        getComo("carlos@teste.com", base() + "/resultados").andExpect(status().isForbidden());
        getComo(null, base() + "/conclusao").andExpect(status().isUnauthorized());
    }
}
