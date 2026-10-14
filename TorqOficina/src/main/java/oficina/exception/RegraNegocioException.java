package oficina.exception;

/** Erro de validação ou de permissão que a API pode apresentar ao usuário. */
public final class RegraNegocioException extends RuntimeException {

    private final int status;

    public RegraNegocioException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int status() {
        return status;
    }
}
