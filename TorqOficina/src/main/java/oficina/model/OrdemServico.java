package oficina.model;

import static oficina.validation.Validacao.*;

import java.util.Map;

/** Dados necessários para abrir uma ordem de serviço. */
public record OrdemServico(long veiculoId, long responsavelId, String relato) {
    public static OrdemServico fromForm(Map<String, String> form) {
        return new OrdemServico(
                id(form, "veiculo_id"), id(form, "responsavel_id"), text(form, "relato", 1000));
    }
}
