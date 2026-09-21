package br.com.govalue.support;

import br.com.govalue.domain.Avaliacao;
import br.com.govalue.domain.AvaliacaoFuncionario;
import br.com.govalue.domain.Funcionario;
import br.com.govalue.domain.Perfil;
import br.com.govalue.domain.Pergunta;
import br.com.govalue.domain.Resposta;
import br.com.govalue.domain.Usuario;
import br.com.govalue.repository.AvaliacaoFuncionarioRepository;
import br.com.govalue.repository.AvaliacaoRepository;
import br.com.govalue.repository.FuncionarioRepository;
import br.com.govalue.repository.PadraoRespostaRepository;
import br.com.govalue.repository.PerguntaRepository;
import br.com.govalue.repository.RespostaRepository;
import br.com.govalue.repository.TipoAvaliacaoRepository;
import br.com.govalue.repository.UsuarioRepository;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Fixtures dos testes de integracao. Os dados de referencia (tipos, escala) vem da migracao V2. */
@Component
public class TestData {

    public static final String SENHA = "Senha@1234";

    private final UsuarioRepository usuarios;
    private final FuncionarioRepository funcionarios;
    private final AvaliacaoRepository avaliacoes;
    private final PerguntaRepository perguntas;
    private final TipoAvaliacaoRepository tipos;
    private final PadraoRespostaRepository padroes;
    private final PasswordEncoder encoder;
    private final AvaliacaoFuncionarioRepository vinculos;
    private final RespostaRepository respostas;

    public TestData(
            UsuarioRepository usuarios,
            FuncionarioRepository funcionarios,
            AvaliacaoRepository avaliacoes,
            PerguntaRepository perguntas,
            TipoAvaliacaoRepository tipos,
            PadraoRespostaRepository padroes,
            PasswordEncoder encoder,
            AvaliacaoFuncionarioRepository vinculos,
            RespostaRepository respostas) {
        this.usuarios = usuarios;
        this.funcionarios = funcionarios;
        this.avaliacoes = avaliacoes;
        this.perguntas = perguntas;
        this.tipos = tipos;
        this.padroes = padroes;
        this.encoder = encoder;
        this.vinculos = vinculos;
        this.respostas = respostas;
    }

    public Usuario admin(String email) {
        return usuario(email, email, Perfil.ADMIN, null);
    }

    public Usuario usuario(String nome, String email, Perfil perfil, LocalDate fimVigencia) {
        Usuario u = new Usuario();
        u.setNome(nome);
        u.setEmail(email);
        u.setPerfil(perfil);
        u.setSenhaHash(encoder.encode(SENHA));
        u.setDataInicioVigencia(LocalDate.of(2000, 1, 1));
        u.setDataFimVigencia(fimVigencia);
        return usuarios.save(u);
    }

    public Funcionario funcionario(String nome, String email, String cpf, Funcionario gestor) {
        Funcionario f = new Funcionario();
        f.setCpf(cpf);
        f.setNome(nome);
        f.setUsuario(usuario(nome, email, Perfil.FUNCIONARIO, null));
        f.setGestor(gestor);
        return funcionarios.save(f);
    }

    /** Avaliacao do tipo "Autoavaliacao" com N perguntas na escala padrao. */
    public Avaliacao avaliacao(String descricao, LocalDate inicio, LocalDate fim, int totalPerguntas) {
        Avaliacao a = new Avaliacao();
        a.setDescricao(descricao);
        a.setDataInicioVigencia(inicio);
        a.setDataFimVigencia(fim);
        a.setTipoAvaliacao(tipos.findAll().getFirst());
        a = avaliacoes.save(a);
        for (int i = 1; i <= totalPerguntas; i++) {
            Pergunta p = new Pergunta();
            p.setAvaliacao(a);
            p.setDescricao("Pergunta " + i);
            p.setPadraoResposta(padroes.findAll().getFirst());
            perguntas.save(p);
        }
        return a;
    }

    /** Vincula a avaliacao aos funcionarios (uma linha por funcionario x pergunta), sem respostas. */
    public void vincular(Avaliacao avaliacao, Funcionario... alvo) {
        for (Funcionario f : alvo) {
            for (Pergunta p : perguntas.findByAvaliacaoId(avaliacao.getId())) {
                AvaliacaoFuncionario linha = new AvaliacaoFuncionario();
                linha.setFuncionario(f);
                linha.setAvaliacao(avaliacao);
                linha.setPergunta(p);
                vinculos.save(linha);
            }
        }
    }

    /** Grava a resposta do funcionario na pergunta (indice na ordem de id) usando a opcao (indice na ordem de id). */
    public void responder(Funcionario f, Avaliacao a, int indicePergunta, int indiceOpcao) {
        gravar(f, a, indicePergunta, indiceOpcao, false);
    }

    public void responderComoGestor(Funcionario f, Avaliacao a, int indicePergunta, int indiceOpcao) {
        gravar(f, a, indicePergunta, indiceOpcao, true);
    }

    private void gravar(Funcionario f, Avaliacao a, int indicePergunta, int indiceOpcao, boolean gestor) {
        List<AvaliacaoFuncionario> linhas = vinculos.findDoFuncionarioNaAvaliacao(f.getId(), a.getId());
        AvaliacaoFuncionario linha = linhas.get(indicePergunta);
        Resposta opcao = respostas.findByPadraoRespostaId(linha.getPergunta().getPadraoResposta().getId()).stream()
                .sorted(Comparator.comparing(Resposta::getId))
                .toList()
                .get(indiceOpcao);
        if (gestor) {
            linha.setRespostaGestor(opcao);
        } else {
            linha.setResposta(opcao);
        }
        vinculos.save(linha);
    }
}
