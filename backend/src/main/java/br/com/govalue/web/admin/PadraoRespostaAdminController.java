package br.com.govalue.web.admin;

import br.com.govalue.service.PadraoRespostaService;
import br.com.govalue.web.dto.CatalogoDtos.PadraoRespostaRequest;
import br.com.govalue.web.dto.CatalogoDtos.PadraoRespostaResponse;
import br.com.govalue.web.dto.CatalogoDtos.RespostaRequest;
import br.com.govalue.web.dto.CatalogoDtos.RespostaResponse;
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
public class PadraoRespostaAdminController {

    private final PadraoRespostaService service;

    public PadraoRespostaAdminController(PadraoRespostaService service) {
        this.service = service;
    }

    @GetMapping("/padroes-resposta")
    public List<PadraoRespostaResponse> listar() {
        return service.listar();
    }

    @PostMapping("/padroes-resposta")
    @ResponseStatus(HttpStatus.CREATED)
    public PadraoRespostaResponse criar(@Valid @RequestBody PadraoRespostaRequest request) {
        return service.criar(request);
    }

    @PutMapping("/padroes-resposta/{id}")
    public PadraoRespostaResponse atualizar(
            @PathVariable Long id, @Valid @RequestBody PadraoRespostaRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/padroes-resposta/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        service.excluir(id);
    }

    @GetMapping("/padroes-resposta/{id}/respostas")
    public List<RespostaResponse> respostas(@PathVariable Long id) {
        return service.listarRespostas(id);
    }

    @PostMapping("/padroes-resposta/{id}/respostas")
    @ResponseStatus(HttpStatus.CREATED)
    public RespostaResponse criarResposta(@PathVariable Long id, @Valid @RequestBody RespostaRequest request) {
        return service.criarResposta(id, request);
    }

    @PutMapping("/respostas/{id}")
    public RespostaResponse atualizarResposta(@PathVariable Long id, @Valid @RequestBody RespostaRequest request) {
        return service.atualizarResposta(id, request);
    }

    @DeleteMapping("/respostas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirResposta(@PathVariable Long id) {
        service.excluirResposta(id);
    }
}
