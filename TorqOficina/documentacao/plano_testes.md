# Plano e relatório de testes

Ambiente: Java 23, compilação para Java 17, Connector/J 8.4.0, MariaDB 10.4.32 do XAMPP e Edge. Banco isolado de testes. Destino do projeto: MySQL 8. A validação em MySQL 8 dedicado permanece pendente.

Resultado final: **44/44 cenários aprovados**. A execução automatizada encontra-se em testes/resultado_api.json.

| ID | Precondição e procedimento | Resultado esperado | Obtido |
| --- | --- | --- | --- |
| CT01 | Sem sessão. Consultar /api/clientes | HTTP 401 e ausência de dados | APROVADO |
| CT02 | Conta admin ativa. Informar senha errada | HTTP 401 e login negado | APROVADO |
| CT03 | Conta admin ativa. Entrar com admin e consultar sessão | Sessão válida, cookie HttpOnly e SameSite, sem hash na resposta | APROVADO |
| CT04 | Sessão válida. Cadastrar cliente sem X-CSRF-Token | HTTP 403, sem gravação | APROVADO |
| CT05 | Contas iniciais ativas. Entrar como atendente e mecânico | Perfil correspondente em cada sessão | APROVADO |
| CT06 | Sessões por perfil. Atendente consulta usuários e mecânico cria cliente | HTTP 403 em ambas as operações | APROVADO |
| CT07 | Atendente autenticado. Cadastrar cliente válido e consultar lista | ID criado e cliente listado | APROVADO |
| CT08 | Cliente do teste criado. Repetir documento do cliente | HTTP 409 e quantidade de clientes preservada | APROVADO |
| CT09 | Sessão autenticada. Enviar nome vazio e e-mail sem formato | HTTP 400 | APROVADO |
| CT10 | Cliente do teste criado. Editar nome usando o mesmo ID | Mesmo ID e novo nome persistido | APROVADO |
| CT11 | Sessão autenticada. Enviar placa inválida e cliente inexistente | HTTP 400 e HTTP 404 respectivamente | APROVADO |
| CT12 | Cliente do teste criado. Cadastrar veículo e atualizar km | Cliente vinculado e quilometragem alterada | APROVADO |
| CT13 | Veículo do teste criado. Cadastrar outro veículo com mesma placa | HTTP 409 | APROVADO |
| CT14 | Administrador autenticado. Testar preço negativo e criar serviço de R$ 50 | Negativo rejeitado e serviço válido persistido | APROVADO |
| CT15 | Administrador autenticado. Criar peça com saldo inicial 5 | Entrada inicial com saldo após 5 | APROVADO |
| CT16 | Peça com saldo 5. Entrada 2 e saída 2 com motivos | Saldo final 5 e autor/motivo registrados | APROVADO |
| CT17 | Peça com saldo 5. Tentar saída 999 | HTTP 409 e saldo 5 preservado | APROVADO |
| CT18 | Veículo disponível. Abrir ordem para veículo e responsável | Status aberta e vínculos corretos | APROVADO |
| CT19 | Veículo com ordem ativa. Tentar segunda ordem | HTTP 409 | APROVADO |
| CT20 | Ordem aberta. Tentar alterar para entregue | HTTP 409 | APROVADO |
| CT21 | Ordem sem serviço. Avançar execução e tentar concluir | HTTP 409 na conclusão | APROVADO |
| CT22 | Ordem em execução e peça com saldo 5. Adicionar serviço R$ 50 e 3 peças de R$ 20 | Total R$ 110 e saldo 2 | APROVADO |
| CT23 | Peça no mínimo 2. Consultar painel | Peça presente nos alertas | APROVADO |
| CT24 | Peça com saldo 2. Tentar consumir 5 unidades | HTTP 409, sem novo item ou movimento | APROVADO |
| CT25 | Item de peça incluído. Remover item com quantidade 3 | Saldo 5 e item removido | APROVADO |
| CT26 | Serviço já incluído na ordem. Alterar preço do catálogo para R$ 99 | Preço da ordem permanece R$ 50 | APROVADO |
| CT27 | Ordem com serviço. Mecânico conclui e atendente entrega | Mecânico não entrega. Atendente entrega | APROVADO |
| CT28 | Ordem entregue. Tentar adicionar item e reabrir | HTTP 409 em ambas as operações | APROVADO |
| CT29 | Nova ordem com duas peças. Cancelar e repetir cancelamento | Saldo devolvido uma vez e segunda tentativa rejeitada | APROVADO |
| CT30 | Duas ordens e peça com saldo 1. Executar dois consumos simultâneos | Um sucesso, um HTTP 409 e saldo zero | APROVADO |
| CT31 | Administrador e nova conta. Autenticar, desativar conta e tentar operar | Login e sessão antiga rejeitados com HTTP 401 | APROVADO |
| CT32 | Administrador autenticado. Tentar desativar a própria conta | HTTP 409 | APROVADO |
| CT33 | Sessão válida. Consultar ID inexistente e usar quantidade 1,5 | HTTP 404 e HTTP 400 | APROVADO |
| CT34 | Administrador autenticado. Cadastrar nome contendo aspas e expressão SQL | Texto armazenado como dado e tabelas intactas | APROVADO |
| CT35 | Fluxo completo registrado. Consultar histórico e auditoria | Eventos com usuário, data e ações de ordem | APROVADO |
| CT36 | Sessão de mecânico válida. Logout e consulta /api/me | HTTP 401 após logout | APROVADO |
| CT37 | Ordem em execução e administrador. Editar responsável e relato | Dados atualizados; veículo, itens e estoque preservados | APROVADO |
| CT38 | Mecânico autenticado. Tentar editar os dados da ordem | HTTP 403 | APROVADO |
| CT39 | Atendente autenticado. Editar ordem e consultar histórico e auditoria | Autor e alteração registrados | APROVADO |
| CT40 | Ordem ativa. Editar com responsável inexistente | HTTP 404 e estado preservado | APROVADO |
| CT41 | Ordem ativa. Enviar relato vazio | HTTP 400 e estado preservado | APROVADO |
| CT42 | Sessão válida. Editar sem token CSRF | HTTP 403 | APROVADO |
| CT43 | Ordem concluída. Tentar alterar responsável e relato | HTTP 409 e estado preservado | APROVADO |
| CT44 | Sessões de atendente e mecânico. Consultar auditoria | HTTP 403 | APROVADO |

## Interface e banco

Navegador: 26 cenários de integração, 12 telas, busca global, paginação, filtros, exportação, confirmações e fluxo completo. Layouts de 360, 390, 768 e 1440 px, sem erros de JavaScript. Banco via JDBC: 11 tabelas, hashes PBKDF2, ausência de estoque negativo, conciliação de saldos com movimentos e ausência de ordens ativas duplicadas.

## Defeito corrigido

Uma sessão pertencente a usuário desativado recebia HTTP 404. O servidor passou a revalidar o usuário ativo, remover a sessão e retornar HTTP 401. CT31 foi repetido com aprovação.

## Limites

Não foram executados testes de carga, em produção, em outros navegadores ou em MySQL 8 dedicado. Os cenários automatizados geram dados apenas na instância separada indicada pelo operador.