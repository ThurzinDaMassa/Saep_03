package oficina.web;

import static oficina.dao.JdbcDao.query;
import static oficina.dao.RegistroDao.audit;
import static oficina.validation.Validacao.email;
import static oficina.web.HttpUtil.form;
import static oficina.web.HttpUtil.reply;

import com.sun.net.httpserver.HttpExchange;

import oficina.database.ConnectionFactory;
import oficina.exception.RegraNegocioException;
import oficina.model.Sessao;
import oficina.model.Usuario;
import oficina.security.SenhaService;
import oficina.security.SessaoService;
import oficina.service.OficinaService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Autenticação, CSRF e transações da API; regras de domínio ficam nos serviços. */
public final class ApiController {

    private final ConnectionFactory connections;
    private final SessaoService sessoes;
    private final OficinaService oficina;

    public ApiController(
            ConnectionFactory connections, SessaoService sessoes, OficinaService oficina) {
        this.connections = connections;
        this.sessoes = sessoes;
        this.oficina = oficina;
    }

    public void handle(HttpExchange x) throws IOException {
        try {
            String path = x.getRequestURI().getPath().substring(5), method = x.getRequestMethod();
            if (!Set.of("GET", "POST").contains(method))
                throw new RegraNegocioException(405, "Método não permitido.");
            byte[] bytes = x.getRequestBody().readNBytes(65537);
            if (bytes.length > 65536) throw new RegraNegocioException(413, "Dados muito grandes.");
            var f = form(new String(bytes, StandardCharsets.UTF_8));
            try (var db = connections.connect()) {
                if (path.equals("login") && method.equals("POST")) {
                    var users =
                            query(
                                    db,
                                    "SELECT * FROM usuarios WHERE email=? AND ativo=TRUE",
                                    email(f, "email", true));
                    if (users.isEmpty()
                            || !SenhaService.verify(
                                    f.getOrDefault("senha", ""),
                                    users.get(0).get("senha_hash").toString()))
                        throw new RegraNegocioException(401, "E-mail ou senha incorretos.");
                    Usuario user = Usuario.fromRow(users.get(0));
                    String csrf = SessaoService.token();
                    long uid = user.id();
                    String sid = sessoes.criar(user, csrf);
                    x.getResponseHeaders()
                            .add(
                                    "Set-Cookie",
                                    "torq_session="
                                            + sid
                                            + "; HttpOnly; SameSite=Strict; Path=/; Max-Age=28800");
                    audit(db, uid, "Login realizado");
                    reply(x, 200, Map.of("usuario", user.dadosPublicos(), "csrf", csrf));
                    return;
                }
                Sessao ses = sessoes.session(x);
                var activeUsers =
                        query(db, "SELECT * FROM usuarios WHERE id=? AND ativo=TRUE", ses.userId());
                if (activeUsers.isEmpty()) {
                    sessoes.invalidarUsuario(ses.userId());
                    throw new RegraNegocioException(
                            401, "Seu acesso foi desativado. Entre com um usuário ativo.");
                }
                Usuario user = Usuario.fromRow(activeUsers.get(0));
                if (method.equals("POST")
                        && !MessageDigest.isEqual(
                                ses.csrf().getBytes(StandardCharsets.UTF_8),
                                Objects.toString(x.getRequestHeaders().getFirst("X-CSRF-Token"), "")
                                        .getBytes(StandardCharsets.UTF_8)))
                    throw new RegraNegocioException(403, "Sessão inválida. Atualize a página.");
                if (path.equals("logout") && method.equals("POST")) {
                    sessoes.encerrar(ses);
                    x.getResponseHeaders()
                            .add(
                                    "Set-Cookie",
                                    "torq_session=; HttpOnly; SameSite=Strict; Path=/; Max-Age=0");
                    reply(x, 200, Map.of("ok", true));
                    return;
                }
                if (path.equals("me") && method.equals("GET")) {
                    reply(x, 200, Map.of("usuario", user.dadosPublicos(), "csrf", ses.csrf()));
                    return;
                }
                if (method.equals("GET")) {
                    reply(
                            x,
                            200,
                            oficina.consultar(
                                    db, path, form(x.getRequestURI().getRawQuery()), user));
                    return;
                }
                db.setAutoCommit(false);
                try {
                    Object result = oficina.gravar(db, path, f, user);
                    db.commit();
                    reply(x, 200, result);
                } catch (Exception e) {
                    db.rollback();
                    throw e;
                }
            }
        } catch (RegraNegocioException e) {
            reply(x, e.status(), Map.of("erro", e.getMessage()));
        } catch (SQLException e) {
            System.err.println("SQL: " + e.getSQLState() + " " + e.getMessage());
            String state = Objects.toString(e.getSQLState(), "");
            if (state.startsWith("23"))
                reply(
                        x,
                        409,
                        Map.of(
                                "erro",
                                "Registro duplicado ou referência inválida. Confira os dados."));
            else if (state.equals("40001"))
                reply(x, 409, Map.of("erro", "Outra operação está em andamento. Tente novamente."));
            else
                reply(
                        x,
                        500,
                        Map.of(
                                "erro",
                                "Não foi possível acessar o banco. Confira a conexão no"
                                        + " servidor."));
        } catch (Exception e) {
            e.printStackTrace();
            reply(x, 500, Map.of("erro", "Falha interna ao processar a operação."));
        }
    }
}
