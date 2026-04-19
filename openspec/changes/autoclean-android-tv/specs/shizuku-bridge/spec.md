## ADDED Requirements

### Requirement: Deteccao de Shizuku
O sistema SHALL detectar se o Shizuku esta instalado e ativo ao iniciar e a cada ciclo de monitoramento.

#### Scenario: Shizuku instalado e ativo
- **WHEN** o app inicia e o Shizuku esta rodando com permissao concedida
- **THEN** o sistema registra status "conectado" e habilita todas as funcionalidades que dependem de shell

#### Scenario: Shizuku nao instalado
- **WHEN** o app inicia e o Shizuku nao esta instalado
- **THEN** o sistema exibe instrucoes de instalacao no dashboard e opera em modo degradado (apenas monitoramento)

#### Scenario: Shizuku instalado mas sem permissao
- **WHEN** o app inicia e o Shizuku esta instalado mas a permissao nao foi concedida
- **THEN** o sistema solicita permissao via Shizuku API e aguarda concessao

### Requirement: Execucao de comandos shell
O sistema SHALL fornecer uma interface unificada para executar comandos shell via Shizuku, encapsulando: am force-stop, pm clear, pm disable-user, pm enable, dumpsys.

#### Scenario: Comando executado com sucesso
- **WHEN** um modulo solicita execucao de um comando shell e o Shizuku esta ativo
- **THEN** o sistema executa o comando via Shizuku binder, retorna stdout/stderr e exit code

#### Scenario: Comando falha por Shizuku desconectado
- **WHEN** um modulo solicita execucao e o Shizuku nao esta ativo
- **THEN** o sistema retorna erro especifico (ShizukuNotAvailable) e o modulo chamador decide como proceder

### Requirement: Reconexao automatica apos reboot
O sistema SHALL tentar reconectar ao Shizuku automaticamente apos reboot da TV, registrando listener para Shizuku.onBinderReceived.

#### Scenario: Shizuku reinicia apos boot (Wireless Debugging ativo)
- **WHEN** a TV reinicia e o Wireless Debugging esta ativo
- **THEN** o Shizuku reinicia automaticamente e o AutoClean reconecta via onBinderReceived em ate 60 segundos

#### Scenario: Shizuku nao reinicia apos boot (Wireless Debugging inativo)
- **WHEN** a TV reinicia e o Wireless Debugging nao esta ativo
- **THEN** o sistema opera em modo degradado e registra no log que acoes privilegiadas estao suspensas

### Requirement: Modo degradado sem Shizuku
O sistema SHALL funcionar em modo degradado quando o Shizuku nao esta disponivel, mantendo monitoramento de RAM/storage via APIs publicas e ActivityManager.killBackgroundProcesses (limitado).

#### Scenario: Operacao em modo degradado
- **WHEN** o Shizuku nao esta ativo
- **THEN** o sistema continua monitorando via /proc/meminfo e StorageStatsManager, usa killBackgroundProcesses (limitado), e desabilita funcoes de limpeza de cache e congelamento de bloatware
