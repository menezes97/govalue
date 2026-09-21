package br.com.govalue.web.admin;

import br.com.govalue.service.FuncionarioService;
import br.com.govalue.web.dto.FuncionarioDtos.AtualizarFuncionarioRequest;
import br.com.govalue.web.dto.FuncionarioDtos.CriarFuncionarioRequest;
import br.com.govalue.web.dto.FuncionarioDtos.FuncionarioResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/funcionarios")
public class FuncionarioAdminController {

    private final FuncionarioService service;

    public FuncionarioAdminController(FuncionarioService service) {
        this.service = service;
    }

    @GetMapping
    public List<FuncionarioResponse> listar(@RequestParam(required = false) String busca) {
        return service.listar(busca);
    }

    @GetMapping("/{id}")
    public FuncionarioResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FuncionarioResponse criar(@Valid @RequestBody CriarFuncionarioRequest request) {
        return service.criar(request);
    }

    @PutMapping("/{id}")
    public FuncionarioResponse atualizar(
            @PathVariable Long id, @Valid @RequestBody AtualizarFuncionarioRequest request) {
        return service.atualizar(id, request);
    }
}
