# Validação da versão organizada

Execução realizada em 14/10/2026, em Windows com JDK 21.0.11, compilação para Java 17, H2 2.5.252 e navegador Microsoft Edge. Os testes usaram um banco separado da demonstração, na porta 8081.

| Verificação | Resultado | Evidência |
| --- | --- | --- |
| Compilação das 29 classes Java e geração do JAR | Aprovada | `compilar.bat` e `dist/TorqOficina.jar` |
| Testes da API | 44 de 44 aprovados | `testes/resultado_api_organizado.json` |
| Testes no navegador | 26 de 26 aprovados | `testes/resultado_interface_organizado.json` |
| Invariantes do banco | 5 de 5 aprovadas | `testes/resultado_banco_organizado.txt` |
| Inicialização, HTML direto e persistência após reiniciar | 5 de 5 aprovadas | `testes/resultado_inicializacao_organizado.json` |
| ZIP extraído em outra pasta: inicialização e consultas | 4 de 4 aprovadas | `testes/resultado_pacote_organizado.json` |

Os testes cobrem login, permissões, CSRF, cadastros, edição, preços, ordens, estoque, devoluções, concorrência, histórico e auditoria. No navegador, cobrem formulários, busca, filtros, paginação, CSV, conclusão e entrega, além de layouts entre 360 e 1440 pixels.

As cinco verificações do banco confirmam a conciliação dos saldos com as movimentações, estoque não negativo, hashes PBKDF2, ausência de ordens ativas duplicadas para um veículo e as 11 tabelas relacionais.

O ZIP foi extraído em outra pasta e iniciado pelo mesmo script usado no `.bat`, com a abertura automática do navegador desativada durante o teste. A cópia criou seu próprio banco, autenticou o administrador e consultou os cadastros e as ordens. Não foi necessário recompilar, instalar Maven ou configurar MySQL. O arquivo inclui as 29 classes, o JAR, os drivers e os documentos; exclui arquivos de banco, saídas de compilação e configurações locais de testes. A captura `testes/evidencias_organizado/sistema_funcionando.png` mostra essa execução.

As evidências com sufixo `_organizado` correspondem a esta revisão. Os relatórios sem esse sufixo e os PDFs/slides existentes foram preservados como material da entrega anterior. A implantação em um servidor MySQL 8 dedicado continua dependendo da configuração e validação desse ambiente.

## Iniciador principal ajustado ao ambiente do grupo

O grupo informou que usa o módulo MySQL do XAMPP. Por isso, `INICIAR_SISTEMA.bat` e `iniciar.bat` passaram a chamar o modo MySQL/MariaDB. O modo H2 utilizado nos testes desta revisão permanece em `iniciar_demo.bat`. As evidências anteriores de execução com H2 continuam correspondendo a esse modo; não são uma nova validação do servidor XAMPP do computador de apresentação. O guia TXT principal foi reescrito para XAMPP e phpMyAdmin.

## Integração com o repositório GitHub

Os ajustes de inicialização já existentes no GitHub foram preservados em `AppConfig`: descoberta da pasta `TorqOficina` pela raiz do repositório, prioridade da configuração local e suporte a `torq.root` e `torq.config` explícitos. A versão integrada foi recompilada com JDK 21.0.11 e alvo Java 17. Os oito cenários de `testes/verificar_configuracao.py` passaram; o resultado está em `testes/resultado_configuracao_organizado.json`. Esse teste usa o diagnóstico e não exige uma conexão ao servidor de banco. As configurações do IntelliJ foram preservadas e ajustadas para a pasta temporária do Windows.
