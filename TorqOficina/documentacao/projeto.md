# Torq Oficina

## Identificação

Grupo 04 - Oficina Mecânica. Cliente fictício: **Oficina Rota 4**. Unidade curricular: **Testes de Sistemas**. Docente: **Éder Gabriel Rigueira Campos**. Curso: Técnico em Desenvolvimento de Sistemas - Integrado ao EM. Enunciado destinado às turmas 303 e 304. A turma específica do grupo não foi informada.

Integrantes: Fernando Ian Harmel, Arthur Ayyub Rodrigues, Victor Nestor Prim, Arthur Krebs Delvons e Larissa Beatriz Luz.

## 1. Cliente, problema e proposta

A Oficina Rota 4 é uma empresa fictícia de manutenção de veículos. O cenário fornecido no enunciado descreve registros manuais de clientes, veículos, serviços, peças, ordens e estoque. A separação desses registros dificulta localizar informações, acompanhar reparos e conhecer a disponibilidade dos materiais.

A proposta é integrar o atendimento em uma aplicação local. O atendente registra o cliente e seu veículo, abre uma ordem e define o responsável. O mecânico registra os serviços e as peças usados. O administrador acompanha os atendimentos e repõe o estoque. A entrega encerra o fluxo, preservando os registros para consulta.

O produto implementa o escopo mínimo do PDF. Pagamentos, emissão fiscal, agendamento, fornecedores e comunicação por e-mail não integram esta versão. Os registros e as contas de demonstração são fictícios.

## 2. Usuários, dados e operações

| Perfil | Necessidade | Operações autorizadas |
| --- | --- | --- |
| Administrador | Organizar e supervisionar a oficina | Consultas, cadastros, usuários, catálogos, estoque e todas as ações das ordens |
| Atendente | Receber clientes e entregar veículos | Clientes e veículos, abertura de ordens, mudanças de status, cancelamento e entrega |
| Mecânico | Registrar a manutenção realizada | Consultas, inclusão e remoção de serviços e peças, execução e conclusão de ordens |

Todos os perfis podem consultar clientes, veículos, serviços, peças, ordens, movimentações e histórico. Apenas o administrador consulta e altera usuários e consulta a auditoria. Administrador e atendente editam responsável e relato de ordens em andamento. O mecânico não entrega nem cancela ordens. O atendente não registra itens nem movimenta estoque. O Java verifica as permissões em cada operação, além dos controles visuais da interface.

Dados principais: identificação e contato de clientes, características do veículo, usuário responsável, relato, situação da ordem, preços, quantidades, saldo de peças e eventos com data e autor.

## 3. Requisitos funcionais

| ID | Requisito | Critério de aceitação |
| --- | --- | --- |
| RF01 | Autenticar usuários ativos | Login válido abre o painel e credenciais inválidas são rejeitadas |
| RF02 | Controlar usuários e perfis | Administrador cadastra, edita e desativa contas e define permissões |
| RF03 | Cadastrar, alterar e consultar clientes | Nome, documento único, telefone e e-mail opcional persistem no banco |
| RF04 | Cadastrar, alterar e consultar veículos | Cada veículo pertence a um cliente e possui placa única |
| RF05 | Manter catálogo de serviços | Administrador informa descrição e preço de referência |
| RF06 | Manter catálogo de peças | Código único, preço, saldo inicial e estoque mínimo são registrados |
| RF07 | Abrir e consultar ordens | Veículo, responsável e relato ficam vinculados à ordem |
| RF08 | Acompanhar situação do atendimento | Mudanças respeitam as etapas permitidas e ficam no histórico |
| RF09 | Registrar e remover serviços realizados | Ordem aberta admite quantidade e preço registrado do serviço |
| RF10 | Registrar e remover peças utilizadas | Consumo baixa estoque e remoção devolve a quantidade |
| RF11 | Calcular valor da ordem | Total soma quantidades multiplicadas pelos preços dos itens |
| RF12 | Registrar movimentações | Entrada, saída e devolução guardam motivo, autor, quantidade e saldo |
| RF13 | Exibir alerta de estoque mínimo | Peça com saldo menor ou igual ao mínimo aparece no painel |
| RF14 | Preservar histórico de atendimento | Abertura, itens e status são consultáveis com autor e data |
| RF15 | Apresentar painel e buscas | Painel mostra veículos ativos e listas filtram os registros |
| RF16 | Imprimir ordem e encerrar sessão | Navegador imprime os detalhes e logout invalida o acesso |
| RF17 | Buscar globalmente e navegar por endereço | Ctrl+K localiza páginas, clientes, veículos e ordens. Recarregar mantém os detalhes pelo endereço |
| RF18 | Filtrar, ordenar, paginar e exportar | Consultas preservam filtros e exportam todos os registros filtrados em CSV |
| RF19 | Editar dados de atendimento aberto | Administrador e atendente alteram responsável e relato com registro no histórico. Ordens fechadas rejeitam edição |
| RF20 | Consultar auditoria administrativa | Apenas administrador consulta os 500 últimos acessos e alterações registrados |

