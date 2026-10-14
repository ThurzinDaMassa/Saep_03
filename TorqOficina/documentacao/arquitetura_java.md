# Organização das classes Java

O código está em `src/main/java/oficina`, na estrutura padrão de um projeto Maven. As classes estão formatadas com indentação de quatro espaços, imports explícitos e comentários sobre suas responsabilidades. A compilação usa Java 17 como versão de destino.

Para estudar o programa, comece por `App.java`, siga para `web/ApiController.java` e depois para o serviço da operação que deseja entender.

```text
src/main/java/oficina/
├── App.java
├── config/
│   └── AppConfig.java
├── model/
│   ├── Cliente.java
│   ├── Veiculo.java
│   ├── Servico.java
│   ├── Peca.java
│   ├── OrdemServico.java
│   ├── Usuario.java
│   ├── Perfil.java
│   ├── StatusOrdem.java
│   └── Sessao.java
├── dao/
│   ├── JdbcDao.java
│   ├── ConsultaDao.java
│   └── RegistroDao.java
├── service/
│   ├── OficinaService.java
│   ├── CadastroService.java
│   ├── OrdemService.java
│   └── EstoqueService.java
├── database/
│   ├── ConnectionFactory.java
│   ├── DatabaseInitializer.java
│   └── DadosDemonstracao.java
├── security/
│   ├── SenhaService.java
│   └── SessaoService.java
├── validation/
│   └── Validacao.java
├── exception/
│   └── RegraNegocioException.java
├── web/
│   ├── ApiController.java
│   ├── ArquivoController.java
│   └── HttpUtil.java
└── util/
    └── Json.java
```

| Classe ou conjunto | Responsabilidade real no programa |
| --- | --- |
| `App` | Ponto de entrada: prepara dependências e inicia o servidor local. |
| `AppConfig` | Localiza o projeto pela pasta principal ou por `torq.root`, escolhe a configuração explícita ou local e valida a porta. |
| `Cliente`, `Veiculo`, `Servico`, `Peca`, `OrdemServico` | Records imutáveis com os dados validados dos formulários; são utilizados pelos serviços de cadastro e de ordens. |
| `Usuario` e `Perfil` | Identificam o usuário autenticado e seu perfil; respostas públicas não carregam a senha ou seu hash. |
| `StatusOrdem` | Centraliza os estados e as transições permitidas das ordens. |
| `Sessao` | Dados da sessão: usuário, token CSRF e validade. |
| `JdbcDao` | Executa SQL parametrizado, lê registros e retorna IDs gerados. |
| `ConsultaDao` | Consultas de listas, detalhes da ordem e indicadores do painel. |
| `RegistroDao` | Grava auditoria, histórico e movimentações na conexão da operação. |
| `OficinaService` | Encaminha cada operação ao serviço correspondente e registra a auditoria. |
| `CadastroService` | Regras de clientes, veículos, catálogos e usuários. |
| `OrdemService` | Abertura, edição, serviços, peças, conclusão, entrega e cancelamento. |
| `EstoqueService` | Entradas e saídas manuais, impedindo saldo negativo. |
| `ConnectionFactory` | Abre conexões JDBC para MySQL ou H2 conforme o modo escolhido. |
| `DatabaseInitializer` | Cria a estrutura e insere dados fictícios somente quando não há usuários. |
| `DadosDemonstracao` | Contas, clientes, veículos, serviços, peças e ordens iniciais. |
| `SenhaService` | Gera e verifica hashes PBKDF2 com salt aleatório. |
| `SessaoService` | Cria, expira e invalida sessões e tokens. |
| `Validacao` | Campos obrigatórios, e-mail, inteiros, preços e permissões. |
| `RegraNegocioException` | Comunica erros esperados de validação ou permissão à API. |
| `ApiController` | Recebe as requisições, autentica, verifica CSRF e confirma ou desfaz transações. |
| `ArquivoController` | Entrega HTML, CSS, JavaScript e imagens ao navegador. |
| `HttpUtil` e `Json` | Leitura de formulários, cabeçalhos HTTP e respostas JSON. |

## Exemplo: adicionar uma peça à ordem

1. `web/app.js` envia `POST /api/ordem/peca`, com a ordem, peça e quantidade.
2. `ApiController` valida a sessão e o token CSRF e abre uma transação.
3. `OficinaService` encaminha a operação ao `OrdemService`.
4. `OrdemService` verifica o perfil, o estado da ordem e o saldo, usando bloqueios `FOR UPDATE`.
5. `JdbcDao` registra o item com o preço daquele momento e atualiza o estoque.
6. `RegistroDao` registra a saída, o histórico e a auditoria na mesma transação.
7. A API confirma a transação e a interface atualiza o total e o saldo. Uma falha desfaz as gravações.

Os modelos representam os dados de entrada e a identidade autenticada; consultas de relatórios retornam mapas para manter o contrato JSON já usado pela interface. Os DAO recebem a conexão do chamador para que cada operação use uma única transação.

## Abrir em uma IDE

Abra a pasta `TorqOficina` no IntelliJ, Eclipse ou VS Code com suporte a Java. Importe o `pom.xml` como projeto Maven. A classe principal é `oficina.App`. Defina a pasta de trabalho como `TorqOficina` e a opção de VM `-Dtorq.config=config.demo.properties` para usar o banco local. No Windows, prefira os iniciadores fornecidos, que também tratam a pasta temporária do Java.

O Maven é opcional. `compilar.bat` compila todas as classes e gera `dist/TorqOficina.jar`. Feche o sistema antes de recompilar, porque o Windows mantém o JAR em uso enquanto o servidor está aberto.

A versão original de duas classes está guardada em `documentacao/arquivo/fontes_originais.zip` para consulta histórica. Ela não participa da compilação.
