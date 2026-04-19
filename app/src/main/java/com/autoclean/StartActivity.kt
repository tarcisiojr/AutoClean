package com.autoclean

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.autoclean.scheduler.ScreenWatcherService

/**
 * Activity invisível — garante que o ScreenWatcherService esteja rodando
 * e se encerra imediatamente. Serve para o primeiro launch após instalação
 * (antes do primeiro BOOT_COMPLETED) e para reiniciar manualmente se o
 * usuário precisar.
 */
class StartActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startForegroundService(Intent(this, ScreenWatcherService::class.java))
        finish()
    }
}