## 4. Requisitos não funcionais

| ID | Requisito | Implementação / verificação |
| --- | --- | --- |
| RNF01 | Persistência relacional | MySQL, 11 tabelas InnoDB, PK, FK, UNIQUE e CHECK |
| RNF02 | Proteção de senhas e acesso | PBKDF2-HMAC-SHA256, salt aleatório, sessão HttpOnly e verificação de perfil |
| RNF03 | Consistência das operações | Transações JDBC e bloqueio de linhas em ordens e peças |
| RNF04 | Validação das entradas | Campos obrigatórios, limites, preços, quantidades, placas e e-mails no servidor |
| RNF05 | Interface organizada | Navegação por função, formulários rotulados, mensagens e layout responsivo |
| RNF06 | Execução local e portabilidade | Java 17+, navegador, driver incluído e configuração externa do banco |
| RNF07 | Rastreabilidade | Histórico por ordem, movimentações e auditoria administrativa |
| RNF08 | Funcionamento sem recursos web externos | HTML, CSS, JavaScript e SVG servidos localmente, sem CDN |

O sistema limita a requisição a 64 KiB e a sessão a oito horas. Consultas de histórico, movimentações e auditoria exibem os 500 registros mais recentes. A versão prioriza demonstração local. Não foram realizados testes de carga, avaliação em produção ou implantação com HTTPS.

## 5. Regras de negócio

| ID | Regra |
| --- | --- |
| RN01 | Documento do cliente, placa, código da peça e e-mail do usuário são únicos |
| RN02 | Veículo sempre pertence a um cliente existente. Após gerar histórico, não pode trocar de cliente |
| RN03 | Um veículo só pode ter uma ordem ativa. Concluída ainda é ativa até a entrega |
| RN04 | Apenas usuários ativos podem autenticar e operar |
| RN05 | Quantidades de itens e movimentações são inteiras e maiores que zero. Preços não são negativos |
| RN06 | Estoque nunca pode ficar negativo. Consumo acima do saldo é rejeitado |
| RN07 | Preço unitário fica registrado no item. Alterar o catálogo não recalcula ordens anteriores |
| RN08 | Toda alteração de saldo gera movimentação na mesma transação |
| RN09 | Remover uma peça da ordem devolve sua quantidade. Cancelar devolve os itens restantes uma única vez |
| RN10 | Conclusão exige ao menos um serviço registrado |
| RN11 | Ordens concluídas, entregues e canceladas bloqueiam alterações nos itens |
| RN12 | Apenas administrador e atendente entregam ou cancelam uma ordem |
| RN13 | Saldo menor ou igual ao mínimo gera alerta |
| RN14 | Administrador não pode desativar ou rebaixar a própria conta. Deve permanecer um administrador ativo |
| RN15 | Cadastros históricos são preservados. Não há exclusão física de clientes, veículos ou ordens |
| RN16 | Relato e responsável só podem ser editados antes da conclusão, entrega ou cancelamento |
| RN17 | Exportações seguem os filtros da consulta e neutralizam campos que possam ser interpretados como fórmulas |

Fluxo de status permitido:

