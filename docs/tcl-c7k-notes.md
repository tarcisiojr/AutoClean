# TCL C7K — comportamentos e peculiaridades

Anotações específicas da Smart TV TCL C7K (Android TV 12, MT9653GL, 2GB RAM).

## Problema original

Após factory reset, a TV perde performance progressivamente em poucos dias — apps demoram a abrir, menu trava, vídeo engasga. Diagnóstico:

- **Storage**: não é a causa. Disco com 92% livre.
- **RAM**: causa real. Dos ~2.4GB totais, ~2GB são consumidos por bloatware TCL + serviços Google antes de qualquer app do usuário abrir. Sobra muito pouco para Netflix/YouTube operarem sem precisar matar cada app anterior.
- **Processos Google que reiniciam sozinhos**: `com.google.android.youtube.tv`, `com.android.vending`, `com.google.android.tvrecommendations` e vários outros ressurgem minutos após kill e consomem RAM continuamente.

## Serviços TCL relevantes

| Serviço | Comportamento |
|---|---|
| `TclBackgroundProcessManagerImpl` | Process manager proprietário que reinicia apps e mata services de terceiros agressivamente durante transições de estado (standby, wakeup). Causa: workers com `delay()` são cancelados. |
| `TclPowerManagerServiceEx` | Controla ciclo de standby. Dispara `ACTION_SCREEN_OFF` consistentemente. |
| `com.tcl.guard` | App "BootStartReceiver" que roda `autoCleanRubbish` em boot. Já tenta limpar algo, mas não é suficiente. |

## Shizuku NÃO funciona neste modelo

Tentamos elevar permissões via Shizuku. O `app_process` que inicia o daemon Shizuku aborta com erro `type 3` ao ser lançado. Provável causa: SELinux policy da TCL bloqueia o `exec` de `app_process` fora do contexto do sistema. Solução adotada: viver com as APIs públicas (`killBackgroundProcesses`) e usar ADB pontualmente para ações privilegiadas.

## ADB Wireless

- **IP muda entre reboots**: já vimos `192.168.0.13` e `192.168.0.70` no mesmo ambiente. Porta 5555 é estável.
- **Autorização é por sessão**: após reboot ou troca de IP, a TV pede para autorizar o cliente novamente (popup).
- **Conexão cai ao entrar em standby**: a TV não responde ADB em deep sleep. Precisa acordar via controle remoto antes de qualquer `adb` command.
- **Caminho nas configurações**: Configurações → Sistema → Sobre → tocar 7x em "Build" → voltar → Preferências do desenvolvedor → ADB Wireless.

## Bloatware congelado (one-time via ADB)

Congelados com `pm disable-user --user 0 <pkg>`. Reversível com `pm enable <pkg>`:

```
com.tcl.gamebar
com.tcl.eva
com.tcl.smartalexa
com.tcl.esticker
com.tcl.hotelmenu
com.tcl.ocean.instructions
com.tcl.channelplus
com.tcl.waterfall.overseas
com.tcl.gallery
com.tcl.hearaid
com.tcl.logkit
com.tcl.useragreement
com.tcl.usercenter
com.tcl.repairguide
com.tcl.t_solo
com.tcl.dashboard
```

Ganho imediato: ~70MB de RAM livre após o freeze.

## Alvos do standby cleanup (lista dinâmica do app)

Ver `app/src/main/java/com/autoclean/ram/StandbyTargets.kt`. Categorias:

- **Google TV services** (11 pacotes): YouTube TV, Videos, Play Store, Play Games, Dreamx, Katniss, Recommendations, SetupWraith, MediaShell, YouTube Music.
- **Streaming** (10 pacotes): Netflix, Prime Video, Max, Disney+, Apple TV, Spotify, Sky+, Claro Now, Globoplay, CBS.
- **TCL secundários** (6 pacotes): Exhibit, Suspension, TV, MagicConnect, PartnerCustomizer, MediaCenter.

Esses apps são considerados seguros para matar em background: são relançados sob demanda e não guardam estado crítico na sessão.

## Medição

- RAM disponível (`MemAvailable` em `/proc/meminfo`): ~700MB em idle após freeze manual.
- Ganho após ativar o app: mais difícil de medir pois o kernel recicla páginas em segundo plano. Indicador qualitativo: desaparecimento de micro-travamentos em apps de streaming reportados pelo usuário.
