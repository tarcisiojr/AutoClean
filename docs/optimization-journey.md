# Redução de tamanho e memória

Trajetória do app da versão inicial até a versão atual.

## Comparativo

| Métrica | v1 (UI Leanback) | v2 (daemon) | Redução |
|---|---|---|---|
| APK | ~5 MB | 1.16 MB | -77% |
| RAM (PSS) | ~140 MB | ~22.7 MB | -84% |
| Classes Kotlin | 35+ | 7 | -80% |
| Dependências AndroidX | 7 | 2 | -71% |

## O que foi removido e por quê

### UI Leanback completa
`BrowseSupportFragment` com 6 rows, `CardPresenter`, layouts, drawables (cards, badges, progress bars), colors extensas, styles Leanback. Motivo: o app roda autônomo; status em runtime pode ser checado via `logcat`.

### Dependências
Saíram: `appcompat`, `cardview`, `leanback`, `lifecycle-runtime-ktx`, `lifecycle-service`, `preference-ktx`. Cada uma desses arrasta métodos/recursos consideráveis. Restou: `core-ktx` (necessário para `NotificationCompat`) e `work-runtime-ktx` (WorkManager).

### Workers periódicos
`MonitoringWorker` (rodava a cada 30min e causava micro-freezes) e `WeeklyAnalysisWorker` (só servia à UI) foram deletados. `DailyCleanupWorker` também, porque a limpeza em standby já cobre o ciclo diário.

### Classes de suporte a features mortas
`BloatwareFreezer`/`BloatwareScanner`/`CacheAnalyzer`/`CacheCleaner`: dependiam de `pm`/`am` privilegiados que o app não consegue rodar. `ActionLogger`: só persistia dados para a UI. `SchedulerConfig`: sem workers periódicos, virou redundante.

### Scanner e whitelist
`ProcessScanner` retornava só o próprio processo (limitação do UID), então removido. `RamMonitor`/`DegradedMode`/`ForegroundDetector` só existiam pra decidir quando/quem matar — dispensáveis quando a regra é "mata a lista fixa em standby".

### AIDL + ShellService
Remanescentes da tentativa com Shizuku. Sem uso real.

## R8 + shrinkResources

Ativados em debug e release:

```kotlin
isMinifyEnabled = true
isShrinkResources = true
```

Reduziu o APK mais ~30% além da limpeza de código, removendo símbolos do Kotlin runtime e resources não referenciadas. `proguard-rules.pro` mantém só o mínimo: Application, Activity, Service, Receivers e o construtor do Worker (instanciados por reflexão).

## Decisões de arquitetura que facilitaram cortes

1. **Standby é o único trigger**: dispensa heartbeat de monitoramento.
2. **Lista fixa de alvos**: dispensa scan e whitelist dinâmica.
3. **Kill fire-and-forget**: dispensa medição pós-operação e logging persistente.
4. **Notification `IMPORTANCE_MIN`**: foreground service visível só em detalhes do sistema, sem strings/ícones elaborados.

## O que manter em mente antes de crescer o app

Qualquer feature nova precisa pesar contra o custo de RAM/APK. Se a feature exigir UI, considerar:

- É algo que o usuário checa com que frequência? Se raramente, `logcat` é suficiente.
- Dá pra resolver com script ADB pontual? Se sim, documente em `adb-runbook.md` em vez de codar no app.
- Precisa mesmo ser periódico, ou é um evento? Eventos (SCREEN_OFF, BOOT_COMPLETED, PACKAGE_ADDED) são muito mais baratos que `PeriodicWorkRequest`.
