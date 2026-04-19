## 1. Setup do Projeto Android TV

- [x] 1.1 Criar projeto Android (Kotlin, API 31+, Leanback template) com applicationId com.autoclean
- [x] 1.2 Configurar build.gradle: targetSdk 31, minSdk 31, abiFilters armeabi-v7a, viewBinding habilitado
- [x] 1.3 Adicionar dependencias: Shizuku API, AndroidX Leanback, WorkManager, Lifecycle, SharedPreferences
- [x] 1.4 Configurar AndroidManifest: permissoes (PACKAGE_USAGE_STATS, QUERY_ALL_PACKAGES, RECEIVE_BOOT_COMPLETED, FOREGROUND_SERVICE), Shizuku provider, Leanback feature
- [x] 1.5 Criar estrutura de pacotes: core/, ram/, bloatware/, cache/, scheduler/, dashboard/, data/

## 2. Shizuku Bridge (core)

- [x] 2.1 Implementar ShizukuBridge: deteccao de Shizuku (instalado, ativo, permissao), listeners para onBinderReceived/onBinderDead
- [x] 2.2 Implementar ShellExecutor: interface unificada para executar comandos shell (am, pm, dumpsys) via Shizuku UserService AIDL, retornando stdout/stderr/exitCode
- [x] 2.3 Implementar modo degradado: fallback para APIs publicas quando Shizuku indisponivel (ActivityManager.killBackgroundProcesses, /proc/meminfo)
- [x] 2.4 Implementar reconexao automatica: listener BOOT_COMPLETED + Shizuku.onBinderReceived para reconectar apos reboot

## 3. RAM Manager

- [x] 3.1 Implementar RamMonitor: leitura de /proc/meminfo (MemTotal, MemFree, MemAvailable, Cached), calculo de percentuais
- [x] 3.2 Implementar ProcessScanner: listar processos ativos com PID, RSS, package name via parsing de `ps -A -o PID,RSS,NAME`
- [x] 3.3 Implementar Whitelist: whitelist imutavel (system_server, systemui, launcher, tvinput, bluetooth, wifi) + whitelist customizavel persistida em SharedPreferences
- [x] 3.4 Implementar ForegroundDetector: identificar app em foreground via UsageStatsManager, tracking de cooldown (5min configuravel)
- [x] 3.5 Implementar RamCleaner: logica de kill ordenada por RSS decrescente, respeitando whitelist e cooldown, executando `am force-stop` via ShellExecutor
- [x] 3.6 Implementar ActionLogger: registro de cada kill (timestamp, pacote, RSS, motivo) em banco local (SharedPreferences JSON)

## 4. Bloatware Control

- [x] 4.1 Implementar BloatwareScanner: listar apps com.tcl.* usando PackageManager, excluindo whitelist imutavel
- [x] 4.2 Implementar lista curada: lista pre-configurada de bloatware seguro (gamebar, eva, smartalexa, esticker, hotelmenu, ocean.instructions, channelplus, waterfall.overseas, gallery, hearaid)
- [x] 4.3 Implementar BloatwareFreezer: congelar via `pm disable-user --user 0`, descongelar via `pm enable`, persistir estado em SharedPreferences
- [x] 4.4 Implementar congelamento em lote: congelar todos os recomendados de uma vez com feedback de progresso

## 5. Cache Cleaner

- [x] 5.1 Implementar CacheAnalyzer: coletar tamanho de cache por app via StorageStatsManager
- [x] 5.2 Implementar lista de apps de streaming prioritarios: Netflix, Amazon, HBO, Disney+, Apple TV, Sky, Globo, Spotify, Claro NOW
- [x] 5.3 Implementar CacheCleaner: limpeza via `pm clear --cache-only <pacote>` (ou fallback `rm -rf cache/`) via ShellExecutor, com registro de espaco liberado
- [x] 5.4 Implementar limpeza por threshold: logica que aciona limpeza quando cache total > 1GB (configuravel), priorizando streaming

## 6. Auto Scheduler

- [x] 6.1 Implementar MonitoringWorker: PeriodicWorkRequest a cada 30min, coleta metricas e aciona RamCleaner se necessario
- [x] 6.2 Implementar DailyCleanupWorker: execucao diaria na madrugada (03:00-05:00), kill processos + limpeza cache, com adiamento se TV em uso
- [x] 6.3 Implementar WeeklyAnalysisWorker: analise profunda semanal, geracao de relatorio (top 10 RAM, top 10 cache, tendencias)
- [x] 6.4 Implementar BootReceiver: BroadcastReceiver para BOOT_COMPLETED, re-registro de todos os WorkRequests
- [x] 6.5 Implementar SchedulerConfig: persistencia de intervalos e horarios configuraveis, cancelamento e recriacao de WorkRequests ao alterar

## 7. TV Dashboard (Leanback UI)

- [x] 7.1 Implementar MainFragment: tela principal com BrowseSupportFragment, categorias (Status, RAM, Bloatware, Cache, Logs, Config)
- [x] 7.2 Implementar StatusRow: cards com RAM (barra visual), Storage, status Shizuku, ultima acao
- [x] 7.3 Implementar RamFragment: lista de processos ativos com RSS, status protegido/nao-protegido, opcao de kill manual
- [x] 7.4 Implementar BloatwareFragment: lista de bloatware com toggle congelar/descongelar, botao "Congelar todos recomendados"
- [x] 7.5 Implementar CacheFragment: lista de apps por consumo de cache, botao de limpeza individual e geral
- [x] 7.6 Implementar LogsFragment: lista cronologica reversa das ultimas 100 acoes com detalhes
- [x] 7.7 Implementar ConfigFragment: GuidedStepSupportFragment para editar thresholds, intervalos, whitelist, horario de limpeza

## 8. Integracao e Testes

- [x] 8.1 Integrar todos os modulos no Application class: inicializar Shizuku, registrar Workers, iniciar monitoramento
- [ ] 8.2 Testar instalacao via ADB na TV real: instalar app + Shizuku, conceder permissoes, verificar funcionamento
- [ ] 8.3 Testar ciclo completo: monitoramento → deteccao de RAM baixa → kill → log → dashboard atualizado
- [ ] 8.4 Testar congelamento/descongelamento de bloatware na TV real
- [ ] 8.5 Testar persistencia apos reboot: verificar que Workers reiniciam e Shizuku reconecta (com Wireless Debugging)
- [ ] 8.6 Testar modo degradado: desligar Shizuku e verificar que monitoramento continua funcionando
