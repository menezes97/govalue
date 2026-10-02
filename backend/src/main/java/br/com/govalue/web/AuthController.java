package br.com.govalue.web;

import br.com.govalue.security.UsuarioAutenticado;
import br.com.govalue.service.AuthService;
import br.com.govalue.web.dto.AuthDtos.AlterarSenhaRequest;
import br.com.govalue.web.dto.AuthDtos.LoginRequest;
import br.com.govalue.web.dto.AuthDtos.LoginResponse;
import br.com.govalue.web.dto.AuthDtos.UsuarioResponse;
import br.com.govalue.web.dto.FaceDtos.FaceLoginRequest;
import br.com.govalue.web.dto.LoginResultado;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/login")
    public LoginResultado login(@Valid @RequestBody LoginRequest request) {
        return auth.login(request.email(), request.senha());
    }

    /** Segundo passo do login, só chamado quando /login respondeu com um desafio facial pendente. */
    @PostMapping("/login/face")
    public LoginResponse loginFace(@Valid @RequestBody FaceLoginRequest request) {
        return auth.loginFace(request.tokenFacePendente(), request.imagemBase64());
    }

    @GetMapping("/me")
    public UsuarioResponse me(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return auth.me(usuario);
    }

    @PutMapping("/senha")
    public ResponseEntity<Void> alterarSenha(
            @AuthenticationPrincipal UsuarioAutenticado usuario, @Valid @RequestBody AlterarSenhaRequest request) {
        auth.alterarSenha(usuario, request.senhaAtual(), request.novaSenha());
        return ResponseEntity.noContent().build();
    }
}
