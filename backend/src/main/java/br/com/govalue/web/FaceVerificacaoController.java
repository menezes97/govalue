package br.com.govalue.web;

import br.com.govalue.security.UsuarioAutenticado;
import br.com.govalue.service.FaceVerificacaoService;
import br.com.govalue.web.dto.FaceDtos.RegistrarFaceRequest;
import br.com.govalue.web.dto.FaceDtos.VerificacaoFacialStatusResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Ativar/desativar/consultar o segundo fator facial — requer JWT normal (usuário já logado).
 * O passo de verificação DURANTE o login mora em AuthController/AuthService. */
@RestController
@RequestMapping("/api/auth/face")
public class FaceVerificacaoController {

    private final FaceVerificacaoService service;

    public FaceVerificacaoController(FaceVerificacaoService service) {
        this.service = service;
    }

    @PostMapping("/registrar")
    public ResponseEntity<Void> registrar(
            @AuthenticationPrincipal UsuarioAutenticado usuario, @Valid @RequestBody RegistrarFaceRequest request) {
        service.registrar(usuario, request.imagemBase64(), request.consentimento());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/desativar")
    public ResponseEntity<Void> desativar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        service.desativar(usuario);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/status")
    public VerificacaoFacialStatusResponse status(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return service.status(usuario);
    }
}
