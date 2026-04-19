# Runbook ADB

Comandos úteis para setup, deploy e debug. Ajusta o IP conforme o DHCP da TV.

## Conectar

```bash
# Ligar a TV com o controle antes
adb connect 192.168.0.70:5555
adb devices   # confirma 'device' (não 'offline')
```

Se aparecer `offline`, a TV está em standby ou a autorização expirou. Acorda e autoriza o popup.

## Setup inicial (one-time)

Executar a partir da raiz do repo, após a TV estar conectada:

```bash
# 1. Congelar bloatware TCL (reversível com pm enable)
for pkg in \
    com.tcl.gamebar com.tcl.eva com.tcl.smartalexa com.tcl.esticker \
    com.tcl.hotelmenu com.tcl.ocean.instructions com.tcl.channelplus \
    com.tcl.waterfall.overseas com.tcl.gallery com.tcl.hearaid \
    com.tcl.logkit com.tcl.useragreement com.tcl.usercenter \
    com.tcl.repairguide com.tcl.t_solo com.tcl.dashboard
do
    adb shell "pm disable-user --user 0 $pkg"
done

# 2. Build + instalar o APK
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 3. Iniciar o service (StartActivity)
adb shell am start -n com.autoclean/.StartActivity
```

## Verificar se o service está de pé

```bash
adb shell "dumpsys activity services com.autoclean | grep ServiceRecord"
# Esperado: com.autoclean/.scheduler.ScreenWatcherService
```

## Medir consumo de memória

```bash
adb shell "dumpsys meminfo com.autoclean | head -20"
```

PSS Total esperado: ~20-25MB em idle.

## Testar o fluxo SCREEN_OFF

ADB cai quando a TV dorme. Truque: gravar logcat em arquivo antes de enviar o SLEEP, ler depois.

```bash
adb shell "logcat -c && rm -f /data/local/tmp/autoclean_test.log"
adb shell "nohup logcat -f /data/local/tmp/autoclean_test.log \
    'ScreenStateReceiver:I' 'StandbyCleanupWorker:I' 'WM-WorkerWrapper:I' '*:S' \
    > /dev/null 2>&1 &" &
adb shell input keyevent KEYCODE_SLEEP

# --- Acorda a TV com o controle remoto e reconecta ---
adb disconnect && adb connect 192.168.0.70:5555
adb shell "cat /data/local/tmp/autoclean_test.log | tail -20"
```

Saída esperada:

```
ScreenStateReceiver: SCREEN_OFF detectado — agendando limpeza de standby
StandbyCleanupWorker: Limpeza de standby: 27 pacotes processados
WM-WorkerWrapper: Worker result SUCCESS for Work [...]
```

## Reverter

```bash
# Desinstalar o app
adb uninstall com.autoclean

# Re-habilitar bloatware congelado (se precisar restaurar)
adb shell pm enable com.tcl.gamebar   # repetir para cada pacote
```

## Limitações conhecidas ao rodar comandos

A partir do UID do app (`runtime.exec`), estes comandos **não funcionam**:

- `pm disable-user`, `pm enable`, `pm clear` — exigem shell UID
- `am force-stop` em apps de terceiros — idem
- `ps -A` — retorna apenas os processos do próprio app
- `dumpsys activity activities` completo — restrito

Funciona do app: `ActivityManager.killBackgroundProcesses(pkg)` (permissão normal `KILL_BACKGROUND_PROCESSES`), `/proc/meminfo`, leitura de `cat /proc/meminfo` via `Runtime.exec`.

## Alternativa: script shell como daemon

`autoclean.sh` na raiz do repo é uma alternativa pura em shell, rodando com UID shell (2000) via ADB. Consome ~1MB de RAM mas depende de ADB conectado. Em teoria sobrevive com `nohup`, mas na prática a TCL mata o daemon em vários eventos de standby. Mantido como fallback histórico — o app Android é a solução oficial.
