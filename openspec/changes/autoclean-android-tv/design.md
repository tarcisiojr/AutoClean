## Context

Projeto greenfield para Android TV (API 31+, armeabi-v7a 32-bit). A TV alvo e uma TCL C7K com 2.4GB RAM e 49GB storage rodando Google TV (Android 12). Diagnostico real via ADB mostrou que o problema de performance e 100% RAM — bloatware TCL consome ~617MB e servicos Google ociosos ~600MB, sobrando apenas ~366MB para apps do usuario antes de qualquer interacao. Nao existe solucao open-source equivalente para Android TV. O app sera distribuido via sideload (ADB), sem restricoes de Play Store.

A TV nao tem root. O framework Shizuku permite executar comandos shell (UID 2000) sem root, habilitando `pm clear`, `am force-stop` e `pm disable-user` de forma programatica. Wireless Debugging (Android 11+) permite que o Shizuku sobreviva a reboots.

## Goals / Non-Goals

**Goals:**
- App autonomo que roda sem interacao do usuario apos setup inicial
- Liberar RAM matando processos ociosos de forma inteligente (whitelist/blacklist)
- Permitir congelar/descongelar bloatware TCL de forma segura e reversivel
- Limpar cache de apps de streaming periodicamente
- Dashboard simples navegavel por controle remoto (Leanback)
- Setup unico: instalar via ADB, configurar Shizuku, esquecer

**Non-Goals:**
- Suporte a root (nao necessario com Shizuku)
- Publicacao na Play Store (uso pessoal)
- Suporte a tablets ou celulares (apenas Android TV)
- Overclock ou modificacoes de kernel
- Gerenciamento de rede ou firewall
- Substituicao do launcher Google TV

## Decisions

### 1. Kotlin + Jetpack como stack principal
**Escolha**: Kotlin com AndroidX, Leanback para UI, WorkManager para agendamento.
**Alternativas consideradas**:
- Java puro: mais verboso, menos idiomatico para Android moderno
- Flutter: overhead de runtime em TV com RAM limitada, integracao Shizuku complexa
- React Native: mesmo problema de runtime overhead
**Motivo**: Kotlin e o padrao para Android, zero overhead de runtime adicional, integracao nativa com Shizuku e APIs Android.

### 2. Shizuku como camada de privilegio
**Escolha**: Shizuku para executar comandos shell sem root.
**Alternativas consideradas**:
- Root (Magisk): poder total, mas requer unlock de bootloader e invalida garantia
- ADB companion (PC sempre conectado): funcional mas nao autonomo
- Accessibility Service abuse: fragil, Google restringe cada vez mais
**Motivo**: Shizuku oferece poder de shell (pm clear, am force-stop, pm disable-user) sem root. Com Wireless Debugging, sobrevive a reboots. Unica dependencia externa necessaria.

### 3. WorkManager para agendamento autonomo
**Escolha**: WorkManager com PeriodicWorkRequest em 3 niveis.
**Alternativas consideradas**:
- AlarmManager: menos confiavel com Doze mode
- Foreground Service permanente: consumiria RAM preciosa 24/7
- JobScheduler: WorkManager ja abstrai isso com melhor API
**Motivo**: WorkManager respeita Doze mode, sobrevive a reboots (com RECEIVE_BOOT_COMPLETED), minimo 15min de intervalo e suficiente para monitoramento.

### 4. Arquitetura modular por capability
**Escolha**: Cada capability (ram-manager, bloatware-control, etc.) como modulo independente com interface definida.
```
app/
├── src/main/java/com/autoclean/
│   ├── core/              # Shizuku bridge, WorkManager setup, DI
│   ├── ram/               # RamManager - monitor e kill
│   ├── bloatware/         # BloatwareController - freeze/unfreeze
│   ├── cache/             # CacheCleaner - limpeza periodica
│   ├── scheduler/         # AutoScheduler - orquestracao dos 3 niveis
│   ├── dashboard/         # Leanback UI
│   └── data/              # SharedPreferences, logs, whitelist/blacklist
```
**Motivo**: Cada modulo pode ser testado e evoluido independentemente. Se um falhar, nao derruba os outros.

### 5. Leanback para UI na TV
**Escolha**: AndroidX Leanback com BrowseSupportFragment.
**Alternativas consideradas**:
- Compose for TV: ainda em alpha, menos estavel
- UI customizada com RecyclerView: mais trabalho, reinventando a roda
**Motivo**: Leanback e o framework oficial do Google para Android TV, ja otimizado para navegacao por controle remoto (D-pad).

### 6. Estrategia de protecao contra kill agressivo
**Escolha**: Sistema de whitelist + cooldown + deteccao de app em foreground.
- Whitelist padrao: sistema critico (system_server, systemui, launcher, tvinput, bluetooth, wifi)
- Nunca matar app em foreground
- Cooldown de 5 minutos apos app sair do foreground
- Blacklist configuravel para bloatware (tcl.gamebar, tcl.eva, tcl.smartalexa)
**Motivo**: Evitar matar app que o usuario acabou de usar ou que e essencial para a TV funcionar.

## Risks / Trade-offs

**[Shizuku perde sessao em reboot]** → Mitigacao: Wireless Debugging (Android 11+) permite reconexao automatica. Instruir usuario a manter ativo. Fallback: app funciona em modo degradado (apenas monitoramento, sem acoes).

**[Kill de processo pode afetar experiencia]** → Mitigacao: whitelist de protecao, cooldown de 5min, nunca matar foreground. Dashboard mostra log de acoes para debug.

**[Desabilitar bloatware errado pode quebrar TV]** → Mitigacao: lista curada de apps seguros para desabilitar (baseada em diagnostico real). Apps essenciais (tvinput, systemui, launcher) protegidos em whitelist imutavel. Reversivel com pm enable.

**[WorkManager pode nao executar no horario exato]** → Trade-off aceitavel. Android TV em standby pode atrasar execucao. Para o caso de uso (limpeza periodica), precisao de horario nao e critica.

**[App consome RAM da TV]** → Trade-off: o proprio AutoClean consumira ~30-50MB. Mas libera ~185MB+ de bloatware e ~600MB periodicamente de processos ociosos. Saldo fortemente positivo.

**[armeabi-v7a 32-bit]** → A TV roda em modo 32-bit apesar do CPU ARMv8. Build deve targetar armeabi-v7a. Shizuku suporta 32-bit.
