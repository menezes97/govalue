package br.com.govalue.web;

import br.com.govalue.security.UsuarioAutenticado;
import br.com.govalue.service.RespostaService;
import br.com.govalue.web.dto.FluxoDtos.DetalheAvaliacao;
import br.com.govalue.web.dto.FluxoDtos.ResumoAvaliacao;
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

@RestController
@RequestMapping("/api/minhas-avaliacoes")
public class MinhasAvaliacoesController {

    private final RespostaService service;

    public MinhasAvaliacoesController(RespostaService service) {
        this.service = service;
    }

    @GetMapping
    public List<ResumoAvaliacao> listar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return service.minhasAvaliacoes(usuario);
    }

    @GetMapping("/{avaliacaoId}")
    public DetalheAvaliacao detalhe(
            @AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable Long avaliacaoId) {
        return service.minhaAvaliacao(usuario, avaliacaoId);
    }

    @PutMapping("/{avaliacaoId}/respostas")
    public DetalheAvaliacao responder(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long avaliacaoId,
            @Valid @RequestBody SalvarRespostasRequest request) {
        return service.salvarMinhasRespostas(usuario, avaliacaoId, request);
    }
}
