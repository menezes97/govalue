package br.com.govalue.service;

import br.com.govalue.client.FaceServiceClient;
import br.com.govalue.domain.Usuario;
import br.com.govalue.repository.UsuarioRepository;
import br.com.govalue.security.UsuarioAutenticado;
import br.com.govalue.web.dto.FaceDtos.VerificacaoFacialStatusResponse;
import br.com.govalue.web.error.NegocioException;
import br.com.govalue.web.error.RecursoNaoEncontradoException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Ativacao/desativacao do segundo fator facial, feitas pelo proprio usuario ja autenticado
 * (JWT normal) — diferente do fluxo de LOGIN com face, que mora em AuthService. */
@Service
public class FaceVerificacaoService {

    // Instanciado direto (nao injetado) — mesma razao do AuthService: sem bean ObjectMapper disponivel.
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final UsuarioRepository usuarios;
    private final FaceServiceClient faceService;
    private final Clock clock;

    public FaceVerificacaoService(UsuarioRepository usuarios, FaceServiceClient faceService, Clock clock) {
        this.usuarios = usuarios;
        this.faceService = faceService;
        this.clock = clock;
    }

    @Transactional
    public void registrar(UsuarioAutenticado autenticado, String imagemBase64, boolean consentimento) {
        if (!consentimento) {
            throw new NegocioException("É necessário aceitar o consentimento para ativar a verificação facial");
        }

        Usuario usuario = buscar(autenticado);
        List<Double> embedding = faceService.extrairEmbedding(imagemBase64);

        usuario.setVerificacaoFacialEmbedding(serializarEmbedding(embedding));
        usuario.setVerificacaoFacialHabilitada(true);
        usuario.setVerificacaoFacialConsentimentoEm(Instant.now(clock));
    }

    /** Apaga o embedding (minimizacao de dado biometrico), mas preserva o timestamp de
     * consentimento como prova historica de que ele existiu — ponto fino de LGPD. */
    @Transactional
    public void desativar(UsuarioAutenticado autenticado) {
        Usuario usuario = buscar(autenticado);
        usuario.setVerificacaoFacialHabilitada(false);
        usuario.setVerificacaoFacialEmbedding(null);
    }

    @Transactional(readOnly = true)
    public VerificacaoFacialStatusResponse status(UsuarioAutenticado autenticado) {
        Usuario usuario = buscar(autenticado);
        return new VerificacaoFacialStatusResponse(usuario.isVerificacaoFacialHabilitada());
    }

    private Usuario buscar(UsuarioAutenticado autenticado) {
        return usuarios.findById(autenticado.id())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário", autenticado.id()));
    }

    private String serializarEmbedding(List<Double> embedding) {
        try {
            return OBJECT_MAPPER.writeValueAsString(embedding);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao serializar embedding facial", e);
        }
    }
}
