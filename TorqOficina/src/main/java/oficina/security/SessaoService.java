package oficina.security;

import com.sun.net.httpserver.HttpExchange;

import oficina.exception.RegraNegocioException;
import oficina.model.Sessao;
import oficina.model.Usuario;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Cria, valida e invalida sessões e tokens CSRF. */
public final class SessaoService {

    private final ConcurrentMap<String, Sessao> sessions = new ConcurrentHashMap<>();
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String token() {
        byte[] b = new byte[32];
        RANDOM.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    public String criar(Usuario user, String csrf) {
        sessions.entrySet().removeIf(e -> e.getValue().expires() < System.currentTimeMillis());
        String sid = token();
        sessions.put(
                sid, new Sessao(user.id(), csrf, System.currentTimeMillis() + 8 * 60 * 60 * 1000));
        return sid;
    }

    public void invalidarUsuario(long id) {
        sessions.values().removeIf(s -> s.userId() == id);
    }

    public void encerrar(Sessao sessao) {
        sessions.values().removeIf(sessao::equals);
    }

    public Sessao session(HttpExchange x) {
        String cookie = x.getRequestHeaders().getFirst("Cookie");
        if (cookie != null)
            for (String c : cookie.split(";")) {
                if (c.trim().startsWith("torq_session=")) {
                    var s = sessions.get(c.trim().substring(13));
                    if (s != null && s.expires() > System.currentTimeMillis()) return s;
                }
            }
        throw new RegraNegocioException(401, "Entre para continuar.");
    }
}
