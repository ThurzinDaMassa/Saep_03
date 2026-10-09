# Torq Oficina - Grupo 04

Sistema local de gestão da **Oficina Rota 4**, cliente fictício da avaliação de Testes de Sistemas.

Integrantes:

- Fernando Ian Harmel
- Arthur Ayyub Rodrigues
- Victor Nestor Prim
- Arthur Krebs Delvons
- Larissa Beatriz Luz

Docente: Éder Gabriel Rigueira Campos. Curso: Técnico em Desenvolvimento de Sistemas - Integrado ao EM. A turma específica do grupo não foi informada.

## Versão atualizada

Navegação superior fixa e responsiva, painel com atalhos por situação, animações suaves e suporte à preferência de movimento reduzido. Use **Ctrl+K** ou o botão de busca para localizar páginas, clientes, veículos e ordens.

As listas têm busca sem distinção de acentos, ordenação, paginação de 12/24/48 registros e exportação CSV dos registros filtrados. Ordens, histórico, movimentações e auditoria admitem filtro por período. Peças têm filtro de reposição. A busca e os filtros são preservados durante a navegação e os detalhes da ordem podem ser abertos pelo endereço `#ordem/ID`.

Formulários apresentam instruções, prévia de subtotal e saldo, impedem envio duplicado enquanto salvam e avisam antes de descartar alterações. Administrador e atendente podem editar o responsável e o relato de ordens em andamento. A edição fica registrada e é bloqueada após conclusão, entrega ou cancelamento. A tela Auditoria é exclusiva do administrador. Entrega, cancelamento e remoção de itens usam confirmações dentro da interface.

## Execução em outro computador

Pré-requisitos: **JDK 17 ou superior**, servidor **MySQL 8 ou superior** iniciado e navegador. O driver JDBC já está em `lib`. Não é necessário instalar Maven nem bibliotecas JavaScript. Todos os recursos da interface são locais.

1. Extraia o ZIP inteiro. Mantenha `src`, `web`, `banco` e `lib` na mesma pasta dos arquivos `.bat`.
2. Edite `config.properties`: host, porta, nome do banco, usuário e senha do seu MySQL. O usuário precisa poder criar o banco e as tabelas na primeira preparação.
3. Execute `preparar_banco.bat`. O comando compila o Java, cria o banco e as 11 tabelas e insere os dados de demonstração se não houver usuários. Ele preserva dados existentes.
4. Execute `iniciar.bat` e mantenha o terminal aberto.
5. Acesse **http://localhost:8080**. Se alterar `server.port`, use a porta escolhida.
6. Encerre com **Ctrl+C**.

O pacote não contém servidor MySQL nem JDK. Caso apareça `Access denied`, confira o usuário e a senha. `Communications link failure` indica que a conexão ao banco não foi estabelecida. Verifique se o servidor está ativo e se a porta está correta. `Address already in use` indica uma porta de aplicação ocupada: altere `server.port`.

No computador onde este projeto foi preparado, existe `config.local.properties` para a demonstração isolada. Tanto o Java quanto os arquivos .bat dão preferência a esse arquivo quando ele está presente. Uma configuração passada explicitamente por `-Dtorq.config=CAMINHO` tem prioridade. O ZIP não inclui essa configuração particular.

## Execução no IntelliJ IDEA

Abra o `pom.xml` da pasta `TorqOficina` como projeto Maven e selecione um JDK 17 ou superior. Aguarde a importação das dependências. As configurações compartilhadas na pasta `.run` ficam disponíveis na lista de execução:

1. Inicie o servidor MySQL e ajuste `config.properties` conforme sua instalação.
2. Execute **Torq Oficina - Preparar banco** na primeira execução. Essa configuração passa `--init`, prepara o banco e encerra.
3. Execute **Torq Oficina - Iniciar** para manter o servidor em funcionamento. Ela não passa `--init`.
4. Abra no navegador o endereço exibido no console.

O diretório de trabalho dessas configurações é a pasta `TorqOficina`. Se abrir a pasta pai `Saep_3`, o Java também reconhece a subpasta `TorqOficina`. Para outra localização, use `-Dtorq.root=CAMINHO_DA_PASTA`. O console informa a pasta e a configuração escolhidas, além do host e da porta do banco, sem exibir a senha.

Neste computador, a configuração local usa o banco de demonstração existente na porta **3307** e publica o site em **http://localhost:8082**. Esse servidor de banco precisa permanecer iniciado. O pacote de entrega usa `config.properties` e deve ser ajustado à instalação MySQL do computador de destino.

No Windows com Java 23, se aparecer `Unable to establish loopback connection`, use uma pasta temporária curta já existente, por exemplo:

```powershell
java -Djdk.net.unixdomain.tmpdir=C:\Temp -cp "build;lib\mysql-connector-j-8.4.0.jar" oficina.App
```

## Contas fictícias

| Perfil | E-mail | Senha |
| --- | --- | --- |
| Administrador | admin@torq.local | Oficina@123 |
| Atendente | atendimento@torq.local | Atende@123 |
| Mecânico | mecanico@torq.local | Mecanico@123 |

