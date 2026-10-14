package oficina.model;

import static oficina.validation.Validacao.*;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;

/** Cadastro da peça. O saldo só muda por movimentações rastreáveis. */
public record Peca(String codigo, String nome, BigDecimal preco, int estoqueMinimo) {
    public static Peca fromForm(Map<String, String> form) {
        return new Peca(
                text(form, "codigo", 30).toUpperCase(Locale.ROOT),
                text(form, "nome", 100),
                money(form, "preco"),
                integer(form, "estoque_minimo", 0, 1000000));
    }
}
