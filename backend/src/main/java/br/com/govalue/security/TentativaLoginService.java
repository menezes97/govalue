package br.com.govalue.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** Contador em memoria de tentativas de login falhas, por e-mail — bloqueia apos N falhas numa
 * janela de tempo. Instancia unica da aplicacao (nao multi-instancia), entao em memoria e
 * suficiente aqui; nao ha necessidade real de Redis/tabela no banco para esse volume. */
@Component
public class TentativaLoginService {

    private static final int LIMITE_FALHAS = 5;
    private static final Duration JANELA = Duration.ofMinutes(15);

    private final Map<String, Deque<Instant>> falhasPorEmail = new ConcurrentHashMap<>();
    private final Clock clock;

    public TentativaLoginService(Clock clock) {
        this.clock = clock;
    }

    public boolean estaBloqueado(String email) {
        Deque<Instant> falhas = falhasPorEmail.get(normalizar(email));
        if (falhas == null) return false;
        synchronized (falhas) {
            removerExpiradas(falhas);
            return falhas.size() >= LIMITE_FALHAS;
        }
    }

    public void registrarFalha(String email) {
        Deque<Instant> falhas = falhasPorEmail.computeIfAbsent(normalizar(email), k -> new ArrayDeque<>());
        synchronized (falhas) {
            falhas.addLast(clock.instant());
            removerExpiradas(falhas);
        }
    }

    /** Chamado em todo login bem-sucedido, pra nao punir o usuario por falhas antigas depois
     * que ele ja provou quem e. */
    public void limpar(String email) {
        falhasPorEmail.remove(normalizar(email));
    }

    /** Zera todos os contadores — usado só nos testes de integração, pra isolar um teste do
     * outro (o bean é único, compartilhado entre os métodos de teste do mesmo contexto Spring). */
    public void limparTudo() {
        falhasPorEmail.clear();
    }

    private void removerExpiradas(Deque<Instant> falhas) {
        Instant limite = clock.instant().minus(JANELA);
        while (!falhas.isEmpty() && falhas.peekFirst().isBefore(limite)) {
            falhas.pollFirst();
        }
    }

    private String normalizar(String email) {
        return email.toLowerCase();
    }
}
