# AGENTS.md — AutoClean Android TV

Guia para agentes de IA (Claude Code e outros) trabalhando neste repositório.

## O que é

App Android TV minimalista para a Smart TV TCL C7K (Android 12, MT9653GL, 2GB RAM). Um `ForegroundService` invisível escuta `ACTION_SCREEN_OFF` e dispara `killBackgroundProcesses` em 27 pacotes conhecidos de bloatware/streaming toda vez que a TV entra em standby.

Sem UI. Sem workers periódicos. Fire-and-forget.

## Stack

- **Linguagem**: Kotlin 1.9 (JVM 17)
- **Build**: Gradle 8.x + AGP
- **Dependências de runtime**: `androidx.core:core-ktx`, `androidx.work:work-runtime-ktx`
- **targetSdk**: 31 (Android 12) — `minSdk` idem
- **Arquitetura**: armeabi-v7a (TV é 32-bit)

## Estrutura

```
app/src/main/java/com/autoclean/
├── AutoCleanApp.kt              # Application: inicia o ScreenWatcherService
├── StartActivity.kt             # Activity invisível (Theme.NoDisplay)
├── ram/StandbyTargets.kt        # Lista de 27 pacotes alvo
└── scheduler/
    ├── BootReceiver.kt          # BOOT_COMPLETED → reinicia service
    ├── ScreenStateReceiver.kt   # ACTION_SCREEN_OFF → enfileira worker
    ├── ScreenWatcherService.kt  # ForegroundService que hospeda o receiver
    └── StandbyCleanupWorker.kt  # One-shot: itera StandbyTargets e mata cada um
```

## Comandos essenciais

```bash
# Build
./gradlew assembleDebug

# Instalar na TV (requer ADB conectado)
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Ver logs do fluxo
adb shell "logcat -d | grep -E 'ScreenState|Standby|WM-WorkerWrapper'"

# Medir memória
adb shell "dumpsys meminfo com.autoclean | head -20"
```

Fluxo completo de deploy/teste e setup inicial: [docs/adb-runbook.md](docs/adb-runbook.md).

## Documentação de aprendizados

Ler antes de fazer mudanças não-triviais — poupa tempo descobrindo os mesmos limites:

- **[docs/architecture.md](docs/architecture.md)** — diagrama do fluxo, papel de cada componente, racionais de design.
- **[docs/tcl-c7k-notes.md](docs/tcl-c7k-notes.md)** — peculiaridades da TV: serviços proprietários da TCL que afetam o app, falha do Shizuku, ADB instável, lista de bloatware congelado.
- **[docs/adb-runbook.md](docs/adb-runbook.md)** — comandos para setup inicial (congelamento de bloatware one-time), deploy, debug e reversão.
- **[docs/optimization-journey.md](docs/optimization-journey.md)** — histórico de redução de APK/RAM (de 5MB/140MB para 1.16MB/22MB), o que foi removido e por quê. Consultar antes de adicionar qualquer dependência ou feature nova.

## Regras importantes ao editar

1. **Não adicione workers periódicos**. Já provamos que causam micro-freezes na UI e não trazem ganho real vs. trigger por evento.
2. **Não reintroduza UI a menos que pedido**. A ausência de UI é o que mantém o app em ~22MB.
3. **Não dependa de shell UID**. Comandos `pm`, `am` (exceto via API pública `ActivityManager`), `ps -A` não funcionam no UID do app. Ver `docs/adb-runbook.md` seção "Limitações".
4. **Lista de alvos é fixa**. `StandbyTargets.kt` é a fonte de verdade. Adicionar um pacote lá é a única mudança necessária para matar mais apps em standby.
5. **ForegroundService é obrigatório**. `ACTION_SCREEN_OFF` não pode ser recebido por receiver declarado no Manifest a partir do Android 8.
6. **R8 está ativo em debug e release**. Se adicionar novas classes instanciadas por reflexão (workers, receivers, services), atualizar `app/proguard-rules.pro`.

## Convenções

- Comentários de código em português brasileiro (regras globais do usuário).
- Nomes de classes/variáveis em inglês (convenção técnica).
- Commits descritivos em português, com `Co-Authored-By: Claude` quando aplicável.
- Responder ao usuário sempre em português brasileiro.

## Hardware de teste

- TV: TCL C7K 55" (Android TV 12)
- ADB Wireless: IP varia por DHCP, porta 5555. IP atual pode estar em `.local-notes` do desenvolvedor.
- A TV cai offline no ADB quando entra em standby — precisa acordar com o controle antes de qualquer interação.