| Situação atual | Próximas situações |
| --- | --- |
| Aberta | Em execução, Aguardando peças ou Cancelada |
| Em execução | Aguardando peças, Concluída ou Cancelada |
| Aguardando peças | Em execução ou Cancelada |
| Concluída | Entregue |
| Entregue | Nenhuma |
| Cancelada | Nenhuma |

A situação do veículo deriva da ordem ativa, mantendo os registros consistentes. Itens de ordens canceladas permanecem no histórico, com devolução das peças ao estoque.

## 6. Modelagem do banco

Todas as tabelas usam `id BIGINT AUTO_INCREMENT` como chave primária. Valores monetários usam `DECIMAL(10,2)`. O esquema completo está em `banco/02_estrutura.sql` e o diagrama em formato Mermaid está em `documentacao/modelo_er.mmd`.

| Entidade | Atributos principais além de id | Chaves / relacionamentos |
| --- | --- | --- |
| usuarios | nome, email, senha_hash, perfil, ativo, criado_em | email UNIQUE. Referenciada por ordens e registros de operações |
| clientes | nome, documento, telefone, email, criado_em | documento UNIQUE. Cliente 1:N veículos |
| veiculos | cliente_id, placa, marca, modelo, ano, km | cliente_id FK. placa UNIQUE. Veículo 1:N ordens |
| servicos | nome, descricao, preco | Serviço 1:N itens de serviço |
| pecas | codigo, nome, preco, estoque, estoque_minimo | codigo UNIQUE. Peça 1:N itens e movimentações |
| ordens | veiculo_id, responsavel_id, relato, status, aberta_em, atualizada_em | FKs para veículo e usuário. Ordem 1:N itens e histórico |
| ordem_servicos | ordem_id, servico_id, quantidade, preco_unitario | FKs para ordem e serviço. Resolve associação N:N |
| ordem_pecas | ordem_id, peca_id, quantidade, preco_unitario | FKs para ordem e peça. Resolve associação N:N |
| movimentacoes | peca_id, ordem_id, usuario_id, tipo, quantidade, saldo_apos, motivo, criado_em | FKs para peça, ordem opcional e usuário |
| historico_ordens | ordem_id, usuario_id, evento, criado_em | FKs para ordem e usuário |
| auditoria | usuario_id, acao, criado_em | FK para usuário |

Os dados de cliente não se repetem na ordem: o Java obtém o cliente por meio do veículo. As tabelas de itens armazenam quantidade e preço do momento da inclusão. Movimentações manuais podem não ter uma ordem vinculada. Registros de histórico permanecem vinculados à ordem original.

Índices em status e veículo apoiam as consultas de atendimento. Chaves estrangeiras impedem referências órfãs. Restrições CHECK complementam a validação Java. No MySQL, use versão 8.0.16 ou posterior para que CHECK seja efetivamente aplicado. A aplicação verifica as mesmas regras fundamentais antes de gravar.

## 7. Criação e população

A preparação Java `--init` cria o banco configurado e aplica o esquema. Se a tabela de usuários estiver vazia, uma transação cria três usuários, três clientes, três veículos, três serviços, quatro peças e três ordens em situações diferentes. Há um serviço na primeira ordem, registros de histórico e as movimentações iniciais do estoque.

O administrador utiliza `admin@torq.local / Oficina@123`, o atendente `atendimento@torq.local / Atende@123` e o mecânico `mecanico@torq.local / Mecanico@123`. Os documentos de demonstração são identificadores fictícios `DEMO-001` a `DEMO-003`, não CPFs reais.

| Peça inicial | Preço | Saldo | Mínimo | Alerta |
| --- | --- | --- | --- | --- |
| Óleo 5W30 1 litro | R$ 42,90 | 20 | 5 | Não |
| Filtro de óleo | R$ 35,00 | 3 | 4 | Sim |
| Pastilhas de freio | R$ 165,00 | 8 | 3 | Não |
| Filtro de ar | R$ 48,00 | 2 | 3 | Sim |

