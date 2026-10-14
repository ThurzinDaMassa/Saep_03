package oficina.validation;

import oficina.exception.RegraNegocioException;
import oficina.model.Usuario;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Valida os campos recebidos da interface e as permissões dos perfis. */
public final class Validacao {
    public static String text(Map<String, String> f, String key, int max) {
        String v = f.getOrDefault(key, "").trim();
        if (v.isEmpty() || v.length() > max)
            throw new RegraNegocioException(
                    400, "Preencha corretamente: " + key + " (até " + max + " caracteres).");
        return v;
    }

    public static String optional(Map<String, String> f, String key, int max) {
        String v = f.getOrDefault(key, "").trim();
        if (v.length() > max) throw new RegraNegocioException(400, "Campo muito longo: " + key);
        return v;
    }

    public static int integer(Map<String, String> f, String key, int min, int max) {
        try {
            int v = Integer.parseInt(f.getOrDefault(key, ""));
            if (v < min || v > max) throw new NumberFormatException();
            return v;
        } catch (NumberFormatException e) {
            throw new RegraNegocioException(400, "Valor inválido para " + key + ".");
        }
    }

    public static long id(Map<String, String> f, String key) {
        return integer(f, key, 1, Integer.MAX_VALUE);
    }

    public static BigDecimal money(Map<String, String> f, String key) {
        try {
            var v = new BigDecimal(f.getOrDefault(key, "").replace(",", "."));
            if (v.signum() < 0 || v.scale() > 2 || v.compareTo(new BigDecimal("99999999.99")) > 0)
                throw new NumberFormatException();
            return v.setScale(2);
        } catch (Exception e) {
            throw new RegraNegocioException(400, "Preço inválido. Use até duas casas decimais.");
        }
    }

    public static String email(Map<String, String> f, String key, boolean required) {
        String v = required ? text(f, key, 150) : optional(f, key, 150);
        if (!v.isEmpty() && !v.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
            throw new RegraNegocioException(400, "E-mail inválido.");
        return v.toLowerCase(Locale.ROOT);
    }

    public static void permit(Usuario user, String... roles) {
        if (!Set.of(roles).contains(user.perfil().codigo()))
            throw new RegraNegocioException(403, "Seu perfil não tem permissão para esta ação.");
    }
}
