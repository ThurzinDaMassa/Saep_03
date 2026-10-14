package oficina.web;

import com.sun.net.httpserver.HttpExchange;

import oficina.util.Json;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Leitura de formulários e respostas HTTP com cabeçalhos de segurança. */
public final class HttpUtil {
    public static Map<String, String> form(String body) {
        var data = new HashMap<String, String>();
        if (body == null || body.isEmpty()) return data;
        for (String item : body.split("&")) {
            var pair = item.split("=", 2);
            data.put(
                    URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                    pair.length > 1 ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : "");
        }
        return data;
    }

    public static void headers(HttpExchange x) {
        x.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        x.getResponseHeaders().set("X-Frame-Options", "DENY");
        x.getResponseHeaders().set("Referrer-Policy", "same-origin");
        x.getResponseHeaders()
                .set(
                        "Content-Security-Policy",
                        "default-src 'self'; img-src 'self' data:; style-src 'self'; script-src"
                                + " 'self'; frame-ancestors 'none'; base-uri 'self'; form-action"
                                + " 'self'");
    }

    public static void reply(HttpExchange x, int code, Object data) throws IOException {
        headers(x);
        x.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        x.getResponseHeaders().set("Cache-Control", "no-store");
        byte[] b = Json.stringify(data).getBytes(StandardCharsets.UTF_8);
        x.sendResponseHeaders(code, b.length);
        try (var out = x.getResponseBody()) {
            out.write(b);
        }
    }
}