O script `03_dados_demonstracao.sql` contém essa mesma população exportada do banco de demonstração. Execute os scripts na ordem indicada no README, em banco vazio, ou utilize a preparação pelo Java. Os testes usam outro banco e não integram os dados fictícios da entrega.

## 8. Stack e arquitetura

**Servidor:** Java 17 ou superior, API HTTP da biblioteca padrão e JDBC. **Banco:** MySQL com tabelas InnoDB. **Driver:** MySQL Connector/J 8.4.0. **Interface:** HTML5, CSS3, JavaScript e SVG. **Ferramentas de desenvolvimento e validação:** JDK 23, compilador javac, Edge, testes HTTP em Python e automação de navegador com Playwright.

O sistema não depende de framework web nem de bibliotecas JavaScript. Python e Playwright foram ferramentas auxiliares de teste e de preparação dos documentos, não componentes necessários à execução do sistema. O arquivo pom.xml facilita importar o código em uma IDE que utilize Maven. A execução pelos arquivos .bat usa o JAR incluído e dispensa Maven.

Fluxo técnico: o navegador solicita os recursos de `web`, faz requisições à API Java e recebe JSON. As gravações usam formulários URL encoded. O Java valida os campos e o perfil, executa comandos JDBC parametrizados e confirma ou desfaz a transação. O MySQL armazena os relacionamentos e saldos. O Java escuta apenas na interface local 127.0.0.1.

## 9. Autenticação e integridade

O login localiza um usuário ativo pelo e-mail e compara a senha com seu hash PBKDF2-HMAC-SHA256. Cada hash usa salt aleatório de 16 bytes e 210.000 iterações. O servidor cria uma sessão aleatória mantida em memória e envia um cookie HttpOnly com SameSite=Strict. A sessão expira em oito horas ou no logout. Reiniciar o servidor exige novo login.

As gravações exigem token CSRF correspondente à sessão. As respostas não expõem senha_hash. O Java confere novamente se o usuário está ativo a cada requisição. Os textos apresentados pela interface passam por escape HTML, e comandos SQL usam PreparedStatement para os valores recebidos.

O consumo de uma peça bloqueia a ordem e a peça com SELECT FOR UPDATE. Na mesma transação, verifica saldo, altera estoque, insere item, registra movimentação e histórico. Uma falha executa rollback. Cancelamento e remoção fazem a devolução também em transação. Duas ordens disputando a última unidade resultaram em um consumo aprovado e outro rejeitado no teste de concorrência.

## 10. Identidade visual

Nome: **Torq Oficina**. Cliente fictício: **Oficina Rota 4**. A logomarca contém uma chave mecânica branca sobre base laranja e o nome Torq na interface. O arquivo editável da marca está em `web/logo.svg`.

| Elemento | Padrão |
| --- | --- |
| Cor principal | Verde escuro #142C25, cabeçalho e acesso |
| Cor de ação | Laranja #E76636, ações principais e marca |
| Cor de confirmação | Verde #32775E |
| Fundo | Claro #F3F6F4 |
| Tipografia | Segoe UI, com fallback Arial e sans-serif |
| Componentes | Navegação superior fixa, cartões, listas paginadas, formulários e indicação textual de status |

Mensagens, rótulos e dados usam português. Valores aparecem em reais. As cores de status acompanham textos, permitindo identificar a situação sem depender apenas da cor.

A versão atualizada apresenta animações curtas de entrada, abertura de modais, estados de carregamento e avisos. A preferência de movimento reduzido do sistema operacional é respeitada. O cabeçalho fica no topo, a navegação é horizontal e o painel usa cartões adaptados ao celular.

Busca global por Ctrl+K, filtros sem distinção de acentos, ordenação, paginação de 12/24/48 linhas e CSV apoiam as consultas. Movimentações, histórico, auditoria e ordens também filtram por período. Os filtros são preservados na sessão do navegador e a navegação aceita Voltar e endereços diretos de ordens.

Os formulários têm instruções, prévia do subtotal e do saldo, proteção contra envio duplicado enquanto salvam e aviso antes de descartar preenchimento. A resposta de uma ordem atrasada não substitui uma página aberta depois. Uma falha de atualização mantém os dados já exibidos e informa como tentar novamente.

