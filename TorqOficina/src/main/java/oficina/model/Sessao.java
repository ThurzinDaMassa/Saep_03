package oficina.model;

/** Sessão mantida em memória; expira após oito horas ou ao sair. */
public record Sessao(long userId, String csrf, long expires) {}
