package oficina.model;

import java.util.Set;

/** Estados da ordem e transições permitidas pela operação da oficina. */
public enum StatusOrdem {
    ABERTA("aberta"),
    EM_EXECUCAO("em_execucao"),
    AGUARDANDO_PECAS("aguardando_pecas"),
    CONCLUIDA("concluida"),
    ENTREGUE("entregue"),
    CANCELADA("cancelada");
    private final String codigo;

    StatusOrdem(String codigo) {
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }

    public boolean fechada() {
        return this == CONCLUIDA || this == ENTREGUE || this == CANCELADA;
    }

    public boolean podeMudarPara(String destino) {
        return switch (this) {
            case ABERTA -> Set.of("em_execucao", "aguardando_pecas", "cancelada").contains(destino);
            case EM_EXECUCAO ->
                    Set.of("aguardando_pecas", "concluida", "cancelada").contains(destino);
            case AGUARDANDO_PECAS -> Set.of("em_execucao", "cancelada").contains(destino);
            case CONCLUIDA -> destino.equals("entregue");
            case ENTREGUE, CANCELADA -> false;
        };
    }

    public static StatusOrdem from(Object codigo) {
        for (StatusOrdem status : values())
            if (status.codigo.equals(codigo.toString())) return status;
        throw new IllegalArgumentException("Status inválido: " + codigo);
    }
}
