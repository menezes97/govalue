package br.com.govalue.web.admin;

import br.com.govalue.service.VinculoService;
import br.com.govalue.web.dto.FluxoDtos.VincularRequest;
import br.com.govalue.web.dto.FluxoDtos.VinculoResultado;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/avaliacoes")
public class VinculoAdminController {

    private final VinculoService service;

    public VinculoAdminController(VinculoService service) {
        this.service = service;
    }

    /** Corpo opcional: sem funcionarioIds, vincula a todos os funcionarios ativos. */
    @PostMapping("/{avaliacaoId}/vincular")
    public VinculoResultado vincular(
            @PathVariable Long avaliacaoId, @RequestBody(required = false) VincularRequest request) {
        return service.vincular(avaliacaoId, request);
    }
}
