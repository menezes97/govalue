package br.com.govalue.security;

import br.com.govalue.domain.Perfil;
import br.com.govalue.domain.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final SecretKey key;
    private final Duration expiracao;

    public JwtService(
            @Value("${govalue.jwt.secret}") String secret,
            @Value("${govalue.jwt.expiration-minutes}") long expirationMinutes) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("govalue.jwt.secret precisa ter ao menos 32 caracteres");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expiracao = Duration.ofMinutes(expirationMinutes);
    }

    public Instant expiraEm(Instant emitidoEm) {
        return emitidoEm.plus(expiracao);
    }

    public String gerar(Usuario usuario, Instant emitidoEm) {
        return Jwts.builder()
                .subject(usuario.getEmail())
                .claim("uid", usuario.getId())
                .claim("perfil", usuario.getPerfil().name())
                .issuedAt(Date.from(emitidoEm))
                .expiration(Date.from(expiraEm(emitidoEm)))
                .signWith(key)
                .compact();
    }

    /** Retorna vazio para token invalido, expirado ou adulterado. */
    public Optional<UsuarioAutenticado> validar(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            Long id = claims.get("uid", Long.class);
            Perfil perfil = Perfil.valueOf(claims.get("perfil", String.class));
            return Optional.of(new UsuarioAutenticado(id, claims.getSubject(), perfil));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
