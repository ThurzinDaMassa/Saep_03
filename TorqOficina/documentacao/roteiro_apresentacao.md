# Roteiro de apresentação - Grupo 04

Distribuição sugerida. Cada integrante deve revisar o código e compreender todo o fluxo. A distribuição abaixo não comprova quem desenvolveu cada parte.

| Integrante | Parte sugerida | Explicação essencial |
| --- | --- | --- |
| Fernando Ian Harmel | Projeto, cliente e problema | Por que a oficina precisa integrar os registros e qual é o escopo |
| Arthur Ayyub Rodrigues | Requisitos, stack e banco | Cliente 1:N veículos, ordem 1:N itens, JDBC e chaves estrangeiras |
| Victor Nestor Prim | Autenticação, painel e cadastros | Perfis, sessão, validações e ligação de cliente com veículo |
| Arthur Krebs Delvons | Ordem, peças e estoque | Preço registrado, transação, baixa e devolução de peças |
| Larissa Beatriz Luz | Histórico, testes e encerramento | Evidências, falha corrigida e integridade dos saldos |

## Preparação

Inicie o MySQL, prepare um banco novo de demonstração e inicie o Java. Abra http://localhost:8080 e os slides. Mantenha o terminal do Java aberto. Confirme o acesso do administrador antes da apresentação. Tenha o PDF como alternativa de leitura dos slides.

## Demonstração prática completa

1. **Acesso:** entre como `admin@torq.local`, senha `Oficina@123`. Mostre o nome, a logomarca, o painel e os alertas.
2. **Cliente:** cadastre `Cliente Apresentação`, identificação `DEMO-APRES-01`, telefone `(47) 99900-0011` e e-mail `apresentacao@example.com`. Se já existir, utilize uma identificação nova.
3. **Veículo:** selecione o cliente cadastrado, placa `GPO4A04`, marca `Fiat`, modelo `Uno`, ano `2018`, quilometragem `80000`. A placa é fictícia. Em apresentações repetidas, use outra placa válida.
4. **Ordem:** clique em Nova ordem. Selecione o veículo, responsável `Mecânico` e relato `Troca de óleo e revisão preventiva`.
5. **Status:** avance de Aberta para Em execução.
6. **Mão de obra:** adicione o serviço `Troca de óleo`, quantidade 1, preço registrado de R$ 90,00.
7. **Materiais:** adicione `Óleo 5W30 1 litro`, quantidade 4, preço de R$ 42,90 por unidade. Adicione `Filtro de óleo`, quantidade 1, preço de R$ 35,00.
8. **Total:** confira R$ 90,00 de serviços e R$ 206,60 de peças. O valor registrado será **R$ 296,60**. Se os preços do catálogo forem alterados, refaça o cálculo com os preços mostrados na ordem.
9. **Regra de estoque:** tente adicionar 999 unidades de filtro. A operação deve exibir estoque insuficiente e preservar os itens e o saldo.
10. **Rastreabilidade:** abra Peças e estoque. O óleo terá 16 unidades e o filtro 2 unidades, considerando a carga inicial e nenhuma outra operação. Abra Movimentações para mostrar as duas saídas vinculadas à ordem. O filtro continuará com alerta de mínimo.
11. **Finalização:** volte à ordem, avance para Concluída e depois Entregue. A ordem sai do painel de atendimentos ativos e permanece na consulta e no histórico.
12. **Consulta final:** confira o histórico com abertura, itens e mudanças de status. Mostre a opção de imprimir a ordem. Termine com logout.

O administrador reúne permissões para executar todo o fluxo durante uma demonstração. Para demonstrar separação de funções, use o atendente para cadastro e entrega e o mecânico para itens e conclusão.

## Perguntas de revisão individual

- Como a interface conversa com o Java e como o Java usa JDBC?
- Por que um veículo precisa ter um cliente e por que uma ordem precisa ter um veículo?
- Por que preços e quantidades ficam nas tabelas de itens?
- O que ocorre se duas ordens tentarem consumir a última peça?
- Como um cancelamento devolve peças sem apagar o histórico?
- Onde o sistema verifica permissões além de esconder os botões?
- Por que a senha não aparece em texto puro no banco?
- Qual é a diferença entre serviço cadastrado e serviço realizado em uma ordem?
- Quais testes comprovaram as regras de estoque e quais ambientes foram usados?
- O que ainda precisaria ser feito para utilizar o sistema em uma oficina real?

## Checklist antes de apresentar

- [ ] Cada integrante consegue executar o sistema e explicar o fluxo.
- [ ] Todos conhecem as 11 tabelas e os três perfis.
- [ ] A regra da avaliação sobre assistência de IA foi considerada pelo grupo.
- [ ] A conexão foi validada no MySQL da máquina da apresentação.
- [ ] O JDK e o servidor MySQL estão ativos.
- [ ] Slides, scripts SQL, código e documentação estão disponíveis.
- [ ] A demonstração completa foi ensaiada pelos cinco integrantes.
