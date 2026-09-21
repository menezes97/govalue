package br.com.govalue.web.admin;

import br.com.govalue.service.AvaliacaoService;
import br.com.govalue.web.dto.CatalogoDtos.AvaliacaoRequest;
import br.com.govalue.web.dto.CatalogoDtos.AvaliacaoResponse;
import br.com.govalue.web.dto.CatalogoDtos.PerguntaRequest;
import br.com.govalue.web.dto.CatalogoDtos.PerguntaResponse;
import br.com.govalue.web.dto.CatalogoDtos.TipoAvaliacaoResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AvaliacaoAdminController {

    private final AvaliacaoService service;

    public AvaliacaoAdminController(AvaliacaoService service) {
        this.service = service;
    }

    @GetMapping("/tipos-avaliacao")
    public List<TipoAvaliacaoResponse> tipos() {
        return service.listarTipos();
    }

    @GetMapping("/avaliacoes")
    public List<AvaliacaoResponse> listar() {
        return service.listar();
    }

    @GetMapping("/avaliacoes/{id}")
    public AvaliacaoResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping("/avaliacoes")
    @ResponseStatus(HttpStatus.CREATED)
    public AvaliacaoResponse criar(@Valid @RequestBody AvaliacaoRequest request) {
        return service.criar(request);
    }

    @PutMapping("/avaliacoes/{id}")
    public AvaliacaoResponse atualizar(@PathVariable Long id, @Valid @RequestBody AvaliacaoRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/avaliacoes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        service.excluir(id);
    }

    @GetMapping("/avaliacoes/{id}/perguntas")
    public List<PerguntaResponse> perguntas(@PathVariable Long id) {
        return service.listarPerguntas(id);
    }

    @PostMapping("/avaliacoes/{id}/perguntas")
    @ResponseStatus(HttpStatus.CREATED)
    public PerguntaResponse criarPergunta(@PathVariable Long id, @Valid @RequestBody PerguntaRequest request) {
        return service.criarPergunta(id, request);
    }

    @PutMapping("/perguntas/{id}")
    public PerguntaResponse atualizarPergunta(@PathVariable Long id, @Valid @RequestBody PerguntaRequest request) {
        return service.atualizarPergunta(id, request);
    }

    @DeleteMapping("/perguntas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirPergunta(@PathVariable Long id) {
        service.excluirPergunta(id);
    }
}
