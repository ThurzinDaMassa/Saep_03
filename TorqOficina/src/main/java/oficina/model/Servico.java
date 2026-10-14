package oficina.model;

import static oficina.validation.Validacao.*;

import java.math.BigDecimal;
import java.util.Map;

/** Serviço do catálogo. Preços usam BigDecimal para preservar os centavos. */
public record Servico(String nome, String descricao, BigDecimal preco) {
    public static Servico fromForm(Map<String, String> form) {
        return new Servico(
                text(form, "nome", 100), text(form, "descricao", 255), money(form, "preco"));
    }
}
