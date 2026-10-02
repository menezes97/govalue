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

    private static final String ESCOPO_FACE_PENDENTE = "FACE_PENDENTE";

    private final SecretKey key;
    private final Duration expiracao;
    private final Duration expiracaoFacePendente;
    private final Clock clock;

    public JwtService(
            @Value("${govalue.jwt.secret}") String secret,
            @Value("${govalue.jwt.expiration-minutes}") long expirationMinutes,
            @Value("${govalue.jwt.face-pending-expiration-minutes}") long facePendingExpirationMinutes,
            Clock clock) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("govalue.jwt.secret precisa ter ao menos 32 caracteres");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expiracao = Duration.ofMinutes(expirationMinutes);
        this.expiracaoFacePendente = Duration.ofMinutes(facePendingExpirationMinutes);
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

    /** Retorna vazio para token invalido, expirado, adulterado, ou sem a claim "perfil" (caso do
     * token de face pendente — Enum.valueOf(null) lanca NullPointerException, nao
     * IllegalArgumentException, entao o null e checado explicitamente antes). */
    public Optional<UsuarioAutenticado> validar(String token) {
        try {
            Claims claims = parsear(token);
            String perfilClaim = claims.get("perfil", String.class);
            if (perfilClaim == null) {
                return Optional.empty();
            }
            Long id = claims.get("uid", Long.class);
            Perfil perfil = Perfil.valueOf(perfilClaim);
            return Optional.of(new UsuarioAutenticado(id, claims.getSubject(), perfil));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /** Token do passo intermediario do login (senha OK, falta o segundo fator facial). Sem claim
     * "perfil" de proposito: se usado como Bearer normal, validar() falha ao resolver o Perfil —
     * defesa em profundidade, alem de nunca passar pelo JwtAuthenticationFilter (ver FaceVerificacaoController). */
    public TokenEmitido emitirFacePendente(Usuario usuario) {
        Instant emitidoEm = clock.instant();
        Instant expiraEm = emitidoEm.plus(expiracaoFacePendente);
        String token = Jwts.builder()
                .subject(usuario.getEmail())
                .claim("uid", usuario.getId())
                .claim("escopo", ESCOPO_FACE_PENDENTE)
                .issuedAt(Date.from(emitidoEm))
                .expiration(Date.from(expiraEm))
                .signWith(key)
                .compact();
        return new TokenEmitido(token, expiraEm);
    }

    /** Retorna o uid do usuario se o token for um token de face pendente valido e nao expirado. */
    public Optional<Long> validarFacePendente(String token) {
        try {
            Claims claims = parsear(token);
            if (!ESCOPO_FACE_PENDENTE.equals(claims.get("escopo", String.class))) {
                return Optional.empty();
            }
            return Optional.of(claims.get("uid", Long.class));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private Claims parsear(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .clock(() -> Date.from(clock.instant()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
