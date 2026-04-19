## ADDED Requirements

### Requirement: Monitoramento de RAM em tempo real
O sistema SHALL coletar metricas de RAM (total, livre, disponivel, cached) via leitura de /proc/meminfo a cada ciclo de monitoramento.

#### Scenario: Coleta de metricas
- **WHEN** o ciclo de monitoramento executa
- **THEN** o sistema coleta MemTotal, MemFree, MemAvailable e Cached e armazena em memoria para consulta pelo dashboard

### Requirement: Kill de processos ociosos
O sistema SHALL encerrar processos em background que nao estao em uso ativo quando a RAM disponivel estiver abaixo do threshold configurado (padrao: 500MB).

#### Scenario: RAM abaixo do threshold
- **WHEN** MemAvailable < 500MB (configuravel)
- **THEN** o sistema identifica processos em background nao-protegidos, ordenados por consumo de RSS decrescente, e executa `am force-stop` via Shizuku para cada um ate que MemAvailable ultrapasse o threshold

#### Scenario: App em foreground protegido
- **WHEN** um processo esta em foreground (top activity)
- **THEN** o sistema MUST NOT encerrar esse processo, independente do consumo de RAM

#### Scenario: Cooldown apos saida do foreground
- **WHEN** um app sai do foreground ha menos de 5 minutos
- **THEN** o sistema MUST NOT encerrar esse processo (cooldown configuravel)

### Requirement: Whitelist de protecao
O sistema SHALL manter uma whitelist imutavel de processos que nunca serao encerrados, incluindo: system_server, com.android.systemui, launcher ativo, com.tcl.tvinput, com.android.bluetooth, servicos de wifi.

#### Scenario: Processo na whitelist
- **WHEN** um processo esta na whitelist imutavel
- **THEN** o sistema MUST NOT encerrar esse processo mesmo com RAM critica

### Requirement: Whitelist configuravel do usuario
O sistema SHALL permitir que o usuario adicione/remova apps da whitelist customizada via dashboard.

#### Scenario: Usuario adiciona app a whitelist
- **WHEN** o usuario marca um app como protegido no dashboard
- **THEN** esse app nao sera encerrado nos proximos ciclos de limpeza

### Requirement: Log de acoes
O sistema SHALL registrar cada acao de kill com timestamp, nome do processo, RAM liberada estimada e motivo.

#### Scenario: Kill executado com sucesso
- **WHEN** um processo e encerrado com sucesso
- **THEN** o sistema registra: timestamp, package name, RSS antes do kill, e motivo ("RAM abaixo de threshold")
