package br.com.govalue.service;

import br.com.govalue.domain.Funcionario;
import br.com.govalue.domain.Perfil;
import br.com.govalue.domain.Usuario;
import br.com.govalue.repository.FuncionarioRepository;
import br.com.govalue.repository.UsuarioRepository;
import br.com.govalue.validation.Cpfs;
import br.com.govalue.web.dto.FuncionarioDtos.AtualizarFuncionarioRequest;
import br.com.govalue.web.dto.FuncionarioDtos.CriarFuncionarioRequest;
import br.com.govalue.web.dto.FuncionarioDtos.FuncionarioResponse;
import br.com.govalue.web.error.NegocioException;
import br.com.govalue.web.error.RecursoNaoEncontradoException;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FuncionarioService {

    private static final int LIMITE_HIERARQUIA = 1000;

    private final FuncionarioRepository funcionarios;
    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;

    public FuncionarioService(FuncionarioRepository funcionarios, UsuarioRepository usuarios, PasswordEncoder encoder) {
        this.funcionarios = funcionarios;
        this.usuarios = usuarios;
        this.encoder = encoder;
    }

    @Transactional(readOnly = true)
    public List<FuncionarioResponse> listar(String busca) {
        String termo = busca == null ? "" : busca.trim();
        return funcionarios.findByNomeContainingIgnoreCaseOrderByNome(termo).stream()
                .map(FuncionarioResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public FuncionarioResponse buscar(Long id) {
        return FuncionarioResponse.de(obter(id));
    }

    @Transactional
    public FuncionarioResponse criar(CriarFuncionarioRequest req) {
        String cpf = Cpfs.normalizar(req.cpf());
        if (funcionarios.existsByCpf(cpf)) {
            throw new NegocioException("CPF ja cadastrado");
        }
        if (usuarios.existsByEmailIgnoreCase(req.email())) {
            throw new NegocioException("E-mail ja cadastrado");
        }

        Usuario usuario = new Usuario();
        usuario.setNome(req.nome());
        usuario.setEmail(req.email().trim());
        usuario.setPerfil(Perfil.FUNCIONARIO);
        usuario.setSenhaHash(encoder.encode(req.senhaInicial()));
        usuarios.save(usuario);

        Funcionario funcionario = new Funcionario();
        funcionario.setCpf(cpf);
        funcionario.setUsuario(usuario);
        preencher(funcionario, req.nome(), req.funcao(), req.matricula(), req.area());
        funcionario.setGestor(req.gestorId() == null ? null : obter(req.gestorId()));
        return FuncionarioResponse.de(funcionarios.save(funcionario));
    }

    @Transactional
    public FuncionarioResponse atualizar(Long id, AtualizarFuncionarioRequest req) {
        Funcionario funcionario = obter(id);

        String cpf = Cpfs.normalizar(req.cpf());
        if (!cpf.equals(funcionario.getCpf()) && funcionarios.existsByCpf(cpf)) {
            throw new NegocioException("CPF ja cadastrado");
        }

        Usuario usuario = funcionario.getUsuario();
        String email = req.email().trim();
        if (!email.equalsIgnoreCase(usuario.getEmail()) && usuarios.existsByEmailIgnoreCase(email)) {
            throw new NegocioException("E-mail ja cadastrado");
        }

        validarGestor(funcionario, req.gestorId());

        funcionario.setCpf(cpf);
        funcionario.setStatus(req.status());
        preencher(funcionario, req.nome(), req.funcao(), req.matricula(), req.area());
        funcionario.setGestor(req.gestorId() == null ? null : obter(req.gestorId()));
        usuario.setNome(req.nome());
        usuario.setEmail(email);
        return FuncionarioResponse.de(funcionario);
    }

    private void preencher(Funcionario f, String nome, String funcao, String matricula, String area) {
        f.setNome(nome);
        f.setFuncao(funcao);
        f.setMatricula(matricula);
        f.setArea(area);
    }

    /** Impede que alguem seja gestor de si mesmo ou que a hierarquia de gestores forme um ciclo. */
    private void validarGestor(Funcionario funcionario, Long gestorId) {
        if (gestorId == null) {
            return;
        }
        if (gestorId.equals(funcionario.getId())) {
            throw new NegocioException("Funcionario nao pode ser gestor de si mesmo");
        }
        Funcionario atual = obter(gestorId);
        for (int i = 0; atual != null && i < LIMITE_HIERARQUIA; i++) {
            if (atual.getId().equals(funcionario.getId())) {
                throw new NegocioException("Hierarquia de gestores circular");
            }
            atual = atual.getGestor();
        }
    }

    private Funcionario obter(Long id) {
        return funcionarios.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Funcionario", id));
    }
}
