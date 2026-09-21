package br.com.govalue;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.govalue.domain.Avaliacao;
import br.com.govalue.domain.Funcionario;
import br.com.govalue.support.IntegrationTestBase;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FuncionarioAdminIntegrationTest extends IntegrationTestBase {

    private static final String ADMIN = "admin@teste.com";
    private static final String URL = "/api/admin/funcionarios";

    private Funcionario marina;
    private Funcionario carlos;

    @BeforeEach
    void cenario() {
        data.admin(ADMIN);
        marina = data.funcionario("Marina Souza", "marina@teste.com", "52998224725", null);
        carlos = data.funcionario("Carlos Lima", "carlos@teste.com", "11144477735", marina);
    }

    private String novo(String nome, String cpf, String email) {
        return """
                {"nome":"%s","cpf":"%s","email":"%s","senhaInicial":"Senha@1234","funcao":"Dev","area":"TI"}"""
                .formatted(nome, cpf, email);
    }

    private String atualizacao(Funcionario f, String status, Long gestorId) {
        return """
                {"nome":"%s","cpf":"%s","email":"%s","status":"%s","gestorId":%s}"""
                .formatted(f.getNome(), f.getCpf(), f.getUsuario().getEmail(), status, gestorId);
    }

    @Test
    void criarFuncionarioTambemCriaOLoginQueFunciona() throws Exception {
        postComo(ADMIN, URL, novo("Bruno Rocha", "39053344705", "bruno@teste.com"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Bruno Rocha"))
                .andExpect(jsonPath("$.status").value("ATIVO"));

        // o usuario criado junto consegue autenticar com a senha inicial
        token("bruno@teste.com");
    }

    @Test
    void cpfComMascaraEArmazenadoSoComDigitos() throws Exception {
        postComo(ADMIN, URL, novo("Bruno Rocha", "390.533.447-05", "bruno@teste.com"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cpf").value("39053344705"));
    }

    @Test
    void cpfInvalidoRetorna400ComOCampoEMensagemEmPortugues() throws Exception {
        postComo(ADMIN, URL, novo("Bruno", "12345678900", "bruno@teste.com"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.cpf").value("CPF inválido"));
    }

    @Test
    void validacaoDeCamposObrigatoriosNaoVemEmIngles() throws Exception {
        postComo(ADMIN, URL, """
                {"nome":"","cpf":"39053344705","email":"bruno@teste.com","senhaInicial":"Senha@1234"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nome").value(not("must not be blank")));
    }

    @Test
    void cpfDuplicadoEBarradoMesmoComFormatacaoDiferente() throws Exception {
        postComo(ADMIN, URL, novo("Outra Marina", "529.982.247-25", "outra@teste.com"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensagem").value("CPF já cadastrado"));
    }

    @Test
    void emailDuplicadoEBarradoIgnorandoMaiusculas() throws Exception {
        postComo(ADMIN, URL, novo("Bruno", "39053344705", "MARINA@TESTE.COM"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensagem").value("E-mail já cadastrado"));
    }

    @Test
    void gestorInexistenteRetorna404() throws Exception {
        postComo(ADMIN, URL, """
                {"nome":"Bruno","cpf":"39053344705","email":"bruno@teste.com","senhaInicial":"Senha@1234","gestorId":9999}""")
                .andExpect(status().isNotFound());
    }

    @Test
    void ninguemPodeSerGestorDeSiMesmo() throws Exception {
        putComo(ADMIN, URL + "/" + marina.getId(), atualizacao(marina, "ATIVO", marina.getId()))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void hierarquiaCircularEBarrada() throws Exception {
        // Carlos e gerido por Marina; tornar Marina gerida por Carlos fecharia um ciclo
        putComo(ADMIN, URL + "/" + marina.getId(), atualizacao(marina, "ATIVO", carlos.getId()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensagem").value("Hierarquia de gestores circular"));
    }

    @Test
    void atualizarMudaOStatusEBuscaPorNomeIgnoraMaiusculas() throws Exception {
        putComo(ADMIN, URL + "/" + carlos.getId(), atualizacao(carlos, "INATIVO", marina.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INATIVO"));

        getComo(ADMIN, URL + "?busca=CARLOS")
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("INATIVO"));
        getComo(ADMIN, URL).andExpect(jsonPath("$[*].nome").value(containsInAnyOrder("Marina Souza", "Carlos Lima")));
    }

    @Test
    void funcionarioInativoNaoRecebeVinculoEmMassa() throws Exception {
        Avaliacao avaliacao = data.avaliacao("2026", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), 2);
        putComo(ADMIN, URL + "/" + carlos.getId(), atualizacao(carlos, "INATIVO", marina.getId())).andExpect(status().isOk());

        postVazioComo(ADMIN, "/api/admin/avaliacoes/" + avaliacao.getId() + "/vincular")
                .andExpect(jsonPath("$.funcionariosVinculados").value(1));
    }

    @Test
    void apenasAdminAcessaOCadastro() throws Exception {
        getComo("carlos@teste.com", URL).andExpect(status().isForbidden());
        getComo(null, URL).andExpect(status().isUnauthorized());
    }

    @Test
    void senhaInicialCurtaERejeitada() throws Exception {
        postComo(ADMIN, URL, """
                {"nome":"Bruno","cpf":"39053344705","email":"bruno@teste.com","senhaInicial":"123"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.senhaInicial").exists());
    }
}
