## Why

A TCL C7K (Smart TV Pro, Android 12, 2.4GB RAM) sofre degradacao progressiva de desempenho apos semanas de uso. O diagnostico real via ADB revelou que o problema nao e storage (92% livre, 45GB disponiveis) nem cache (apenas 635MB). O gargalo e **RAM**: dos 2,463MB totais, apenas 81MB estao livres antes de qualquer app do usuario ser aberto. Bloatware TCL (~617MB) e servicos Google TV ociosos (~600MB em background) consomem ~1.2GB de RAM sem entregar valor ao usuario. Nao existe solucao open-source para Android TV que resolva isso de forma autonoma.

## What Changes

- Criar aplicacao Android TV (Kotlin) que roda como servico autonomo, sem necessidade de interacao do usuario
- Gerenciamento de RAM: kill periodico de processos ociosos (YouTube background, Play Store, Screensaver, etc.) via Shizuku (permissoes shell sem root)
- Desabilitacao de bloatware: interface para congelar/descongelar apps TCL inuteis (GameBar, EVA, Alexa) com `pm disable-user` via Shizuku
- Limpeza de cache: limpeza periodica de cache de apps de streaming (Netflix, HBO, Prime, Globo, Disney+, Sky, Apple TV, Spotify)
- Monitoramento: coleta de metricas de RAM, storage e processos para dashboard na TV
- Execucao autonoma via WorkManager com estrategia de 3 niveis: monitoramento leve (30min), limpeza diaria (madrugada), analise profunda (semanal)
- Setup unico via ADB: instalar app + Shizuku, conceder permissoes, ativar Wireless Debugging para persistencia apos reboot

## Capabilities

### New Capabilities
- `ram-manager`: Monitoramento de RAM em tempo real e kill de processos ociosos. Identifica apps que consomem RAM sem estarem em uso ativo (YouTube background, Play Store, Screensaver duplo) e os encerra. Whitelist configuravel para proteger apps essenciais.
- `bloatware-control`: Congelar/descongelar apps de sistema inuteis via `pm disable-user`/`pm enable`. Baseado em diagnostico real da TV: tcl.gamebar (63MB, 0 launches), tcl.eva (71MB, 0 launches), tcl.smartalexa (51MB, 0 launches). Totalmente reversivel.
- `cache-cleaner`: Limpeza periodica de cache de apps de streaming e sistema via `pm clear` (Shizuku). Threshold configuravel. Foco nos apps de streaming que acumulam thumbnails e dados temporarios.
- `auto-scheduler`: Execucao autonoma via WorkManager em 3 niveis: monitoramento leve (a cada 30min verifica RAM e age se critico), limpeza diaria (madrugada, limpa caches e mata processos), analise profunda (semanal, relatorio completo).
- `tv-dashboard`: Interface Leanback otimizada para Android TV mostrando status de RAM, storage, processos ativos, historico de acoes e configuracoes. Navegavel por controle remoto.
- `shizuku-bridge`: Integracao com Shizuku para executar comandos shell (pm, am, dumpsys) sem root. Gerencia ciclo de vida do Shizuku, reconexao apos reboot via Wireless Debugging.

### Modified Capabilities
<!-- Nenhuma - projeto novo, repo vazio -->

## Impact

- **Plataforma**: Android TV (API 31+, Android 12), arquitetura armeabi-v7a (32-bit)
- **Dependencias externas**: Shizuku (rikka.shizuku), AndroidX Leanback, WorkManager
- **Hardware alvo**: TCL C7K (Smart TV Pro, MediaTek MT9653, 2.4GB RAM, 49GB storage)
- **Permissoes necessarias**: PACKAGE_USAGE_STATS, QUERY_ALL_PACKAGES, RECEIVE_BOOT_COMPLETED, FOREGROUND_SERVICE (concedidas via ADB)
- **Distribuicao**: Sideload via ADB (uso pessoal), sem Play Store
- **Riscos**: Shizuku perde sessao em reboot sem Wireless Debugging ativo; kill agressivo pode afetar apps em uso; desabilitar bloatware errado pode impactar funcionalidades da TV (mitigado com whitelist de protecao)
