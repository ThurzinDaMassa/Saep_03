package oficina.model;

import java.util.Map;

/** Usuário autenticado. Não carrega hash ou senha para as respostas da API. */
public record Usuario(long id, String nome, String email, Perfil perfil) {
    public static Usuario fromRow(Map<String, Object> row) {
        return new Usuario(
                ((Number) row.get("id")).longValue(),
                row.get("nome").toString(),
                row.get("email").toString(),
                Perfil.from(row.get("perfil").toString()));
    }

    public Map<String, Object> dadosPublicos() {
        return Map.of("id", id, "nome", nome, "email", email, "perfil", perfil.codigo());
    }
}
