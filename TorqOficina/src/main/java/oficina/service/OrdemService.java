package oficina.service;

import static oficina.dao.JdbcDao.insert;
import static oficina.dao.JdbcDao.number;
import static oficina.dao.JdbcDao.one;
import static oficina.dao.JdbcDao.query;
import static oficina.dao.JdbcDao.update;
import static oficina.dao.RegistroDao.history;
import static oficina.dao.RegistroDao.movement;
import static oficina.validation.Validacao.id;
import static oficina.validation.Validacao.integer;
import static oficina.validation.Validacao.permit;
import static oficina.validation.Validacao.text;

import oficina.exception.RegraNegocioException;
import oficina.model.OrdemServico;
import oficina.model.StatusOrdem;
import oficina.model.Usuario;

import java.sql.Connection;
import java.util.Map;

/** Abertura, edição, estados e itens das ordens, com bloqueios transacionais. */
public final class OrdemService {
    public long salvar(Connection db, String path, Map<String, String> f, Usuario user)
            throws Exception {
        long uid = user.id();
        long result = 0;

        switch (path) {
            case "ordens" -> {
                permit(user, "administrador", "atendente");
                OrdemServico novaOrdem = OrdemServico.fromForm(f);
                long vehicle = novaOrdem.veiculoId(), responsible = novaOrdem.responsavelId();
                one(db, "SELECT id FROM veiculos WHERE id=? FOR UPDATE", vehicle);
                one(db, "SELECT id FROM usuarios WHERE id=? AND ativo=TRUE", responsible);
                if (!query(
                                db,
                                "SELECT id FROM ordens WHERE veiculo_id=? AND status NOT IN"
                                        + " ('entregue','cancelada')",
                                vehicle)
                        .isEmpty())
                    throw new RegraNegocioException(
                            409, "Este veículo já possui uma ordem em atendimento.");
                result =
                        insert(
                                db,
                                "INSERT INTO ordens(veiculo_id,responsavel_id,relato)"
                                        + " VALUES(?,?,?)",
                                vehicle,
                                responsible,
                                novaOrdem.relato());
                history(db, result, uid, "Ordem aberta");
            }
            case "ordem/dados" -> {
                permit(user, "administrador", "atendente");
                long order = id(f, "ordem_id"), responsible = id(f, "responsavel_id");
                var current = one(db, "SELECT * FROM ordens WHERE id=? FOR UPDATE", order);
                if (StatusOrdem.from(current.get("status")).fechada())
                    throw new RegraNegocioException(
                            409, "Esta ordem está fechada para alterações.");
                var responsibleUser =
                        one(db, "SELECT nome FROM usuarios WHERE id=? AND ativo=TRUE", responsible);
                String report = text(f, "relato", 1000);
                update(
                        db,
                        "UPDATE ordens SET"
                                + " responsavel_id=?,relato=?,atualizada_em=CURRENT_TIMESTAMP WHERE"
                                + " id=?",
                        responsible,
                        report,
                        order);
                history(
                        db,
                        order,
                        uid,
                        "Dados do atendimento atualizados. Responsável: "
                                + responsibleUser.get("nome"));
                result = order;
            }
            case "ordem/status" -> {
                permit(user, "administrador", "atendente", "mecanico");
                long order = id(f, "ordem_id");
                var o = one(db, "SELECT * FROM ordens WHERE id=? FOR UPDATE", order);
                String before = o.get("status").toString(), next = text(f, "status", 30);
                if (!StatusOrdem.from(before).podeMudarPara(next))
                    throw new RegraNegocioException(409, "Mudança de status não permitida.");
                if (next.equals("entregue")) permit(user, "administrador", "atendente");
                if (next.equals("concluida")
                        && query(db, "SELECT id FROM ordem_servicos WHERE ordem_id=?", order)
                                .isEmpty())
                    throw new RegraNegocioException(
                            409, "Registre ao menos um serviço antes de concluir.");
                if (next.equals("cancelada")) {
                    permit(user, "administrador", "atendente");
                    for (var item :
                            query(
                                    db,
                                    "SELECT * FROM ordem_pecas WHERE ordem_id=? ORDER BY peca_id",
                                    order)) {
                        long part = number(item.get("peca_id"));
                        int qty = ((Number) item.get("quantidade")).intValue();
                        var p = one(db, "SELECT * FROM pecas WHERE id=? FOR UPDATE", part);
                        int stock = ((Number) p.get("estoque")).intValue() + qty;
                        update(db, "UPDATE pecas SET estoque=? WHERE id=?", stock, part);
                        movement(
                                db,
                                part,
                                order,
                                uid,
                                "devolucao",
                                qty,
                                stock,
                                "Cancelamento da OS #" + order);
                    }
                }
                update(
                        db,
                        "UPDATE ordens SET status=?,atualizada_em=CURRENT_TIMESTAMP WHERE id=?",
                        next,
                        order);
                history(db, order, uid, "Status: " + before + " para " + next);
                result = order;
            }
            case "ordem/servico", "ordem/peca" -> {
                permit(user, "administrador", "mecanico");
                long order = id(f, "ordem_id");
                var o = one(db, "SELECT * FROM ordens WHERE id=? FOR UPDATE", order);
                if (StatusOrdem.from(o.get("status")).fechada())
                    throw new RegraNegocioException(
                            409, "Esta ordem está fechada para alterações.");
                int qty = integer(f, "quantidade", 1, 10000);
                boolean part = path.endsWith("peca");
                long item = id(f, part ? "peca_id" : "servico_id");
                var data =
                        one(
                                db,
                                "SELECT * FROM "
                                        + (part ? "pecas" : "servicos")
                                        + " WHERE id=? FOR UPDATE",
                                item);
                if (part) {
                    int stock = ((Number) data.get("estoque")).intValue();
                    if (stock < qty)
                        throw new RegraNegocioException(
                                409, "Estoque insuficiente. Disponível: " + stock + ".");
                    stock -= qty;
                    update(db, "UPDATE pecas SET estoque=? WHERE id=?", stock, item);
                    movement(db, item, order, uid, "saida", qty, stock, "Consumo na OS #" + order);
                }
                result =
                        insert(
                                db,
                                "INSERT INTO "
                                        + (part
                                                ? "ordem_pecas(ordem_id,peca_id"
                                                : "ordem_servicos(ordem_id,servico_id")
                                        + ",quantidade,preco_unitario) VALUES(?,?,?,?)",
                                order,
                                item,
                                qty,
                                data.get("preco"));
                history(
                        db,
                        order,
                        uid,
                        (part ? "Peça" : "Serviço")
                                + " adicionado: "
                                + data.get("nome")
                                + " ("
                                + qty
                                + ")");
                update(db, "UPDATE ordens SET atualizada_em=CURRENT_TIMESTAMP WHERE id=?", order);
            }
            case "ordem/remover-servico", "ordem/remover-peca" -> {
                permit(user, "administrador", "mecanico");
                boolean part = path.endsWith("peca");
                long order = id(f, "ordem_id"), item = id(f, "item_id");
                var o = one(db, "SELECT * FROM ordens WHERE id=? FOR UPDATE", order);
                if (StatusOrdem.from(o.get("status")).fechada())
                    throw new RegraNegocioException(
                            409, "Esta ordem está fechada para alterações.");
                String table = part ? "ordem_pecas" : "ordem_servicos";
                var data =
                        one(
                                db,
                                "SELECT * FROM " + table + " WHERE id=? AND ordem_id=?",
                                item,
                                order);
                if (part) {
                    long p = number(data.get("peca_id"));
                    int qty = ((Number) data.get("quantidade")).intValue();
                    var stockRow = one(db, "SELECT estoque FROM pecas WHERE id=? FOR UPDATE", p);
                    int stock = ((Number) stockRow.get("estoque")).intValue() + qty;
                    update(db, "UPDATE pecas SET estoque=? WHERE id=?", stock, p);
                    movement(
                            db,
                            p,
                            order,
                            uid,
                            "devolucao",
                            qty,
                            stock,
                            "Remoção de item da OS #" + order);
                }
                update(db, "DELETE FROM " + table + " WHERE id=?", item);
                history(
                        db,
                        order,
                        uid,
                        (part ? "Peça" : "Serviço") + " removido da ordem, item #" + item);
                result = order;
            }

            default -> throw new RegraNegocioException(404, "Operação de ordem não encontrada.");
        }
        return result;
    }
}
