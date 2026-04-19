#!/system/bin/sh
# AutoClean - Script de otimização para TCL C7K
# Roda como daemon em background com permissões shell (UID 2000)
# Consumo: ~1MB de RAM vs ~140MB do app Android

LOG="/data/local/tmp/autoclean.log"
INTERVAL=1800  # 30 minutos em segundos
RAM_THRESHOLD=500  # MB - abaixo disso, limpa

log() {
    echo "$(date '+%Y-%m-%d %H:%M:%S') | $1" >> "$LOG"
    echo "$1"
}

# Manter apenas últimas 200 linhas do log
trim_log() {
    if [ -f "$LOG" ] && [ "$(wc -l < "$LOG")" -gt 200 ]; then
        tail -100 "$LOG" > "${LOG}.tmp"
        mv "${LOG}.tmp" "$LOG"
    fi
}

# Bloatware seguro para congelar (já identificado no diagnóstico)
freeze_bloatware() {
    log "=== Verificando bloatware ==="
    for pkg in \
        com.tcl.gamebar \
        com.tcl.eva \
        com.tcl.smartalexa \
        com.tcl.esticker \
        com.tcl.hotelmenu \
        com.tcl.ocean.instructions \
        com.tcl.channelplus \
        com.tcl.waterfall.overseas \
        com.tcl.gallery \
        com.tcl.hearaid \
        com.tcl.logkit \
        com.tcl.useragreement \
        com.tcl.usercenter \
        com.tcl.repairguide \
        com.tcl.t_solo \
        com.tcl.dashboard
    do
        # Só congela se ainda estiver ativo
        state=$(pm list packages -d 2>/dev/null | grep "$pkg")
        if [ -z "$state" ]; then
            pm disable-user --user 0 "$pkg" > /dev/null 2>&1
            if [ $? -eq 0 ]; then
                log "FREEZE: $pkg"
            fi
        fi
    done
}

# Apps que podem ser mortos em background sem problemas
kill_idle_processes() {
    log "=== Liberando RAM ==="
    local killed=0

    # Apps que rodam em background sem necessidade
    for pkg in \
        com.google.android.youtube.tv \
        com.google.android.youtube.tv.recommendations \
        com.android.vending \
        com.google.android.apps.tv.dreamx \
        com.google.android.tungsten.setupwraith \
        com.google.android.apps.mediashell \
        com.android.tv.settings
    do
        # Não mata se estiver em foreground
        local fg=$(dumpsys activity activities 2>/dev/null | grep "mResumedActivity" | grep "$pkg")
        if [ -z "$fg" ]; then
            am force-stop "$pkg" > /dev/null 2>&1
            killed=$((killed + 1))
        fi
    done

    log "KILL: $killed processos encerrados"
}

# Limpar cache dos apps de streaming
clean_streaming_cache() {
    log "=== Limpando cache de streaming ==="
    local cleaned=0

    for pkg in \
        com.netflix.ninja \
        com.amazon.amazonvideo.livingroom \
        com.wbd.stream \
        com.disney.disneyplus \
        com.apple.atve.androidtv.appletv \
        br.com.skymais \
        com.globo.globotv \
        com.spotify.tv.android \
        br.com.claro.now.smarttvclient \
        com.cbs.ca
    do
        # Verifica se o app está instalado
        if pm list packages 2>/dev/null | grep -q "$pkg"; then
            pm clear --cache-only "$pkg" > /dev/null 2>&1
            if [ $? -eq 0 ]; then
                cleaned=$((cleaned + 1))
            fi
        fi
    done

    log "CACHE: $cleaned apps limpos"
}

# Checar RAM disponível
get_available_ram_mb() {
    local avail=$(grep MemAvailable /proc/meminfo | awk '{print int($2/1024)}')
    echo "$avail"
}

# Checar hora atual (0-23)
get_hour() {
    date '+%H' | sed 's/^0//'
}

# Ciclo principal
main_loop() {
    log "========================================="
    log "AutoClean iniciado (PID: $$)"
    log "Intervalo: ${INTERVAL}s | RAM threshold: ${RAM_THRESHOLD}MB"
    log "========================================="

    # Primeira execução: garantir bloatware congelado
    freeze_bloatware

    while true; do
        local ram=$(get_available_ram_mb)
        local hour=$(get_hour)
        log "--- Ciclo: RAM disponível = ${ram}MB ---"

        # Se RAM abaixo do threshold, libera
        if [ "$ram" -lt "$RAM_THRESHOLD" ]; then
            log "RAM BAIXA (${ram}MB < ${RAM_THRESHOLD}MB)"
            kill_idle_processes
        fi

        # Limpeza diária entre 3h e 5h da manhã
        if [ "$hour" -ge 3 ] && [ "$hour" -le 4 ]; then
            # Verifica se já limpou hoje
            local today=$(date '+%Y-%m-%d')
            local last_clean=$(cat /data/local/tmp/autoclean_lastclean 2>/dev/null)
            if [ "$today" != "$last_clean" ]; then
                log "=== LIMPEZA DIÁRIA ==="
                kill_idle_processes
                clean_streaming_cache
                freeze_bloatware  # Regarante após possível atualização
                echo "$today" > /data/local/tmp/autoclean_lastclean
                log "=== LIMPEZA DIÁRIA CONCLUÍDA ==="
            fi
        fi

        trim_log
        sleep $INTERVAL
    done
}

# Verificar se já está rodando
if [ -f /data/local/tmp/autoclean.pid ]; then
    old_pid=$(cat /data/local/tmp/autoclean.pid)
    if kill -0 "$old_pid" 2>/dev/null; then
        echo "AutoClean já está rodando (PID: $old_pid)"
        echo "Para parar: kill $old_pid"
        exit 0
    fi
fi

# Salvar PID
echo $$ > /data/local/tmp/autoclean.pid

# Iniciar
main_loop
