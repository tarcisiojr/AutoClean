## ADDED Requirements

### Requirement: Tela principal com status geral
O sistema SHALL exibir uma tela principal Leanback com: indicador visual de RAM (usado/livre com barra de progresso), indicador de storage, status do Shizuku (conectado/desconectado), e ultima acao executada.

#### Scenario: Dashboard aberto com Shizuku ativo
- **WHEN** o usuario abre o app e o Shizuku esta ativo
- **THEN** o dashboard exibe RAM atual, storage, status "Shizuku: Conectado" em verde, e timestamp da ultima acao

#### Scenario: Dashboard aberto sem Shizuku
- **WHEN** o usuario abre o app e o Shizuku nao esta ativo
- **THEN** o dashboard exibe metricas de monitoramento e status "Shizuku: Desconectado" em vermelho com instrucoes de reconexao

### Requirement: Navegacao por controle remoto
O sistema SHALL ser inteiramente navegavel por D-pad (controle remoto da TV), sem necessidade de mouse ou teclado.

#### Scenario: Navegacao entre secoes
- **WHEN** o usuario pressiona direcional esquerda/direita
- **THEN** o foco muda entre as secoes do dashboard (Status, RAM, Bloatware, Cache, Logs, Config)

### Requirement: Secao de gerenciamento de RAM
O sistema SHALL exibir lista de processos ativos com nome, RSS em MB, status (protegido/nao-protegido) e opcao de kill manual.

#### Scenario: Kill manual de processo
- **WHEN** o usuario seleciona um processo nao-protegido e confirma kill
- **THEN** o sistema executa am force-stop via Shizuku e atualiza a lista

### Requirement: Secao de bloatware
O sistema SHALL exibir a lista de bloatware com toggle congelar/descongelar e botao "Congelar todos recomendados".

#### Scenario: Toggle de bloatware
- **WHEN** o usuario pressiona OK em um app de bloatware
- **THEN** o sistema alterna entre congelado/ativo e exibe feedback visual

### Requirement: Secao de logs
O sistema SHALL exibir historico das ultimas 100 acoes executadas (kills, limpezas, congelamentos) com timestamp e detalhes.

#### Scenario: Consulta de logs
- **WHEN** o usuario acessa a secao de logs
- **THEN** o sistema exibe lista cronologica reversa das acoes com: data/hora, tipo de acao, app afetado, resultado

### Requirement: Secao de configuracoes
O sistema SHALL permitir configurar: threshold de RAM, threshold de cache, intervalos do scheduler, horario de limpeza diaria, e whitelist de apps.

#### Scenario: Alteracao de configuracao
- **WHEN** o usuario altera um valor de configuracao
- **THEN** o valor e persistido em SharedPreferences e aplicado imediatamente nos proximos ciclos
