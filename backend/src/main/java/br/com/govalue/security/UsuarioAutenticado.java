package br.com.govalue.security;

import br.com.govalue.domain.Perfil;

/** Principal colocado no SecurityContext apos validar o JWT. */
public record UsuarioAutenticado(Long id, String email, Perfil perfil) {

    public boolean isAdmin() {
        return perfil == Perfil.ADMIN;
    }
}