## 11. Telas e integração das funcionalidades

As capturas incluídas são do sistema executando com dados fictícios. Os campos e as ações são explicados abaixo. O relatório PDF apresenta as 12 telas, além da busca global e de uma captura do layout móvel.

| Tela | Finalidade e usuário | Dados apresentados | Ações e integração |
| --- | --- | --- | --- |
| Login | Autenticar qualquer perfil ativo | E-mail, senha e mensagem de erro | Entrar cria sessão e abre o painel |
| Painel | Acompanhar operação, todos os perfis | Ordens ativas, execução, prontas e alertas | Abrir ordem, consultar estoque e criar ordem conforme perfil |
| Clientes | Organizar atendimento, administrador e atendente | Nome, documento, telefone e e-mail | Buscar, cadastrar e editar. Alimenta o vínculo do veículo |
| Veículos | Identificar veículos, administrador e atendente | Placa, marca, modelo, cliente, ano e km | Buscar, cadastrar e editar. Veículo disponível entra na ordem |
| Serviços | Consultar catálogo, todos. Alterar, administrador | Nome, descrição e preço | Buscar e manter catálogo. Ordem copia o preço ao registrar serviço |
| Ordens | Consultar atendimentos, todos | Número, veículo, cliente, responsável, status, valor e abertura | Buscar, filtrar por status, abrir detalhes e criar ordem conforme perfil |
| Detalhe da ordem | Executar e acompanhar manutenção, conforme perfil | Relato, serviços, peças, quantidades, preço, total e histórico | Adicionar/remover itens, alterar status e imprimir. Integra estoque e histórico |
| Peças e estoque | Consultar materiais, todos. Alterar, administrador | Código, peça, preço, saldo, mínimo e alerta | Buscar, cadastrar, editar e movimentar. Consumo da ordem altera saldo |
| Movimentações | Rastrear estoque, todos | Data, peça, tipo, quantidade, saldo, ordem, autor e motivo | Buscar e abrir ordem relacionada |
| Histórico | Consultar eventos, todos | Data, ordem, placa, evento e usuário | Buscar e abrir a ordem de origem |
| Usuários | Administrar acessos, administrador | Nome, e-mail, perfil e situação | Cadastrar, editar, trocar senha e desativar. Define acesso às demais telas |
| Auditoria | Rastrear acessos e mudanças, administrador | Data, autor e ação | Buscar, filtrar por período, ordenar, paginar e exportar |

## 12. Plano, execução e resultados de testes

Objetivo: verificar as funcionalidades principais, a validação das regras e a integração entre interface, Java e banco. Estratégia: testes HTTP com dados válidos e inválidos, consultas JDBC para invariantes e navegação real com formulários no Edge. Critério de aprovação: código HTTP esperado, estado persistido correto, saldo consistente e fluxo funcional sem erros de JavaScript.

Ambiente de execução: Windows, Java 23 compilando para Java 17, Connector/J 8.4.0 e **MariaDB 10.4.32 do XAMPP** em banco isolado. Projeto destinado a MySQL 8. A execução em MySQL 8 dedicado não foi realizada neste ambiente. A diferença é registrada como limite da validação, sem alterar a stack solicitada para implantação.

Resultado final: **44 de 44 cenários da API aprovados**, sendo 36 de regressão e oito da edição de atendimento e suas permissões. Verificação direta por JDBC confirmou 11 tabelas, estoque não negativo, senhas com PBKDF2, ausência de ordens ativas duplicadas e conciliação do estoque com movimentações. A interface passou por 26 cenários de integração no Edge, incluindo navegação superior, busca global, filtros, paginação, exportação, confirmação e o fluxo completo. Todas as telas foram conferidas em 360, 390, 768 e 1440 px, sem erros de JavaScript ou transbordamento da página.

O fluxo completo também passou pelos formulários do navegador: cliente, veículo, ordem, serviço, peças, rejeição de estoque insuficiente, conclusão e entrega. O valor do exemplo foi confirmado em R$ 296,60.

