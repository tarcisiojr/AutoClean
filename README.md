# AutoClean — Android TV RAM Janitor

> Minimalist Android TV daemon that frees RAM every time the TV enters standby.
> Built and validated on a **TCL C7K** (Android TV 12, MT9653GL, 2 GB RAM), but the
> approach is portable to other low-RAM Android TVs.

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/platform-Android%20TV%2012-green)]()
[![APK size](https://img.shields.io/badge/APK-~1.2%20MB-brightgreen)]()
[![Idle RAM](https://img.shields.io/badge/RAM-~22%20MB-brightgreen)]()

---

## English (default)

### What it does

AutoClean is a headless `ForegroundService` that listens for `ACTION_SCREEN_OFF`.
Every time the TV enters standby it calls
`ActivityManager.killBackgroundProcesses(...)` on a curated list of 27 known
RAM-hungry packages (Google TV services, streaming apps, secondary TCL apps).
There is **no UI**, no periodic worker, no network calls, no telemetry.

- **Trigger**: `ACTION_SCREEN_OFF` (fires when the panel turns off).
- **Action**: fire-and-forget kill of background processes on a fixed allow-list.
- **Footprint**: ~1.2 MB APK, ~22 MB RSS while idle.

### Why

Cheap Android TVs ship with 1.5–2 GB of RAM and dozens of vendor background
services. Within days of a factory reset the device slows to a crawl: apps take
seconds to open, the launcher stutters, video judders. The kernel won't reclaim
fast enough because most of those services keep waking up. Killing the heavy
ones at the exact moment the user puts the TV to sleep is the cheapest possible
intervention — no root, no Shizuku, no shell daemon.

### How it works

```
┌─────────────────────────────────────────────────────────────┐
│   user presses remote SLEEP                                 │
│            │                                                │
│            ▼                                                │
│   system broadcasts ACTION_SCREEN_OFF                       │
│            │                                                │
│            ▼                                                │
│   ScreenStateReceiver (registered dynamically by the FGS)   │
│            │                                                │
│            ▼                                                │
│   WorkManager enqueues a one-shot StandbyCleanupWorker      │
│            │                                                │
│            ▼                                                │
│   for pkg in StandbyTargets.all:                            │
│       ActivityManager.killBackgroundProcesses(pkg)          │
└─────────────────────────────────────────────────────────────┘
```

Why a `ForegroundService`? On Android 8+ a manifest-declared
`BroadcastReceiver` cannot receive `ACTION_SCREEN_OFF` — it has to be registered
at runtime. The (otherwise invisible) FGS exists only to host that receiver.

Detailed design notes live under [`docs/`](docs/):

- [`docs/architecture.md`](docs/architecture.md) — component diagram and rationale (PT-BR).
- [`docs/tcl-c7k-notes.md`](docs/tcl-c7k-notes.md) — TCL C7K quirks: vendor services, Shizuku failure, ADB instability, bloatware list (PT-BR).
- [`docs/adb-runbook.md`](docs/adb-runbook.md) — bootstrap, deploy, debug and rollback commands (PT-BR).
- [`docs/optimization-journey.md`](docs/optimization-journey.md) — how the APK/RAM dropped from 5 MB/140 MB to 1.2 MB/22 MB (PT-BR).

### Requirements

- **Target device**: Android TV 10+ (built against `minSdk`/`targetSdk` 31).
- **ADB access**: TVs without a USB port use ADB-over-Wi-Fi (see below).
- **Host machine**: Android SDK + ADB. The Gradle wrapper takes care of the
  rest (`./gradlew assembleDebug`).
- **Optional**: a privileged shell over ADB if you also want to *freeze*
  vendor bloatware (`pm disable-user --user 0 <pkg>`). The Android app itself
  does **not** require any privileged permission.

### Enabling Developer Mode and ADB Wireless on the TV

Most Android TVs ship with developer features hidden. Steps below were
validated on a TCL C7K (Google TV / Android TV 12); other models follow the
same pattern but menu labels may differ slightly.

1. **Unlock Developer Options**
   - Open *Settings → System → About* (on Google TV: *Settings → System → About*).
   - Scroll to **Build** (or "Android TV OS build").
   - Press **OK / Enter** on it **7 times** until a toast says
     *"You are now a developer"*.

2. **Enable USB / Network debugging**
   - Go back one level and open *Developer options* (now visible).
   - Toggle **USB debugging** on (Android TV uses the same switch for ADB even
     without a USB port).
   - Toggle **Network debugging** / **ADB Wireless** / **Wireless debugging**
     on (label varies by firmware). On Google TV 11+ the option may be called
     *Wireless debugging*; on Android TV 9–12 it is usually *ADB over network*
     or *Network debugging*.

3. **Find the TV's IP address**
   - *Settings → Network & Internet → (your Wi-Fi) → IP address*, or check
     your router's DHCP list. The IP can change between reboots — reserve it
     in your router if you can.

4. **Authorize and connect from your computer**
   ```bash
   adb connect <TV_IP>:5555
   # A popup appears on the TV: "Allow USB debugging from this computer?"
   # Check "Always allow from this computer" and press OK on the remote.
   adb devices    # the TV should appear as "device" (not "offline" / "unauthorized")
   ```

5. **Troubleshooting**
   - **`unauthorized`** → confirm the popup on the TV. If you missed it,
     toggle *Wireless debugging* off and on again.
   - **`offline`** → the TV is in standby or the auth expired. Wake it with
     the remote and retry.
   - **Refused connection** → some firmwares disable wireless debugging until
     you accept it via USB once. Plug a USB cable (if the TV has a port) or
     try the pairing-code flow on Google TV
     (*Wireless debugging → Pair device with pairing code* → `adb pair <IP>:<PORT>`).
   - **IP keeps changing** → reserve the MAC in your router or use mDNS:
     `adb connect <hostname>.local:5555`.

### Quick start

```bash
# 1. clone
git clone https://github.com/<your-username>/AutoClean.git
cd AutoClean

# 2. point Gradle at your local SDK (local.properties is gitignored)
echo "sdk.dir=$ANDROID_HOME" > local.properties

# 3. build
./gradlew assembleDebug

# 4. enable ADB over Wi-Fi on the TV, find its IP, then:
adb connect <TV_IP>:5555
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 5. kick the foreground service once (BOOT_COMPLETED takes over after reboots)
adb shell am start -n com.autoclean/.StartActivity

# 6. (optional) verify the service is up
adb shell "dumpsys activity services com.autoclean | grep ServiceRecord"
```

The full runbook — including how to test the SCREEN_OFF flow without losing the
ADB session — is in [`docs/adb-runbook.md`](docs/adb-runbook.md).

### Adapting the target list to your TV

The single source of truth is
[`app/src/main/java/com/autoclean/ram/StandbyTargets.kt`](app/src/main/java/com/autoclean/ram/StandbyTargets.kt).
Add or remove package names there, rebuild, reinstall. **Do not** add vendor
packages that coordinate power state, networking, HDMI-CEC or input routing —
killing them can break Wi-Fi resume or hide HDMI inputs. Concrete examples and
symptoms observed on the TCL C7K are documented in `docs/tcl-c7k-notes.md`.

To discover candidate packages on your own TV:

```bash
# list installed packages (vendor + user)
adb shell pm list packages

# rank background processes by memory (run while TV is "idle")
adb shell "dumpsys meminfo --proto" | strings | head -100
adb shell "ps -A -o NAME,RSS,PID | sort -k2 -n -r | head -30"
```

### Optional: freezing vendor bloatware

The standby cleanup keeps RAM low *over time*. To recover the ~70 MB held by
TCL apps that you never use, you can disable them once via ADB (reversible
with `pm enable`). The exact list used on the C7K is in
[`docs/tcl-c7k-notes.md`](docs/tcl-c7k-notes.md). **Verify each package on your
own device first** — vendor app names differ across brands and firmware
versions.

### Security & privacy

- No network permissions are requested. The manifest only declares
  `RECEIVE_BOOT_COMPLETED` and `FOREGROUND_SERVICE`.
- No analytics, no crash reporting, no remote endpoints.
- No persistent storage of user data; the app keeps no preferences.
- The kill list is hard-coded in source; you can audit every package
  [in one file](app/src/main/java/com/autoclean/ram/StandbyTargets.kt).

### Disclaimer

This software is provided **as-is**, with no warranty (see [LICENSE](LICENSE)).
Disabling vendor packages or killing background services can change how your TV
behaves and, in rare cases, may interfere with OTA updates or vendor features.
Test on your own hardware before relying on it. The author is not affiliated
with TCL.

### Contributing

Issues and pull requests are welcome — especially:

- Validated `StandbyTargets` entries for other Android TV brands/models.
- Reports of packages that **must not** be killed on specific firmwares.
- Cleanups that keep the APK small and the RAM footprint flat.

Please open an issue describing the device (brand, model, Android TV version,
RAM) before sending a PR that changes the target list. Read
[`CLAUDE.md`](CLAUDE.md) (alias of `AGENTS.md`) for the codebase conventions
used by AI assistants and by the author.

### License

[MIT](LICENSE) © Tarcísio Júnior

---

## Português (pt-BR)

### O que é

AutoClean é um daemon invisível para Android TV. Um `ForegroundService` escuta
`ACTION_SCREEN_OFF` e, toda vez que a TV entra em standby, chama
`ActivityManager.killBackgroundProcesses(...)` numa lista fixa de 27 pacotes
conhecidos por consumirem RAM em background (serviços Google TV, apps de
streaming, apps secundários da TCL).

Sem UI, sem worker periódico, sem rede, sem telemetria.

- **Gatilho**: `ACTION_SCREEN_OFF` (disparado quando o painel apaga).
- **Ação**: kill fire-and-forget de processos da allow-list fixa.
- **Footprint**: APK ~1,2 MB; ~22 MB de RAM em idle.

Foi construído e validado numa **TCL C7K** (Android TV 12, MT9653GL, 2 GB de
RAM), mas a abordagem é portável para outras TVs Android com pouca memória.

### Por que existe

TVs Android baratas vêm com 1,5–2 GB de RAM e dezenas de serviços de
background do fabricante. Poucos dias depois de um factory reset o aparelho
fica lento: apps demoram a abrir, o launcher trava, o vídeo engasga. O kernel
não consegue recuperar memória rápido o suficiente porque a maioria desses
serviços acorda sozinha. Matar os mais pesados no momento exato em que o
usuário desliga a tela é a intervenção mais barata possível — sem root, sem
Shizuku, sem daemon shell.

### Como funciona

```
┌─────────────────────────────────────────────────────────────┐
│   usuário pressiona SLEEP no controle                       │
│            │                                                │
│            ▼                                                │
│   sistema dispara ACTION_SCREEN_OFF                         │
│            │                                                │
│            ▼                                                │
│   ScreenStateReceiver (registrado em runtime pelo FGS)      │
│            │                                                │
│            ▼                                                │
│   WorkManager enfileira um StandbyCleanupWorker one-shot    │
│            │                                                │
│            ▼                                                │
│   para cada pkg em StandbyTargets.all:                      │
│       ActivityManager.killBackgroundProcesses(pkg)          │
└─────────────────────────────────────────────────────────────┘
```

Por que um `ForegroundService`? A partir do Android 8 não dá para receber
`ACTION_SCREEN_OFF` via `BroadcastReceiver` declarado no manifesto — precisa
ser registrado em runtime. O FGS (invisível) existe só para hospedar esse
receiver.

Detalhes adicionais em [`docs/`](docs/):

- [`docs/architecture.md`](docs/architecture.md) — diagrama e racionais.
- [`docs/tcl-c7k-notes.md`](docs/tcl-c7k-notes.md) — peculiaridades da TCL C7K: serviços proprietários, falha do Shizuku, instabilidade do ADB, bloatware.
- [`docs/adb-runbook.md`](docs/adb-runbook.md) — comandos para setup, deploy, debug e reversão.
- [`docs/optimization-journey.md`](docs/optimization-journey.md) — histórico de redução de APK/RAM (de 5 MB/140 MB para 1,2 MB/22 MB).

### Requisitos

- **Dispositivo**: Android TV 10+ (compilado com `minSdk`/`targetSdk` 31).
- **ADB**: TVs sem porta USB usam ADB-over-Wi-Fi (veja seção abaixo).
- **Máquina de build**: Android SDK + ADB. O wrapper do Gradle resolve o
  resto (`./gradlew assembleDebug`).
- **Opcional**: acesso shell via ADB se quiser também *congelar* bloatware do
  fabricante (`pm disable-user --user 0 <pkg>`). O app em si **não precisa**
  de nenhuma permissão privilegiada.

### Como ativar o modo desenvolvedor e o ADB Wireless na TV

A maioria das TVs Android vem com as opções de desenvolvedor ocultas. O passo
a passo abaixo foi validado numa TCL C7K (Google TV / Android TV 12); em
outros modelos o caminho é parecido, mas os rótulos podem variar.

1. **Liberar Opções de Desenvolvedor**
   - Abra *Configurações → Sistema → Sobre* (em Google TV: *Configurações →
     Sistema → Sobre*).
   - Desça até **Build** (ou "Versão do sistema Android TV").
   - Pressione **OK / Enter** **7 vezes** até aparecer a mensagem
     *"Agora você é um desenvolvedor"*.

2. **Habilitar depuração USB / por rede**
   - Volte um nível e entre em *Opções do desenvolvedor* (agora visível).
   - Ative **Depuração USB** (a TV usa esse mesmo switch para ADB mesmo sem
     porta USB).
   - Ative **Depuração por rede** / **ADB Wireless** / **Depuração sem fio**
     (o nome varia). Em Google TV 11+ costuma ser *Depuração sem fio*; em
     Android TV 9–12, *ADB over network* ou *Depuração de rede*.

3. **Descobrir o IP da TV**
   - *Configurações → Rede e Internet → (sua Wi-Fi) → Endereço IP*, ou
     consulte a lista de DHCP do roteador. O IP pode mudar entre reboots —
     reserve no roteador se possível.

4. **Autorizar e conectar do computador**
   ```bash
   adb connect <IP_DA_TV>:5555
   # Aparece um popup na TV: "Permitir depuração USB deste computador?"
   # Marque "Sempre permitir deste computador" e confirme no controle.
   adb devices    # a TV deve aparecer como "device" (não "offline" / "unauthorized")
   ```

5. **Solução de problemas**
   - **`unauthorized`** → confirme o popup na TV. Se não apareceu, desligue e
     religue *Depuração sem fio*.
   - **`offline`** → a TV está em standby ou a autorização expirou. Acorde
     com o controle e tente de novo.
   - **Conexão recusada** → alguns firmwares só liberam ADB sem fio depois de
     uma autorização inicial via USB. Use cabo (se houver porta) ou o fluxo
     de *pareamento por código* do Google TV (*Depuração sem fio → Parear
     dispositivo com código* → `adb pair <IP>:<PORTA>`).
   - **IP mudando o tempo todo** → reserve o MAC da TV no roteador ou use
     mDNS: `adb connect <hostname>.local:5555`.

### Começando rápido

```bash
# 1. clonar
git clone https://github.com/<seu-usuario>/AutoClean.git
cd AutoClean

# 2. apontar o Gradle pro seu SDK local (local.properties está no .gitignore)
echo "sdk.dir=$ANDROID_HOME" > local.properties

# 3. build
./gradlew assembleDebug

# 4. habilitar ADB Wi-Fi na TV, descobrir o IP e:
adb connect <IP_DA_TV>:5555
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 5. acordar o foreground service uma vez (depois o BOOT_COMPLETED cuida)
adb shell am start -n com.autoclean/.StartActivity

# 6. (opcional) confirmar que o service está de pé
adb shell "dumpsys activity services com.autoclean | grep ServiceRecord"
```

Runbook completo — incluindo como testar o fluxo SCREEN_OFF sem perder a
sessão ADB — em [`docs/adb-runbook.md`](docs/adb-runbook.md).

### Adaptando a lista de alvos para sua TV

A fonte de verdade é
[`app/src/main/java/com/autoclean/ram/StandbyTargets.kt`](app/src/main/java/com/autoclean/ram/StandbyTargets.kt).
Adiciona/remove pacotes lá, rebuilda, reinstala. **Não inclua** pacotes do
fabricante que coordenam estado de energia, rede, HDMI-CEC ou roteamento de
entradas — matar esses pode quebrar reconexão Wi-Fi ou esconder entradas HDMI.
Exemplos concretos observados na TCL C7K estão em `docs/tcl-c7k-notes.md`.

Para descobrir candidatos na sua TV:

```bash
# listar pacotes instalados (sistema + usuário)
adb shell pm list packages

# rankear processos por memória com a TV em "idle"
adb shell "dumpsys meminfo --proto" | strings | head -100
adb shell "ps -A -o NAME,RSS,PID | sort -k2 -n -r | head -30"
```

### Opcional: congelar bloatware

A limpeza no standby mantém a RAM baixa *ao longo do tempo*. Para recuperar
os ~70 MB ocupados por apps TCL que você nunca usa, dá pra desabilitá-los
uma vez via ADB (reversível com `pm enable`). A lista usada na C7K está em
[`docs/tcl-c7k-notes.md`](docs/tcl-c7k-notes.md). **Valide cada pacote no seu
próprio dispositivo** — os nomes mudam entre marcas e versões de firmware.

### Segurança e privacidade

- Nenhuma permissão de rede é declarada. O manifest pede apenas
  `RECEIVE_BOOT_COMPLETED` e `FOREGROUND_SERVICE`.
- Sem analytics, sem crash reporting, sem endpoints remotos.
- Sem armazenamento persistente de dados do usuário; o app não guarda
  preferências.
- A lista de pacotes está em código aberto, num único arquivo
  [auditável](app/src/main/java/com/autoclean/ram/StandbyTargets.kt).

### Aviso

Software fornecido **como está**, sem garantias (ver [LICENSE](LICENSE)).
Desabilitar pacotes do fabricante ou matar processos de background pode
alterar o comportamento da sua TV e, em casos raros, interferir com
atualizações OTA ou recursos do fabricante. Teste antes de depender disso. O
autor não tem vínculo com a TCL.

### Contribuindo

Issues e pull requests são bem-vindos — em especial:

- Entradas validadas para `StandbyTargets` em outras marcas/modelos de Android TV.
- Relatos de pacotes que **não devem** ser mortos em firmwares específicos.
- Limpezas que mantenham o APK pequeno e a RAM idle baixa.

Antes de mandar um PR alterando a lista, abra uma issue descrevendo o
dispositivo (marca, modelo, versão do Android TV, RAM). Convenções do
codebase estão em [`CLAUDE.md`](CLAUDE.md) (alias de `AGENTS.md`).

### Licença

[MIT](LICENSE) © Tarcísio Júnior
