package oficina.database;

import static oficina.dao.JdbcDao.number;
import static oficina.dao.JdbcDao.one;

import oficina.config.AppConfig;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Prepara as tabelas e popula apenas bancos sem usuários; preserva dados existentes. */
public final class DatabaseInitializer {

    private final AppConfig config;
    private final ConnectionFactory connections;

    public DatabaseInitializer(AppConfig config, ConnectionFactory connections) {
        this.config = config;
        this.connections = connections;
    }

    public void initialize() throws Exception {
        if (!config.local()) {
            try (var db = connections.connection(false);
                    var stmt = db.createStatement()) {
                stmt.execute(
                        "CREATE DATABASE IF NOT EXISTS `"
                                + config.dbName()
                                + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            }
        }
        try (var db = connections.connect()) {
            String file = config.local() ? "banco/04_estrutura_h2.sql" : "banco/02_estrutura.sql";
            String sql =
                    Files.readString(config.root().resolve(file), StandardCharsets.UTF_8)
                            .replaceAll("(?m)^--.*$", "");
            for (String statement : sql.split(";"))
                if (!statement.isBlank()) {
                    try (var st = db.createStatement()) {
                        st.execute(statement);
                    }
                }
            if (number(one(db, "SELECT COUNT(*) n FROM usuarios").get("n")) == 0) {
                db.setAutoCommit(false);
                try {
                    DadosDemonstracao.seed(db);
                    db.commit();
                } catch (Exception e) {
                    db.rollback();
                    throw e;
                }
            }
        }
        System.out.println("Banco preparado. Dados existentes preservados.");
    }
}
