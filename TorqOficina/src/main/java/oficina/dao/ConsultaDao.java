package oficina.dao;

import static oficina.dao.JdbcDao.one;
import static oficina.dao.JdbcDao.query;
import static oficina.validation.Validacao.id;
import static oficina.validation.Validacao.permit;

import oficina.exception.RegraNegocioException;
import oficina.model.Usuario;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/** Consultas de cadastros, ordens e relatórios usados pela interface. */
public final class ConsultaDao {
    private static final String ORDER_SELECT =
            "SELECT o.*,v.placa,v.marca,v.modelo,c.nome cliente,u.nome responsavel,"
                    + " COALESCE((SELECT SUM(quantidade*preco_unitario) FROM ordem_servicos WHERE"
                    + " ordem_id=o.id),0)+COALESCE((SELECT SUM(quantidade*preco_unitario) FROM"
                    + " ordem_pecas WHERE ordem_id=o.id),0) total FROM ordens o JOIN veiculos v ON"
                    + " v.id=o.veiculo_id JOIN clientes c ON c.id=v.cliente_id JOIN usuarios u ON"
                    + " u.id=o.responsavel_id ";

    public static Object read(Connection db, String path, Map<String, String> params, Usuario user)
            throws SQLException {
        return switch (path) {
            case "clientes" -> query(db, "SELECT * FROM clientes ORDER BY nome");
            case "veiculos" ->
                    query(
                            db,
                            "SELECT v.*,c.nome cliente FROM veiculos v JOIN clientes c ON"
                                    + " c.id=v.cliente_id ORDER BY v.id DESC");
            case "servicos" -> query(db, "SELECT * FROM servicos ORDER BY nome");
            case "pecas" ->
                    query(db, "SELECT *,estoque<=estoque_minimo alerta FROM pecas ORDER BY nome");
            case "usuarios" -> {
                permit(user, "administrador");
                yield query(db, "SELECT id,nome,email,perfil,ativo FROM usuarios ORDER BY nome");
            }
            case "responsaveis" ->
                    query(db, "SELECT id,nome,perfil FROM usuarios WHERE ativo=TRUE ORDER BY nome");
            case "ordens" -> query(db, ORDER_SELECT + "ORDER BY o.id DESC");
            case "ordem" -> {
                long order = id(params, "id");
                var detail = new LinkedHashMap<String, Object>();
                detail.put("ordem", one(db, ORDER_SELECT + "WHERE o.id=?", order));
                detail.put(
                        "servicos",
                        query(
                                db,
                                "SELECT i.*,s.nome FROM ordem_servicos i JOIN servicos s ON"
                                        + " s.id=i.servico_id WHERE i.ordem_id=?",
                                order));
                detail.put(
                        "pecas",
                        query(
                                db,
                                "SELECT i.*,p.nome,p.codigo FROM ordem_pecas i JOIN pecas p ON"
                                        + " p.id=i.peca_id WHERE i.ordem_id=?",
                                order));
                detail.put(
                        "historico",
                        query(
                                db,
                                "SELECT h.*,u.nome usuario FROM historico_ordens h JOIN usuarios u"
                                    + " ON u.id=h.usuario_id WHERE h.ordem_id=? ORDER BY h.id DESC",
                                order));
                yield detail;
            }
            case "movimentacoes" ->
                    query(
                            db,
                            "SELECT m.*,p.nome peca,p.codigo,u.nome usuario FROM movimentacoes m"
                                    + " JOIN pecas p ON p.id=m.peca_id JOIN usuarios u ON"
                                    + " u.id=m.usuario_id ORDER BY m.id DESC LIMIT 500");
            case "historico" ->
                    query(
                            db,
                            "SELECT h.*,u.nome usuario,v.placa FROM historico_ordens h JOIN"
                                + " usuarios u ON u.id=h.usuario_id JOIN ordens o ON"
                                + " o.id=h.ordem_id JOIN veiculos v ON v.id=o.veiculo_id ORDER BY"
                                + " h.id DESC LIMIT 500");
            case "auditoria" -> {
                permit(user, "administrador");
                yield query(
                        db,
                        "SELECT a.*,u.nome usuario FROM auditoria a JOIN usuarios u ON"
                                + " u.id=a.usuario_id ORDER BY a.id DESC LIMIT 500");
            }
            case "dashboard" ->
                    Map.of(
                            "resumo",
                                    one(
                                            db,
                                            "SELECT (SELECT COUNT(*) FROM ordens WHERE status NOT"
                                                + " IN ('entregue','cancelada'))"
                                                + " atendimentos,(SELECT COUNT(*) FROM ordens WHERE"
                                                + " status='em_execucao') em_execucao,(SELECT"
                                                + " COUNT(*) FROM ordens WHERE status='concluida')"
                                                + " prontas,(SELECT COUNT(*) FROM pecas WHERE"
                                                + " estoque<=estoque_minimo) alertas"),
                            "ordens",
                                    query(
                                            db,
                                            ORDER_SELECT
                                                    + "WHERE o.status NOT IN"
                                                    + " ('entregue','cancelada') ORDER BY o.id"
                                                    + " DESC"),
                            "alertas",
                                    query(
                                            db,
                                            "SELECT * FROM pecas WHERE estoque<=estoque_minimo"
                                                    + " ORDER BY estoque,nome"));
            default -> throw new RegraNegocioException(404, "Consulta não encontrada.");
        };
    }
}
