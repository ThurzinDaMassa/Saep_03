package oficina.model;

/** Perfis reconhecidos pelo sistema. */
public enum Perfil {
    ADMINISTRADOR("administrador"),
    ATENDENTE("atendente"),
    MECANICO("mecanico");
    private final String codigo;

    Perfil(String codigo) {
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }

    public static Perfil from(String codigo) {
        for (Perfil perfil : values()) if (perfil.codigo.equals(codigo)) return perfil;
        throw new IllegalArgumentException("Perfil inválido: " + codigo);
    }
}
