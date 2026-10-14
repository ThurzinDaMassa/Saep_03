package oficina.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

/** Carrega configuração UTF-8 e resolve caminhos a partir da pasta do projeto. */
public final class AppConfig {

    private final Path root;
    private final Path arquivo;
    private final Properties values = new Properties();

    public AppConfig() throws IOException {
        root = localizarProjeto();
        arquivo = localizarConfiguracao();
        if (!Files.isRegularFile(arquivo))
            throw new IOException(
                    "Configuração não encontrada: "
                            + arquivo
                            + ". Use a pasta TorqOficina como diretório de trabalho ou informe"
                            + " -Dtorq.root=CAMINHO.");
        try (var reader = Files.newBufferedReader(arquivo, StandardCharsets.UTF_8)) {
            values.load(reader);
        }
        if (!Set.of("mysql", "h2").contains(engine()))
            throw new IllegalArgumentException("db.engine deve ser mysql ou h2.");
    }

    private static Path localizarProjeto() {
        String explicit = System.getProperty("torq.root");
        if (explicit != null) return Path.of(explicit).toAbsolutePath().normalize();
        Path current = Path.of(".").toAbsolutePath().normalize();
        for (Path candidate : List.of(current, current.resolve("TorqOficina"))) {
            if (Files.isDirectory(candidate.resolve("web"))
                    && Files.isDirectory(candidate.resolve("banco"))) {
                return candidate;
            }
        }
        return current;
    }

    private Path localizarConfiguracao() {
        String explicit = System.getProperty("torq.config");
        if (explicit != null) return root.resolve(explicit).normalize();
        Path local = root.resolve("config.local.properties");
        return Files.isRegularFile(local) ? local : root.resolve("config.properties");
    }

    public Path root() {
        return root;
    }

    public Path arquivo() {
        return arquivo;
    }

    public String get(String key, String fallback) {
        return values.getProperty(key, fallback).trim();
    }

    public String password() {
        return values.getProperty("db.password", "");
    }

    public String engine() {
        return get("db.engine", "mysql").toLowerCase(Locale.ROOT);
    }

    public boolean local() {
        return engine().equals("h2");
    }

    public int porta() {
        int port = Integer.parseInt(get("server.port", "8080"));
        if (port < 1 || port > 65535)
            throw new IllegalArgumentException("server.port deve estar entre 1 e 65535.");
        return port;
    }

    public String dbName() {
        String name = get("db.name", "torq_oficina");
        if (!name.matches("[a-zA-Z0-9_]+"))
            throw new IllegalArgumentException("Nome de banco inválido.");
        return name;
    }

    public Path bancoLocal() {
        return root.resolve(get("db.path", "dados/torq_demo")).toAbsolutePath().normalize();
    }
}
