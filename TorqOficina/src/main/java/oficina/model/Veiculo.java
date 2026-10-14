package oficina.model;

import static oficina.validation.Validacao.*;

import oficina.exception.RegraNegocioException;

import java.util.Locale;
import java.util.Map;

/** Veículo vinculado a um cliente, com placa normalizada e quilometragem válida. */
public record Veiculo(long clienteId, String placa, String marca, String modelo, int ano, int km) {
    public static Veiculo fromForm(Map<String, String> form) {
        long cliente = id(form, "cliente_id");
        String placa =
                text(form, "placa", 10).toUpperCase(Locale.ROOT).replace("-", "").replace(" ", "");
        if (!placa.matches("[A-Z]{3}[0-9][A-Z0-9][0-9]{2}"))
            throw new RegraNegocioException(400, "Placa inválida. Exemplo: ABC1D23 ou ABC1234.");
        return new Veiculo(
                cliente,
                placa,
                text(form, "marca", 50),
                text(form, "modelo", 60),
                integer(form, "ano", 1900, 2100),
                integer(form, "km", 0, Integer.MAX_VALUE));
    }
}
