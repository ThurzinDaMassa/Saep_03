package oficina.service;

import static oficina.dao.JdbcDao.one;
import static oficina.dao.JdbcDao.update;
import static oficina.dao.RegistroDao.movement;
import static oficina.validation.Validacao.id;
import static oficina.validation.Validacao.integer;
import static oficina.validation.Validacao.permit;
import static oficina.validation.Validacao.text;

import oficina.exception.RegraNegocioException;
import oficina.model.Usuario;

import java.sql.Connection;
import java.util.Map;
import java.util.Set;

/** Movimentação manual de estoque com validação e registro do saldo. */
public final class EstoqueService {
    public long salvar(Connection db, String path, Map<String, String> f, Usuario user)
            throws Exception {
        long uid = user.id();
        long result = 0;

        switch (path) {
            case "estoque/movimentar" -> {
                permit(user, "administrador");
                long part = id(f, "peca_id");
                int qty = integer(f, "quantidade", 1, 1000000);
                String type = text(f, "tipo", 20), reason = text(f, "motivo", 255);
                if (!Set.of("entrada", "saida").contains(type))
                    throw new RegraNegocioException(400, "Tipo de movimentação inválido.");
                var p = one(db, "SELECT * FROM pecas WHERE id=? FOR UPDATE", part);
                int stock = ((Number) p.get("estoque")).intValue();
                if (type.equals("saida") && qty > stock)
                    throw new RegraNegocioException(409, "Estoque insuficiente.");
                stock += type.equals("entrada") ? qty : -qty;
                if (stock > 100000000)
                    throw new RegraNegocioException(400, "Saldo acima do limite.");
                update(db, "UPDATE pecas SET estoque=? WHERE id=?", stock, part);
                movement(db, part, null, uid, type, qty, stock, reason);
                result = part;
            }

            default -> throw new RegraNegocioException(404, "Operação de estoque não encontrada.");
        }
        return result;
    }
}
