package br.com.govalue.security;

import br.com.govalue.domain.Perfil;
import br.com.govalue.domain.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Emite e valida JWTs usando o mesmo Clock da aplicacao, para que emissao e expiracao sejam coerentes (e testaveis). */
@Service
public class JwtService {

    public record TokenEmitido(String token, Instant expiraEm) {}

    private final SecretKey key;
    private final Duration expiracao;
    private final Clock clock;

    public JwtService(
            @Value("${govalue.jwt.secret}") String secret,
            @Value("${govalue.jwt.expiration-minutes}") long expirationMinutes,
            Clock clock) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("govalue.jwt.secret precisa ter ao menos 32 caracteres");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expiracao = Duration.ofMinutes(expirationMinutes);
        this.clock = clock;
    }

    public TokenEmitido emitir(Usuario usuario) {
        Instant emitidoEm = clock.instant();
        Instant expiraEm = emitidoEm.plus(expiracao);
        String token = Jwts.builder()
                .subject(usuario.getEmail())
                .claim("uid", usuario.getId())
                .claim("perfil", usuario.getPerfil().name())
                .issuedAt(Date.from(emitidoEm))
                .expiration(Date.from(expiraEm))
                .signWith(key)
                .compact();
        return new TokenEmitido(token, expiraEm);
    }

    /** Retorna vazio para token invalido, expirado ou adulterado. */
    public Optional<UsuarioAutenticado> validar(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Long id = claims.get("uid", Long.class);
            Perfil perfil = Perfil.valueOf(claims.get("perfil", String.class));
            return Optional.of(new UsuarioAutenticado(id, claims.getSubject(), perfil));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
