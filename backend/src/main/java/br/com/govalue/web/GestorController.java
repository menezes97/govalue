package br.com.govalue.web;

import br.com.govalue.security.UsuarioAutenticado;
import br.com.govalue.service.RespostaService;
import br.com.govalue.web.dto.FluxoDtos.DetalheAvaliacao;
import br.com.govalue.web.dto.FluxoDtos.PendenciaGestor;
import br.com.govalue.web.dto.FluxoDtos.SalvarRespostasRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints do gestor ("pesquisas a avaliar"): so respondem para subordinados diretos de quem esta logado. */
@RestController
@RequestMapping("/api/gestor/avaliacoes")
public class GestorController {

    private final RespostaService service;

    public GestorController(RespostaService service) {
        this.service = service;
    }

    @GetMapping
    public List<PendenciaGestor> listar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return service.avaliacoesDosSubordinados(usuario);
    }

    @GetMapping("/{avaliacaoId}/funcionarios/{funcionarioId}")
    public DetalheAvaliacao detalhe(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long avaliacaoId,
            @PathVariable Long funcionarioId) {
        return service.avaliacaoDoSubordinado(usuario, avaliacaoId, funcionarioId);
    }

    @PutMapping("/{avaliacaoId}/funcionarios/{funcionarioId}/respostas")
    public DetalheAvaliacao responder(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long avaliacaoId,
            @PathVariable Long funcionarioId,
            @Valid @RequestBody SalvarRespostasRequest request) {
        return service.salvarRespostasDoGestor(usuario, avaliacaoId, funcionarioId, request);
    }
}
