package oficina.web;

import static oficina.web.HttpUtil.headers;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Serve os arquivos da interface apenas dentro da pasta web. */
public final class ArquivoController {

    private final Path root;

    public ArquivoController(Path root) {
        this.root = root;
    }

    public void handle(HttpExchange x) throws IOException {
        headers(x);
        if (!x.getRequestMethod().equals("GET")) {
            x.sendResponseHeaders(405, -1);
            x.close();
            return;
        }
        var base = root.resolve("web").normalize();
        String request = x.getRequestURI().getPath();
        Path p =
                base.resolve(request.equals("/") ? "index.html" : request.substring(1)).normalize();
        if (!p.startsWith(base) || !Files.isRegularFile(p)) {
            x.sendResponseHeaders(404, -1);
            x.close();
            return;
        }
        String name = p.toString();
        String type =
                name.endsWith(".html")
                        ? "text/html"
                        : name.endsWith(".css")
                                ? "text/css"
                                : name.endsWith(".js")
                                        ? "application/javascript"
                                        : name.endsWith(".svg")
                                                ? "image/svg+xml"
                                                : "application/octet-stream";
        x.getResponseHeaders().set("Content-Type", type + "; charset=utf-8");
        x.getResponseHeaders().set("Cache-Control", "no-cache");
        byte[] b = Files.readAllBytes(p);
        x.sendResponseHeaders(200, b.length);
        try (var out = x.getResponseBody()) {
            out.write(b);
        }
    }
}
