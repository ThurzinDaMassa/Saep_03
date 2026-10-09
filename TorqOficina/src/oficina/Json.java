package oficina;

import java.util.*;

/** Serialização de respostas. A entrada da API usa formulários URL encoded. */
public final class Json {
    private Json() {}
    public static String stringify(Object value) {
        if (value == null) return "null";
        if (value instanceof Boolean || value instanceof Number) return value.toString();
        if (value instanceof Map<?,?> map) {
            var parts = new ArrayList<String>();
            map.forEach((k,v) -> parts.add(stringify(k.toString()) + ":" + stringify(v)));
            return "{" + String.join(",", parts) + "}";
        }
        if (value instanceof Iterable<?> list) {
            var parts = new ArrayList<String>();
            list.forEach(v -> parts.add(stringify(v)));
            return "[" + String.join(",", parts) + "]";
        }
        var out = new StringBuilder("\"");
        for (char c : value.toString().toCharArray()) {
            switch(c) {
                case '"' -> out.append("\\\""); case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n"); case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> { if(c < 32) out.append(String.format("\\u%04x",(int)c)); else out.append(c); }
            }
        }
        return out.append('"').toString();
    }
}
