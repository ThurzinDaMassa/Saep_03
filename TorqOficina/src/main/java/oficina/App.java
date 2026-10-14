package oficina;

import static oficina.dao.JdbcDao.one;

import com.sun.net.httpserver.HttpServer;

import oficina.config.AppConfig;
import oficina.database.ConnectionFactory;
import oficina.database.DatabaseInitializer;
import oficina.security.SessaoService;
import oficina.service.OficinaService;
import oficina.web.ApiController;
import oficina.web.ArquivoController;

import java.awt.Desktop;
import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.concurrent.Executors;

/** Ponto de entrada do sistema. Conecta os serviços, o banco e o servidor HTTP. */
public final class App {

    private App() {}

    public static void main(String[] args) {
        try {
            iniciar(args);
        } catch (Exception error) {
            System.err.println("\nNão foi possível iniciar o Torq Oficina.");
            if (error instanceof BindException)
                System.err.println(
                        "A porta está em uso. Feche a outra instância ou altere server.port na"
                                + " configuração.");
            else if (error instanceof ClassNotFoundException)
                System.err.println(
                        "Driver do banco ausente. Extraia o pacote completo, incluindo a pasta"
                                + " lib.");
            else if (error instanceof SQLException sql && "28000".equals(sql.getSQLState()))
                System.err.println("Acesso ao banco negado. Confira db.user e db.password.");
            else if (error instanceof SQLException sql && sql.getErrorCode() == 90020)
                System.err.println(
                        "O banco local já está aberto. Feche a outra janela do sistema.");
            else System.err.println(error.getMessage());
            System.err.println(
                    "Para demonstração use INICIAR_SISTEMA.bat. Para MySQL confira"
                            + " config.properties e o servidor do banco.");
            System.exit(1);
        }
    }

    private static void iniciar(String[] args) throws Exception {
        AppConfig config = new AppConfig();
        var flags = Arrays.asList(args);
        if (flags.contains("--diagnostico")) {
            System.out.println("Java: " + System.getProperty("java.version"));
            System.out.println("Projeto: " + config.root());
            System.out.println("Configuração: " + config.arquivo());
            System.out.println(
                    "Banco: "
                            + (config.local()
                                    ? "H2 local em " + config.bancoLocal()
                                    : "MySQL / " + config.dbName()));
            System.out.println("Endereço: http://localhost:" + config.porta());
            try (var factory = new ConnectionFactory(config)) {
                System.out.println("Driver JDBC disponível.");
            }
            return;
        }
        var connections = new ConnectionFactory(config);
        if (config.local() || flags.contains("--init"))
            new DatabaseInitializer(config, connections).initialize();
        if (flags.contains("--init")) {
            connections.close();
            return;
        }
        connections.manterAberto();
        try (var db = connections.connect()) {
            one(db, "SELECT COUNT(*) n FROM usuarios");
        }
        if (!Files.isRegularFile(config.root().resolve("web/index.html")))
            throw new IOException("Interface ausente. Extraia a pasta web junto com o programa.");
        var sessoes = new SessaoService();
        var oficina = new OficinaService(sessoes);
        var api = new ApiController(connections, sessoes, oficina);
        var arquivos = new ArquivoController(config.root());
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", config.porta()), 0);
        var executor = Executors.newFixedThreadPool(12);
        server.createContext("/api/", api::handle);
        server.createContext("/", arquivos::handle);
        server.setExecutor(executor);
        Runtime.getRuntime()
                .addShutdownHook(
                        new Thread(
                                () -> {
                                    server.stop(1);
                                    executor.shutdown();
                                    try {
                                        connections.close();
                                    } catch (SQLException e) {
                                        System.err.println(
                                                "Falha ao encerrar banco: " + e.getMessage());
                                    }
                                }));
        server.start();
        String url = "http://localhost:" + config.porta();
        System.out.println("\nTORQ OFICINA - SISTEMA INICIADO");
        System.out.println("Endereço: " + url);
        System.out.println(
                "Banco: "
                        + (config.local()
                                ? "H2 local, com dados persistidos em " + config.bancoLocal()
                                : "MySQL / " + config.dbName()));
        System.out.println("Acesso de demonstração: admin@torq.local / Oficina@123");
        System.out.println("Mantenha esta janela aberta. Encerre com Ctrl+C.");
        if (flags.contains("--abrir") && Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(URI.create(url));
            } catch (Exception e) {
                System.out.println("Abra o endereço acima no navegador.");
            }
        }
    }
}
