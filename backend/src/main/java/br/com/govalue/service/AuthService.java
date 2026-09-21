package br.com.govalue.service;

import br.com.govalue.domain.Funcionario;
import br.com.govalue.domain.Usuario;
import br.com.govalue.repository.FuncionarioRepository;
import br.com.govalue.repository.UsuarioRepository;
import br.com.govalue.security.JwtService;
import br.com.govalue.security.UsuarioAutenticado;
import br.com.govalue.web.dto.AuthDtos.LoginResponse;
import br.com.govalue.web.dto.AuthDtos.UsuarioResponse;
import br.com.govalue.web.error.CredenciaisInvalidasException;
import br.com.govalue.web.error.NegocioException;
import br.com.govalue.web.error.RecursoNaoEncontradoException;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UsuarioRepository usuarios;
    private final FuncionarioRepository funcionarios;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final Clock clock;

    public AuthService(
            UsuarioRepository usuarios,
            FuncionarioRepository funcionarios,
            PasswordEncoder encoder,
            JwtService jwt,
            Clock clock) {
        this.usuarios = usuarios;
        this.funcionarios = funcionarios;
        this.encoder = encoder;
        this.jwt = jwt;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(String email, String senha) {
        Usuario usuario = usuarios.findByEmailIgnoreCase(email).orElseThrow(CredenciaisInvalidasException::new);

        if (!encoder.matches(senha, usuario.getSenhaHash()) || !usuario.vigenteEm(LocalDate.now(clock))) {
            throw new CredenciaisInvalidasException();
        }

        JwtService.TokenEmitido emitido = jwt.emitir(usuario);
        return new LoginResponse(emitido.token(), emitido.expiraEm(), paraResponse(usuario));
    }

    @Transactional(readOnly = true)
    public UsuarioResponse me(UsuarioAutenticado autenticado) {
        Usuario usuario = usuarios.findById(autenticado.id())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário", autenticado.id()));
        return paraResponse(usuario);
    }

    @Transactional
    public void alterarSenha(UsuarioAutenticado autenticado, String senhaAtual, String novaSenha) {
        Usuario usuario = usuarios.findById(autenticado.id())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário", autenticado.id()));

        if (!encoder.matches(senhaAtual, usuario.getSenhaHash())) {
            throw new NegocioException("Senha atual incorreta");
        }
        usuario.setSenhaHash(encoder.encode(novaSenha));
    }

    private UsuarioResponse paraResponse(Usuario usuario) {
        Funcionario funcionario = funcionarios.findByUsuarioId(usuario.getId()).orElse(null);
        boolean gestor = funcionario != null && funcionarios.existsByGestorId(funcionario.getId());
        Long funcionarioId = funcionario == null ? null : funcionario.getId();
        return new UsuarioResponse(
                usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getPerfil(), funcionarioId, gestor);
    }
}