O administrador pode gerenciar usuários, catálogos e estoque. Atendentes gerenciam clientes, veículos, abertura e entrega de ordens. Mecânicos registram serviços e peças e acompanham a execução. Todos consultam a operação. Senhas no banco usam PBKDF2 com salt.

## Banco por scripts SQL

A preparação pelo Java é a opção mais simples. Como alternativa, abra MySQL Workbench e execute, **uma única vez em banco novo**, nesta ordem:

1. `banco/01_banco.sql` cria e seleciona `torq_oficina`.
2. `banco/02_estrutura.sql` cria tabelas, chaves, índices e restrições.
3. `banco/03_dados_demonstracao.sql` insere os dados fictícios, incluindo hashes das contas acima.

Não importe `03_dados_demonstracao.sql` depois da preparação automática, porque ambos criam os mesmos registros. O script de população usa IDs explícitos e é destinado a banco vazio. Se escolher outro nome de banco, substitua o nome em `01_banco.sql` e na configuração.

## Estrutura da entrega

| Pasta / arquivo | Conteúdo |
| --- | --- |
| src/oficina/App.java | Servidor HTTP, regras, autenticação, JDBC e transações |
| src/oficina/Json.java | Serialização de respostas JSON |
| web | Interface HTML, CSS, JavaScript e logomarca SVG |
| banco | Scripts de criação, estrutura e população |
| lib | MySQL Connector/J 8.4.0 e informação de licença |
| documentacao | Requisitos, modelagem, roteiro, testes e capturas das telas |
| entrega | Documentação PDF, apresentação PPTX e apresentação PDF |
| testes | Testes automatizados e resultados registrados |
| pom.xml | Importação opcional como projeto Maven em uma IDE |

Para compilar sem os `.bat`:

```powershell
javac --release 17 -encoding UTF-8 -d build src\oficina\Json.java src\oficina\App.java
java -cp "build;lib\mysql-connector-j-8.4.0.jar" oficina.App --init
java -cp "build;lib\mysql-connector-j-8.4.0.jar" oficina.App
```

## Testes

Os testes gravam dados. Use um **banco novo e exclusivo de testes**, nunca o banco de demonstração ou de uma oficina real. Crie `config.teste.properties` a partir de `config.properties`, alterando `db.name=torq_test` e `server.port=8081`. Prepare e execute uma segunda instância:

```powershell
java -Dtorq.config=config.teste.properties -cp "build;lib\mysql-connector-j-8.4.0.jar" oficina.App --init
java -Dtorq.config=config.teste.properties -cp "build;lib\mysql-connector-j-8.4.0.jar" oficina.App
```

Em outro terminal, com Python 3 instalado:

```powershell
python testes\testar_api.py --url http://127.0.0.1:8081 --confirmar-banco-teste
python testes\testar_melhorias_api.py --url http://127.0.0.1:8081 --confirmar-banco-teste
javac --release 17 -encoding UTF-8 -d build testes\VerificarBanco.java
java -cp "build;lib\mysql-connector-j-8.4.0.jar" VerificarBanco config.teste.properties
```

A confirmação no comando registra que o operador escolheu a instância de testes. O script não consegue detectar o nome do banco pelo navegador. Os resultados incluídos refletem a execução em **Java 23, Connector/J 8.4.0 e MariaDB 10.4.32 do XAMPP**, com navegador Edge em modo headless. O destino especificado do projeto é MySQL 8. A execução em um servidor MySQL 8 dedicado ainda deve ser confirmada na máquina de apresentação.

Os dois testes da API totalizam 44 cenários. Execute-os nessa ordem em um banco recém-preparado. O teste de interface está em `testes/testar_interface.mjs`; é opcional e requer Node, Playwright e Edge, usados apenas para testes. Com o módulo Playwright disponível localmente, execute `node testes/testar_interface.mjs --url http://127.0.0.1:8081 --confirmar-banco-teste`. O caminho de um módulo já instalado também pode ser informado em `TORQ_PLAYWRIGHT_MODULE`. O resultado está em `testes/resultado_interface.json` e a captura do fluxo completo em `testes/evidencias`.

## Demonstração

Siga `documentacao/roteiro_apresentacao.md`. Para manter os dados de demonstração após uma apresentação, crie outro banco de demonstração com nome novo e execute a preparação nele. Não há comando de reset destrutivo no sistema.

## Escopo e autoria

O projeto cobre as 12 etapas funcionais e documentais do enunciado. A apresentação presencial e o domínio individual dependem do grupo. A divisão de falas é uma sugestão, não uma afirmação de autoria ou de participação já realizada. O enunciado contém uma restrição ao uso de IA na avaliação. Este material foi preparado com assistência de IA e o grupo precisa considerar a regra da disciplina antes de utilizá-lo na entrega.

Aplicação para execução e demonstração local. Pagamentos, emissão fiscal e agendamentos não fazem parte do escopo solicitado.
