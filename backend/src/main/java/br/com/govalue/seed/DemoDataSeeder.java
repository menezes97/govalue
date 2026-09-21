package br.com.govalue.seed;

import br.com.govalue.domain.Avaliacao;
import br.com.govalue.domain.Funcionario;
import br.com.govalue.domain.PadraoResposta;
import br.com.govalue.domain.Perfil;
import br.com.govalue.domain.Pergunta;
import br.com.govalue.domain.Usuario;
import br.com.govalue.repository.AvaliacaoRepository;
import br.com.govalue.repository.FuncionarioRepository;
import br.com.govalue.repository.PadraoRespostaRepository;
import br.com.govalue.repository.PerguntaRepository;
import br.com.govalue.repository.TipoAvaliacaoRepository;
import br.com.govalue.repository.UsuarioRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Popula o banco com dados 100% FICTICIOS para demonstracao. Nenhum dado real do TCC original e usado.
 * Roda apenas se o banco estiver sem usuarios e govalue.seed.enabled=true.
 */
@Component
@ConditionalOnProperty(name = "govalue.seed.enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    static final String SENHA_DEMO = "Demo@1234";

    private final UsuarioRepository usuarios;
    private final FuncionarioRepository funcionarios;
    private final TipoAvaliacaoRepository tipos;
    private final PadraoRespostaRepository padroes;
    private final AvaliacaoRepository avaliacoes;
    private final PerguntaRepository perguntas;
    private final PasswordEncoder encoder;
    private final Clock clock;

    public DemoDataSeeder(
            UsuarioRepository usuarios,
            FuncionarioRepository funcionarios,
            TipoAvaliacaoRepository tipos,
            PadraoRespostaRepository padroes,
            AvaliacaoRepository avaliacoes,
            PerguntaRepository perguntas,
            PasswordEncoder encoder,
            Clock clock) {
        this.usuarios = usuarios;
        this.funcionarios = funcionarios;
        this.tipos = tipos;
        this.padroes = padroes;
        this.avaliacoes = avaliacoes;
        this.perguntas = perguntas;
        this.encoder = encoder;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (usuarios.count() > 0) {
            return;
        }

        criarUsuario("Admin Demo", "admin@govalue.dev", Perfil.ADMIN);

        Funcionario marina = criarFuncionario("Marina Souza", "marina@govalue.dev", "52998224725", "Gerente", "Tecnologia", null);
        criarFuncionario("Carlos Lima", "carlos@govalue.dev", "11144477735", "Desenvolvedor", "Tecnologia", marina);
        criarFuncionario("Ana Pereira", "ana@govalue.dev", "12345678909", "Analista", "Tecnologia", marina);

        criarAvaliacaoDemo();
        log.info("Dados de demonstracao criados. Login: admin@govalue.dev / {}", SENHA_DEMO);
    }

    private Usuario criarUsuario(String nome, String email, Perfil perfil) {
        Usuario u = new Usuario();
        u.setNome(nome);
        u.setEmail(email);
        u.setPerfil(perfil);
        u.setSenhaHash(encoder.encode(SENHA_DEMO));
        u.setDataInicioVigencia(LocalDate.now(clock));
        return usuarios.save(u);
    }

    private Funcionario criarFuncionario(
            String nome, String email, String cpf, String funcao, String area, Funcionario gestor) {
        Funcionario f = new Funcionario();
        f.setCpf(cpf);
        f.setNome(nome);
        f.setFuncao(funcao);
        f.setArea(area);
        f.setMatricula("M" + cpf.substring(0, 4));
        f.setUsuario(criarUsuario(nome, email, Perfil.FUNCIONARIO));
        f.setGestor(gestor);
        return funcionarios.save(f);
    }

    private void criarAvaliacaoDemo() {
        Avaliacao a = new Avaliacao();
        a.setDescricao("Autoavaliação de desempenho 2026");
        a.setDataInicioVigencia(LocalDate.of(2026, 1, 1));
        a.setDataFimVigencia(LocalDate.of(2026, 12, 31));
        a.setTipoAvaliacao(tipos.findAll().stream()
                .filter(t -> t.getDescricao().equals("Autoavaliação"))
                .findFirst()
                .orElseThrow());
        a = avaliacoes.save(a);

        PadraoResposta padrao = padroes.findAll().getFirst();
        for (String texto : List.of(
                "Entendo claramente as metas do meu papel.",
                "Recebo feedback com frequência suficiente.",
                "Tenho as ferramentas necessárias para entregar meu trabalho.",
                "Me sinto reconhecido(a) pelas minhas entregas.")) {
            Pergunta p = new Pergunta();
            p.setAvaliacao(a);
            p.setDescricao(texto);
            p.setPadraoResposta(padrao);
            perguntas.save(p);
        }
    }
}
