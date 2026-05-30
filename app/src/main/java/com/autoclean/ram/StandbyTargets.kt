package com.autoclean.ram

/**
 * Lista de pacotes conhecidos que consomem RAM em background numa TCL C7K
 * e podem ser encerrados com segurança durante o standby.
 *
 * Critérios para entrar na lista:
 * - App costuma rodar em background mesmo quando não está em uso
 * - Matar o processo não causa perda de estado perceptível pelo usuário
 * - App é reiniciado automaticamente quando necessário
 */
object StandbyTargets {

    /** Serviços Google que reiniciam sozinhos e consomem RAM sem necessidade */
    private val googleServices = listOf(
        "com.google.android.youtube.tv",
        "com.google.android.youtube.tv.recommendations",
        "com.google.android.youtube.tvmusic",
        "com.google.android.videos",
        "com.google.android.play.games",
        "com.android.vending",
        "com.google.android.apps.tv.dreamx",
        "com.google.android.tungsten.setupwraith",
        "com.google.android.apps.mediashell",
        "com.google.android.tvrecommendations",
        "com.google.android.katniss"
    )

    /** Apps de streaming que mantêm sessão viva em background sem necessidade */
    private val streamingApps = listOf(
        "com.netflix.ninja",
        "com.amazon.amazonvideo.livingroom",
        "com.wbd.stream",
        "com.disney.disneyplus",
        "com.apple.atve.androidtv.appletv",
        "com.spotify.tv.android",
        "br.com.skymais",
        "br.com.claro.now.smarttvclient",
        "com.globo.globotv",
        "com.cbs.ca"
    )

    /** Apps TCL secundários que podem ficar em background */
    private val tclSecondary = listOf(
        "com.tcl.exhibit",
        "com.tcl.partnercustomizer"
        // Removidos:
        // - com.tcl.magiconnectfree e com.tcl.suspension: atraso de ~30s para
        //   reconectar Wi-Fi ao sair do standby.
        // - com.tcl.tv e com.tcl.ui_mediaCenter: faziam dispositivos HDMI-CEC
        //   (ex.: PS5 atrás do receiver) sumirem da lista de entradas.
        // Ver docs/tcl-c7k-notes.md.
    )

    /** Todos os pacotes-alvo da limpeza de standby */
    val all: List<String> = googleServices + streamingApps + tclSecondary
}
