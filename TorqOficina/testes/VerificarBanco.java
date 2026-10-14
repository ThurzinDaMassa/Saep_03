import oficina.config.AppConfig;
import oficina.database.ConnectionFactory;

import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** Verificação direta das invariantes do banco por JDBC. */
public class VerificarBanco {
    public static void main(String[] args) throws Exception {
        if (args.length != 1)
            throw new IllegalArgumentException(
                    "Informe o arquivo de configuração do banco de testes.");
        System.setProperty("torq.config", args[0]);
        AppConfig config = new AppConfig();
        try (var factory = new ConnectionFactory(config);
                var db = factory.connect();
                var st = db.createStatement()) {
            System.out.println(
                    "Banco validado: "
                            + db.getMetaData().getDatabaseProductName()
                            + " "
                            + db.getMetaData().getDatabaseProductVersion());
            check(
                    st,
                    "Saldos conciliados com movimentações",
                    "SELECT COUNT(*) FROM pecas p WHERE p.estoque <> COALESCE((SELECT SUM(CASE WHEN"
                        + " m.tipo='saida' THEN -m.quantidade ELSE m.quantidade END) FROM"
                        + " movimentacoes m WHERE m.peca_id=p.id),0)");
            check(st, "Estoque não negativo", "SELECT COUNT(*) FROM pecas WHERE estoque<0");
            check(
                    st,
                    "Senhas com PBKDF2",
                    "SELECT COUNT(*) FROM usuarios WHERE senha_hash NOT LIKE '210000$%$%'");
            check(
                    st,
                    "Veículos sem ordens ativas duplicadas",
                    "SELECT COUNT(*) FROM (SELECT veiculo_id FROM ordens WHERE status NOT IN"
                        + " ('entregue','cancelada') GROUP BY veiculo_id HAVING COUNT(*)>1) a");
            try (var count =
                    db.prepareStatement(
                            "SELECT COUNT(*) FROM information_schema.tables WHERE"
                                + " table_schema=?")) {
                count.setString(1, config.local() ? db.getSchema() : db.getCatalog());
                try (var rs = count.executeQuery()) {
                    rs.next();
                    if (rs.getInt(1) != 11)
                        throw new AssertionError(
                                "Esperadas 11 tabelas; encontradas " + rs.getInt(1));
                    System.out.println("APROVADO: 11 tabelas relacionais");
                }
            }
        }
    }

    static void check(Statement st, String label, String sql) throws Exception {
        try (var rs = st.executeQuery(sql)) {
            rs.next();
            if (rs.getInt(1) != 0)
                throw new AssertionError(label + ": " + rs.getInt(1) + " inconsistências");
            System.out.println("APROVADO: " + label);
        }
    }
}
