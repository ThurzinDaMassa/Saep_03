# Torq Oficina — Grupo 04

Sistema de gestão da Oficina Rota 4: **interface web + servidor Java + banco relacional**. Login, cadastros, ordens, estoque, histórico e auditoria funcionam com gravação de dados.

## Para rodar agora

1. **Extraia o pacote inteiro**, mantendo as pastas juntas.
2. Abra o **XAMPP Control Panel**, clique em **Start** na linha **MySQL** e confira os dados de conexão em `config.properties`.
3. Na pasta principal, dê dois cliques em **`INICIAR_SISTEMA.bat`**. Dentro de `TorqOficina`, o equivalente é `iniciar.bat`.
4. O navegador abre em **http://localhost:8080**. Entre com **admin@torq.local** e **Oficina@123**.

Mantenha a janela do programa aberta. Encerre com **Ctrl+C**. Se o navegador não abrir automaticamente, acesse o endereço informado no terminal.

**Requisitos para o modo principal:** Java 17 ou superior instalado e banco iniciado pelo XAMPP (MySQL/MariaDB). Um JDK 17/21 também serve. O iniciador procura o Java instalado, sem exigir que o grupo configure manualmente o PATH. Se não houver Java, ele informa o que falta. Instalador: [Eclipse Temurin](https://adoptium.net/temurin/releases/).

O programa já está compilado em `dist/TorqOficina.jar`. O iniciador principal prepara e usa o banco indicado em `config.properties`, pelo servidor iniciado no XAMPP. Na instalação padrão, a configuração fornecida usa `127.0.0.1:3306`, banco `torq_oficina`, usuário `root` e senha vazia; ajuste esses valores se o seu banco tiver outra configuração. Não é necessário instalar Maven, Node ou Python para executar.

Para consultar as tabelas pelo phpMyAdmin, inicie também **Apache** no XAMPP e abra `http://localhost/phpmyadmin` (ou o endereço com a porta configurada para Apache). A aplicação Java atende na sua própria porta e precisa apenas do banco iniciado. O guia `../COMO_RODAR_E_APRESENTAR_O_SISTEMA.txt` explica a apresentação e a exportação/importação do banco entre computadores.

O XAMPP inclui MariaDB nas versões atuais, mantendo o botão MySQL no painel. Consulte a [FAQ oficial do XAMPP](https://www.apachefriends.org/faq_windows.html).

Há uma alternativa independente do XAMPP em **`iniciar_demo.bat`**, com banco H2 incluído em `lib` e criado em `dados/torq_demo.mv.db`. Essa alternativa usa outra base e preserva seus próprios registros. Para apresentar o banco do XAMPP, use o iniciador principal.

Para usar os cadastros e salvar dados, entre pelo endereço do servidor. Se abrir `web/index.html` diretamente, a página orientará a iniciar o programa pelo `.bat`.

## Arquivos que o grupo vai usar

| Arquivo | Para que serve |
| --- | --- |
| `../INICIAR_SISTEMA.bat` | Prepara e inicia o sistema com o banco do XAMPP e abre o navegador. |
| `iniciar.bat` | O mesmo iniciador, dentro da pasta do projeto. |
| `iniciar_mysql.bat` | Prepara e inicia o sistema com MySQL/MariaDB conforme a configuração. |
| `iniciar_demo.bat` | Alternativa com banco H2 local, separado do XAMPP. |
| `diagnosticar.bat` | Mostra o Java encontrado, a configuração, o banco e o endereço. |
| `compilar.bat` | Recompila todas as classes e gera o JAR; requer JDK 17+. |
| `empacotar.bat` | Gera o ZIP para compartilhar, com o programa, fontes e documentos. |
| `preparar_banco.bat` | Prepara somente o MySQL, preservando dados existentes. |
| `config.demo.properties` | Configuração do banco local e da porta da demonstração. |
| `config.properties` | Configuração para o MySQL. |

## Contas fictícias

| Perfil | E-mail | Senha |
| --- | --- | --- |
| Administrador | admin@torq.local | Oficina@123 |
| Atendente | atendimento@torq.local | Atende@123 |
| Mecânico | mecanico@torq.local | Mecanico@123 |

Administrador gerencia usuários, catálogos e estoque. Atendentes gerenciam clientes, veículos, abertura e entrega de ordens. Mecânicos registram serviços e peças e acompanham a execução. Senhas no banco usam PBKDF2 com salt.

## Onde estão as classes Java

Os **29 arquivos Java** estão em `src/main/java/oficina`, organizados em pacotes:

| Pacote | Conteúdo |
| --- | --- |
| `model` | Cliente, veículo, serviço, peça, ordem, usuário, perfil, estado e sessão. |
| `dao` | SQL parametrizado, consultas, auditoria, histórico e movimentações. |
| `service` | Regras de cadastro, ordens e estoque. |
| `database` | Conexão JDBC, preparação das tabelas e dados fictícios. |
| `security` | Hash de senha, sessões e tokens. |
| `web` | Controle da API, transações e entrega dos arquivos da interface. |
| `config`, `validation`, `exception`, `util` | Configuração, validações, erros e JSON. |

`App.java` é o ponto de entrada. O [guia das classes](documentacao/arquitetura_java.md) mostra a árvore completa, a função de cada classe e um exemplo de operação passando da tela até o banco.

Abra `TorqOficina/pom.xml` na IDE como projeto Maven, ou abra a pasta `TorqOficina` no VS Code com suporte a Java. A estrutura de fontes é a padrão do Maven. **Maven é opcional para executar ou compilar pelos `.bat`.** Feche o servidor antes de recompilar.

## Usar MySQL

O modo MySQL permanece disponível para a implantação e para a exigência da disciplina. Ele usa a mesma API e as mesmas regras Java. O banco de demonstração local e o MySQL armazenam seus dados separadamente.

1. No XAMPP, inicie o módulo MySQL (MariaDB); alternativamente, use um servidor MySQL 8 ou superior.
2. Edite `config.properties`: host, porta, nome do banco, usuário e senha. O usuário precisa poder criar o banco e suas tabelas na primeira preparação.
3. Execute **`iniciar_mysql.bat`**. Ele prepara a estrutura e inicia o servidor em uma única execução.

O MySQL Connector/J 8.4.0 está incluído em `lib`. Caso exista `config.local.properties`, os iniciadores de MySQL dão preferência a ele. A alternativa `iniciar_demo.bat` usa sempre `config.demo.properties`.

Como alternativa à preparação automática, execute os scripts abaixo, nessa ordem, **somente em um banco novo**:

1. `banco/01_banco.sql` — criação e seleção do banco.
2. `banco/02_estrutura.sql` — 11 tabelas, relacionamentos e restrições.
3. `banco/03_dados_demonstracao.sql` — registros fictícios.

Não execute o terceiro script depois da preparação automática: ambos criam os mesmos registros. O esquema local H2 fica separado em `banco/04_estrutura_h2.sql`.

## Se aparecer um erro

| Mensagem ou situação | O que fazer |
| --- | --- |
| Java não encontrado | Instale Java/JDK 17+ e abra novamente `INICIAR_SISTEMA.bat`. |
| Porta em uso | Feche a outra instância ou altere `server.port` no arquivo do modo escolhido. Use a nova porta no navegador. |
| Banco local já aberto | Feche a outra janela do sistema antes de iniciar novamente. |
| Driver ou interface ausente | Extraia o ZIP inteiro; mantenha `lib`, `web`, `banco` e `dist` dentro de `TorqOficina`. |
| MySQL: acesso negado | Confira `db.user` e `db.password`. |
| MySQL: falha de conexão | Inicie o servidor e confira host e porta. |
| Falha ao gerar o JAR | Feche o sistema antes de executar `compilar.bat`. |

O iniciador também trata a pasta temporária usada pelo Java no Windows, para evitar a falha `Unable to establish loopback connection` encontrada durante a validação. `diagnosticar.bat` ajuda a conferir a instalação.

## Testes e evidências

A versão organizada passou em **44 cenários de API**, **26 cenários no navegador** e **5 verificações do banco**, usando JDK 21.0.11, H2 2.5.252 e Microsoft Edge. Consulte [o relatório de validação](documentacao/validacao_organizacao.md) e os arquivos `testes/resultado_*_organizado.*`.

Para repetir os testes, use um banco **novo e exclusivo de testes**. Copie `config.teste.example.properties` para `config.teste.properties`. Escolha um caminho novo em `db.path` a cada execução completa. Na pasta `TorqOficina`, execute:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts\executar.ps1 -Acao demo -Configuracao config.teste.properties -SemAbrir
```

Em outro terminal, com Python 3 instalado:

```powershell
python testes\testar_api.py --url http://127.0.0.1:8081 --confirmar-banco-teste --saida testes\resultado_api_organizado.json
python testes\testar_melhorias_api.py --url http://127.0.0.1:8081 --confirmar-banco-teste --saida testes\resultado_api_organizado.json
```

Execute nessa ordem; o segundo script usa o relatório do primeiro. A confirmação registra que o operador escolheu o banco de testes; a API não revela o nome do banco. Os testes gravam dados e não devem ser usados no banco da demonstração.

O teste de navegador requer Node, Playwright e Edge apenas no ambiente de testes:

```powershell
node testes\testar_interface.mjs --url http://127.0.0.1:8081 --confirmar-banco-teste --saida testes\resultado_interface_organizado.json --evidencias testes\evidencias_organizado
```

Se o Playwright já estiver instalado em outro lugar, informe seu módulo por `TORQ_PLAYWRIGHT_MODULE`. Depois dos testes, **encerre o servidor** para verificar diretamente o banco H2:

```powershell
javac --release 17 -encoding UTF-8 -cp dist\TorqOficina.jar -d build\testes testes\VerificarBanco.java
java -cp "build\testes;dist\TorqOficina.jar;lib\*" VerificarBanco config.teste.properties
```

Os relatórios anteriores, sem o sufixo `_organizado`, foram preservados. Eles registram a validação anterior em Java 23 e MariaDB 10.4.32 do XAMPP. A execução em MySQL 8 dedicado ainda deve ser validada na máquina de implantação.

## Material da entrega

| Pasta | Conteúdo |
| --- | --- |
| `src/main/java` | Classes Java da versão organizada. |
| `dist` | Programa compilado para Java 17. |
| `web` | Interface HTML, CSS, JavaScript e SVG. |
| `banco` | Scripts MySQL e esquema do banco local. |
| `lib` | Drivers JDBC incluídos no pacote. |
| `scripts` | Iniciador que localiza o Java, compila e executa. |
| `documentacao` | Requisitos, modelagem, roteiro, guia das classes, testes e capturas. |
| `entrega` | PDFs e apresentação PPTX da entrega anterior. |
| `testes` | Testes automatizados, relatórios anteriores e evidências desta revisão. |
| `dados` | Banco local criado durante a execução; não precisa vir no ZIP. |

Integrantes: Fernando Ian Harmel, Arthur Ayyub Rodrigues, Victor Nestor Prim, Arthur Krebs Delvons e Larissa Beatriz Luz. Docente: Éder Gabriel Rigueira Campos. Curso: Técnico em Desenvolvimento de Sistemas — Integrado ao EM.

## Escopo e autoria

Aplicação para execução e demonstração local. Pagamentos, emissão fiscal e agendamentos não fazem parte do escopo. A apresentação presencial e o domínio individual dependem do grupo. A divisão de falas é uma sugestão, não uma afirmação de autoria ou de participação já realizada. O enunciado contém uma restrição ao uso de IA na avaliação. Este material foi preparado com assistência de IA e o grupo precisa considerar a regra da disciplina antes de utilizá-lo na entrega.

Referência técnica do banco local: [documentação oficial do H2](https://h2database.com/html/features.html). Os drivers e suas licenças estão identificados em `lib/LEIA-ME.txt`.
