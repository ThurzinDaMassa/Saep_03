package oficina.dao;

import static oficina.dao.JdbcDao.insert;

import java.sql.Connection;
import java.sql.SQLException;

/** Persiste histórico das ordens, auditoria e movimentações de estoque. */
public final class RegistroDao {
    public static void history(Connection db, long order, long user, String event)
            throws SQLException {
        insert(
                db,
                "INSERT INTO historico_ordens(ordem_id,usuario_id,evento) VALUES(?,?,?)",
                order,
                user,
                event);
    }

    public static void audit(Connection db, long user, String event) throws SQLException {
        insert(db, "INSERT INTO auditoria(usuario_id,acao) VALUES(?,?)", user, event);
    }

    public static void movement(
            Connection db,
            long part,
            Long order,
            long user,
            String type,
            int qty,
            int balance,
            String reason)
            throws SQLException {
        insert(
                db,
                "INSERT INTO"
                    + " movimentacoes(peca_id,ordem_id,usuario_id,tipo,quantidade,saldo_apos,motivo)"
                    + " VALUES(?,?,?,?,?,?,?)",
                part,
                order,
                user,
                type,
                qty,
                balance,
                reason);
    }
}
