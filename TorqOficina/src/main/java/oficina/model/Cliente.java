package oficina.model;

import static oficina.validation.Validacao.*;

import java.util.Map;

/** Dados validados para cadastrar ou editar um cliente. */
public record Cliente(String nome, String documento, String telefone, String email) {
    public static Cliente fromForm(Map<String, String> form) {
        return new Cliente(
                text(form, "nome", 120),
                text(form, "documento", 20),
                text(form, "telefone", 25),
                oficina.validation.Validacao.email(form, "email", false));
    }
}
