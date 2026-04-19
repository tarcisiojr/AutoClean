## ADDED Requirements

### Requirement: Monitoramento leve periodico
O sistema SHALL executar um ciclo de monitoramento leve a cada 30 minutos via WorkManager PeriodicWorkRequest.

#### Scenario: Ciclo de monitoramento leve
- **WHEN** o WorkManager dispara o ciclo de 30 minutos
- **THEN** o sistema coleta metricas de RAM e storage, e se MemAvailable < threshold, aciona o ram-manager para liberar memoria

### Requirement: Limpeza diaria agendada
O sistema SHALL executar uma limpeza completa uma vez por dia, preferencialmente entre 03:00 e 05:00 (madrugada).

#### Scenario: Limpeza diaria executada
- **WHEN** o horario esta dentro da janela de limpeza diaria e a limpeza nao foi executada hoje
- **THEN** o sistema executa: kill de todos os processos ociosos nao-protegidos, limpeza de cache dos apps de streaming, e registra relatorio no log

#### Scenario: TV em uso durante janela de limpeza
- **WHEN** o horario esta na janela de limpeza mas a TV esta com um app em foreground (usuario ativo)
- **THEN** o sistema MUST adiar a limpeza por 30 minutos e tentar novamente

### Requirement: Analise semanal profunda
O sistema SHALL executar uma analise profunda uma vez por semana, gerando relatorio completo de uso de RAM, storage, processos mais pesados e recomendacoes.

#### Scenario: Analise semanal executada
- **WHEN** 7 dias se passaram desde a ultima analise profunda
- **THEN** o sistema gera relatorio com: top 10 processos por RAM, top 10 apps por cache, espaco livre em storage, tendencia de uso (melhorando/piorando), e armazena para consulta no dashboard

### Requirement: Inicio automatico apos boot
O sistema SHALL registrar um BroadcastReceiver para BOOT_COMPLETED e reiniciar os agendamentos do WorkManager apos reboot da TV.

#### Scenario: TV reinicia apos queda de energia
- **WHEN** a TV reinicia e o broadcast BOOT_COMPLETED e recebido
- **THEN** o sistema re-registra todos os PeriodicWorkRequests e tenta reconectar ao Shizuku

### Requirement: Configuracao de intervalos
O sistema SHALL permitir que o usuario configure os intervalos de monitoramento (minimo 15min) e o horario da limpeza diaria via dashboard.

#### Scenario: Usuario altera intervalo de monitoramento
- **WHEN** o usuario altera o intervalo de monitoramento para 60 minutos
- **THEN** o sistema cancela o WorkRequest atual e cria um novo com o intervalo de 60 minutos
