package br.com.govalue.service;

import br.com.govalue.client.FaceServiceClient;
import br.com.govalue.client.dto.FaceServiceDtos.VerificarResponse;
import br.com.govalue.domain.Funcionario;
import br.com.govalue.domain.Usuario;
import br.com.govalue.repository.FuncionarioRepository;
import br.com.govalue.repository.UsuarioRepository;
import br.com.govalue.security.JwtService;
import br.com.govalue.security.UsuarioAutenticado;
import br.com.govalue.web.dto.AuthDtos.LoginResponse;
import br.com.govalue.web.dto.AuthDtos.UsuarioResponse;
import br.com.govalue.web.dto.FaceDtos.LoginDesafioFacialResponse;
import br.com.govalue.web.dto.LoginResultado;
import br.com.govalue.web.error.CredenciaisInvalidasException;
import br.com.govalue.web.error.NegocioException;
import br.com.govalue.web.error.RecursoNaoEncontradoException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    // Instanciado direto (nao injetado): este projeto nao tem um bean ObjectMapper autoconfigurado
    // disponivel para injecao, e e so usado aqui para (de)serializar o embedding facial.
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final UsuarioRepository usuarios;
    private final FuncionarioRepository funcionarios;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final FaceServiceClient faceService;
    private final Clock clock;

    public AuthService(
            UsuarioRepository usuarios,
            FuncionarioRepository funcionarios,
            PasswordEncoder encoder,
            JwtService jwt,
            FaceServiceClient faceService,
            Clock clock) {
        this.usuarios = usuarios;
        this.funcionarios = funcionarios;
        this.encoder = encoder;
        this.jwt = jwt;
        this.faceService = faceService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public LoginResultado login(String email, String senha) {
        Usuario usuario = usuarios.findByEmailIgnoreCase(email).orElseThrow(CredenciaisInvalidasException::new);

        if (!encoder.matches(senha, usuario.getSenhaHash()) || !usuario.vigenteEm(LocalDate.now(clock))) {
            throw new CredenciaisInvalidasException();
        }

        if (usuario.isVerificacaoFacialHabilitada()) {
            JwtService.TokenEmitido pendente = jwt.emitirFacePendente(usuario);
            return new LoginDesafioFacialResponse(pendente.token(), pendente.expiraEm());
        }

        JwtService.TokenEmitido emitido = jwt.emitir(usuario);
        return new LoginResponse(emitido.token(), emitido.expiraEm(), paraResponse(usuario));
    }

    /** Segundo passo do login quando a verificacao facial esta ativada. Qualquer falha (token
     * invalido/expirado, rosto sem correspondencia) cai na mesma excecao generica do login por
     * senha — mesmo principio de nao revelar detalhe a quem esta tentando autenticar. */
    @Transactional(readOnly = true)
    public LoginResponse loginFace(String tokenFacePendente, String imagemBase64) {
        Long uid = jwt.validarFacePendente(tokenFacePendente).orElseThrow(CredenciaisInvalidasException::new);
        Usuario usuario = usuarios.findById(uid).orElseThrow(CredenciaisInvalidasException::new);

        if (!usuario.isVerificacaoFacialHabilitada() || usuario.getVerificacaoFacialEmbedding() == null) {
            throw new CredenciaisInvalidasException();
        }

        List<Double> embeddingReferencia = desserializarEmbedding(usuario.getVerificacaoFacialEmbedding());
        VerificarResponse resultado = faceService.verificar(imagemBase64, embeddingReferencia);

        if (!resultado.corresponde()) {
            throw new CredenciaisInvalidasException();
        }

        JwtService.TokenEmitido emitido = jwt.emitir(usuario);
        return new LoginResponse(emitido.token(), emitido.expiraEm(), paraResponse(usuario));
    }

    private List<Double> desserializarEmbedding(String json) {
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<List<Double>>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Embedding facial salvo em formato inválido", e);
        }
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
