package oficina.database;

import oficina.config.AppConfig;

import java.nio.file.Files;
import java.sql.*;

/** Cada operação recebe sua conexão JDBC; H2 mantém uma conexão de apoio durante a execução. */
public final class ConnectionFactory implements AutoCloseable {
    private final AppConfig config;
    private Connection apoio;

    public ConnectionFactory(AppConfig config) throws Exception {
        this.config = config;
        Class.forName(config.local() ? "org.h2.Driver" : "com.mysql.cj.jdbc.Driver");
        if (config.local()) {
            String path = config.bancoLocal().toString();
            if (path.contains(";"))
                throw new IllegalArgumentException(
                        "A pasta do banco não pode conter ponto e vírgula.");
            Files.createDirectories(config.bancoLocal().getParent());
        }
    }

    public Connection connect() throws SQLException {
        return connection(true);
    }

    public Connection connection(boolean withDatabase) throws SQLException {
        if (config.local()) {
            String url =
                    "jdbc:h2:file:"
                            + config.bancoLocal().toString().replace('\\', '/')
                            + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;LOCK_TIMEOUT=10000";
            return DriverManager.getConnection(url, config.get("db.user", "sa"), config.password());
        }
        String url =
                "jdbc:mysql://"
                        + config.get("db.host", "127.0.0.1")
                        + ":"
                        + config.get("db.port", "3306")
                        + "/"
                        + (withDatabase ? config.dbName() : "")
                        + "?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8&connectionTimeZone=LOCAL&connectTimeout=5000&socketTimeout=15000";
        return DriverManager.getConnection(url, config.get("db.user", "root"), config.password());
    }

    public void manterAberto() throws SQLException {
        if (config.local()) apoio = connect();
    }

    @Override
    public void close() throws SQLException {
        if (apoio != null) apoio.close();
    }
}
