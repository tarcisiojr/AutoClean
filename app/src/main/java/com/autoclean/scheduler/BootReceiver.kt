package com.autoclean.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Receiver para BOOT_COMPLETED: reinicia o ScreenWatcherService após reboot.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Log.i(TAG, "Boot detectado — iniciando ScreenWatcherService")
        context.startForegroundService(Intent(context, ScreenWatcherService::class.java))
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
