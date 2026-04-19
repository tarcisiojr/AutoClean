# Arquitetura

O AutoClean é um daemon Android TV: um `ForegroundService` invisível que aciona uma limpeza de RAM toda vez que a TV entra em standby.

## Fluxo completo

```
┌─ TV em uso ──────────────────────────────────────────────────┐
│  ScreenWatcherService (foreground, ocioso)                    │
│    └─ ScreenStateReceiver registrado dinamicamente            │
└───────────────────────────────────────────────────────────────┘
                          │
                          │  usuário desliga a TV
                          ▼
             Android envia ACTION_SCREEN_OFF
                          │
                          ▼
          ScreenStateReceiver.onReceive()
                          │
                          ▼
     WorkManager enqueue StandbyCleanupWorker (one-shot)
                          │
                          ▼
  for pkg in StandbyTargets.all:
      ActivityManager.killBackgroundProcesses(pkg)
                          │
                          ▼
              Kernel reclama páginas
```

## Componentes

| Arquivo | Papel |
|---|---|
| `AutoCleanApp.kt` | `Application`. Inicia o `ScreenWatcherService` no `onCreate`. |
| `StartActivity.kt` | Activity invisível (`Theme.NoDisplay`). Garante o primeiro start do service após instalação, já que `BOOT_COMPLETED` só dispara no próximo boot. |
| `scheduler/ScreenWatcherService.kt` | `ForegroundService` com notificação `IMPORTANCE_MIN`. Registra o `ScreenStateReceiver` dinamicamente em `onCreate` e desregistra em `onDestroy`. |
| `scheduler/ScreenStateReceiver.kt` | Filtra `ACTION_SCREEN_OFF` e enfileira o worker com `ExistingWorkPolicy.KEEP` (evita duplicar se disparar duas vezes). |
| `scheduler/StandbyCleanupWorker.kt` | `CoroutineWorker` que itera `StandbyTargets.all` e chama `killBackgroundProcesses` em cada pacote. Fire-and-forget. |
| `scheduler/BootReceiver.kt` | Reinicia o `ScreenWatcherService` após `BOOT_COMPLETED`. |
| `ram/StandbyTargets.kt` | Lista de 27 pacotes conhecidos por consumir RAM em background na TCL C7K (Google TV services, apps de streaming, apps TCL secundários). |

## Por que `ForegroundService` e não só `BroadcastReceiver` no Manifest

Desde Android 8, `ACTION_SCREEN_OFF`/`ACTION_SCREEN_ON` não podem ser recebidos por receivers declarados no Manifest — só dinamicamente. Para manter o registro ativo sem depender da Activity, é necessário um componente de longa duração; `ForegroundService` é a única garantia de que o processo não será morto sem aviso.

## Por que matar a lista conhecida e não escanear

`ps -A` a partir de um UID de app retorna apenas os processos do próprio app (restrição do Android 8+). `ActivityManager.getRunningAppProcesses` devolve só o próprio processo em versões modernas. Sem shell/root, não há como enumerar processos alheios — mas `killBackgroundProcesses(pkg)` funciona em qualquer pacote com a permissão `KILL_BACKGROUND_PROCESSES` (normal permission). Por isso mantemos uma lista fixa de alvos e chamamos o kill diretamente: é no-op se o app não está rodando, e o pacote morre se estiver.

## Por que não medir RAM depois do kill

O process manager da TCL cancela o worker quando o display entra em deep sleep (~1-2s depois do SCREEN_OFF). Qualquer `delay()` ou leitura após os kills tem alta chance de ser abortada. Como os `killBackgroundProcesses` são fire-and-forget no kernel, a limpeza já aconteceu — só perdemos a métrica pós-kill, que não é crítica.

## Comportamento após OTA

A TCL pode re-habilitar bloatware congelado em updates do sistema. Nesse caso, rode novamente o congelamento manual via ADB (ver `adb-runbook.md`). O app em si não lida com re-freeze porque `pm disable-user` exige shell UID.