Os resultados completos estão em `testes/resultado_api.json` e `testes/resultado_interface.json`. O relatório de testes inclui precondições, procedimentos e resultados esperados para repetição. Testes de carga e em outros navegadores não foram realizados. O tamanho das listas corresponde a uma base pequena de demonstração.

## 13. Problemas encontrados e correções

| Problema observado | Tratamento | Evidência |
| --- | --- | --- |
| Sessão de usuário desativado retornava registro não encontrado | Revalidar usuário ativo e retornar 401, removendo a sessão | CT31 passou após a correção |
| Conexão Java bloqueada no ambiente restrito de execução | Executar a validação local com acesso à porta do banco isolado | Inicialização e consultas JDBC concluídas |
| Java 23 falhava ao criar socket em pasta temporária do ambiente | Definir pasta temporária curta na execução de validação | Servidores de demonstração e teste iniciados |
| Teste de navegador encontrava dois campos com rótulo E-mail | Restringir o seletor de teste ao formulário do modal | Cadastro e edição pela interface concluídos |

As dificuldades acima refletem esta implementação e sua validação. A divisão de falas não atribui experiências ou autoria de código aos integrantes.

## 14. Cobertura das 12 etapas

| Etapa do enunciado | Entrega correspondente |
| --- | --- |
| 1. Levantamento de requisitos | Usuários, dados, RF01-RF20, RNF01-RNF08 e RN01-RN17 |
| 2. Modelagem | Dicionário das 11 tabelas, chaves, relacionamentos e modelo_er.mmd |
| 3. Criação e população | 01_banco.sql, 02_estrutura.sql, 03_dados_demonstracao.sql e inicialização Java |
| 4. Autenticação | Login, usuários, perfis, hash, sessão e logout |
| 5. Interface principal | Painel, logomarca SVG e identidade visual |
| 6. Cadastro e consulta | Clientes, veículos, serviços, peças, usuários, buscas e alterações |
| 7. Operação principal | Abertura, itens, totais, status e entrega de ordens |
| 8. Controle operacional | Catálogo de serviços, peças e movimentação de estoque |
| 9. Alertas e histórico | Mínimo, movimentos, histórico de ordens e auditoria |
| 10. Testes | 44 cenários da API, 26 da interface e invariantes JDBC |
| 11. Apresentação | PPTX, PDF dos slides, telas e roteiro para cinco integrantes |
| 12. Organização e entrega | Código, driver, configuração, scripts, README e pacote ZIP |

A apresentação presencial, o ensaio, o domínio individual e a adequação às regras da disciplina dependem do grupo. O enunciado prevê 180 minutos para o desenvolvimento e não representa um registro de tempo efetivamente gasto nesta preparação.

## 15. Demonstração e próximos passos do grupo

O roteiro em `documentacao/roteiro_apresentacao.md` descreve um atendimento completo: login, cliente, veículo, abertura, execução, serviços, consumo de peças, teste negativo de estoque, conferência das movimentações, conclusão, entrega e consulta do histórico.

Exemplo da carga inicial: Troca de óleo de R$ 90,00, quatro litros de óleo de R$ 42,90 e um filtro de R$ 35,00 resultam em R$ 296,60. O consumo reduz o saldo de óleo de 20 para 16 e de filtro de 3 para 2, quando nenhuma outra operação ocorreu.

Cada integrante deve explicar a interface, os vínculos do banco e as regras centrais. A distribuição de falas proposta identifica os cinco integrantes sem afirmar que desenvolveram partes específicas. Antes da apresentação, o grupo deve validar o projeto no MySQL da máquina escolhida e ensaiar o fluxo completo.

## 16. Referências e transparência

Fonte do escopo: PDF fornecido pelo usuário, Grupo 04 - Oficina Mecânica, quatro páginas. Foram utilizadas as ferramentas e os componentes locais disponíveis. Não houve pesquisa externa na internet para produzir a solução.

O material foi preparado com assistência de IA. O PDF contém uma regra que proíbe IA na avaliação. O grupo precisa considerar essa regra antes de utilizar os arquivos como entrega e deve compreender o conteúdo que apresentar.
