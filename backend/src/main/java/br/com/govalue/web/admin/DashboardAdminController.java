package br.com.govalue.web.admin;

import br.com.govalue.service.DashboardService;
import br.com.govalue.web.dto.DashboardDtos.Conclusao;
import br.com.govalue.web.dto.DashboardDtos.ResultadoAvaliacao;
import br.com.govalue.web.dto.DashboardDtos.Visao;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard/avaliacoes/{avaliacaoId}")
public class DashboardAdminController {

    private final DashboardService service;

    public DashboardAdminController(DashboardService service) {
        this.service = service;
    }

    @GetMapping("/conclusao")
    public Conclusao conclusao(@PathVariable Long avaliacaoId) {
        return service.conclusao(avaliacaoId);
    }

    @GetMapping("/resultados")
    public ResultadoAvaliacao resultados(
            @PathVariable Long avaliacaoId, @RequestParam(defaultValue = "FUNCIONARIO") Visao visao) {
        return service.resultados(avaliacaoId, visao);
    }
}
