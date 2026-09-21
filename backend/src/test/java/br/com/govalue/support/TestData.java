package br.com.govalue.support;

import br.com.govalue.domain.Avaliacao;
import br.com.govalue.domain.Funcionario;
import br.com.govalue.domain.Perfil;
import br.com.govalue.domain.Pergunta;
import br.com.govalue.domain.Usuario;
import br.com.govalue.repository.AvaliacaoRepository;
import br.com.govalue.repository.FuncionarioRepository;
import br.com.govalue.repository.PadraoRespostaRepository;
import br.com.govalue.repository.PerguntaRepository;
import br.com.govalue.repository.TipoAvaliacaoRepository;
import br.com.govalue.repository.UsuarioRepository;
import java.time.LocalDate;
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

    public TestData(
            UsuarioRepository usuarios,
            FuncionarioRepository funcionarios,
            AvaliacaoRepository avaliacoes,
            PerguntaRepository perguntas,
            TipoAvaliacaoRepository tipos,
            PadraoRespostaRepository padroes,
            PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.funcionarios = funcionarios;
        this.avaliacoes = avaliacoes;
        this.perguntas = perguntas;
        this.tipos = tipos;
        this.padroes = padroes;
        this.encoder = encoder;
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
}
