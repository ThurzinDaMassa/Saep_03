package oficina.service;

import static oficina.dao.RegistroDao.audit;

import oficina.dao.ConsultaDao;
import oficina.exception.RegraNegocioException;
import oficina.model.Usuario;
import oficina.security.SessaoService;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

/** Encaminha operações aos serviços e registra sua auditoria na mesma transação. */
public final class OficinaService {

    private final CadastroService cadastros;
    private final OrdemService ordens = new OrdemService();
    private final EstoqueService estoque = new EstoqueService();

    public OficinaService(SessaoService sessoes) {
        cadastros = new CadastroService(sessoes);
    }

    public Object consultar(Connection db, String path, Map<String, String> params, Usuario user)
            throws SQLException {
        return ConsultaDao.read(db, path, params, user);
    }

    public Object gravar(Connection db, String path, Map<String, String> form, Usuario user)
            throws Exception {
        long result =
                switch (path) {
                    case "clientes", "veiculos", "servicos", "pecas", "usuarios" ->
                            cadastros.salvar(db, path, form, user);
                    case "ordens",
                                    "ordem/dados",
                                    "ordem/status",
                                    "ordem/servico",
                                    "ordem/peca",
                                    "ordem/remover-servico",
                                    "ordem/remover-peca" ->
                            ordens.salvar(db, path, form, user);
                    case "estoque/movimentar" -> estoque.salvar(db, path, form, user);
                    default -> throw new RegraNegocioException(404, "Operação não encontrada.");
                };
        boolean edit = form.containsKey("id") && !form.get("id").isBlank();
        audit(db, user.id(), path + (edit ? " alterado #" : " registrado #") + result);
        return Map.of("ok", true, "id", result);
    }
}
