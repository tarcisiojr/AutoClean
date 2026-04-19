package com.autoclean.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

/**
 * Receiver dinâmico que dispara limpeza quando a TV entra em standby.
 * Registrado via ScreenWatcherService (não pode ser declarado no manifest
 * para ACTION_SCREEN_OFF a partir do Android 8).
 */
class ScreenStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_SCREEN_OFF) return

        Log.i(TAG, "SCREEN_OFF detectado — agendando limpeza de standby")

        val request = OneTimeWorkRequestBuilder<StandbyCleanupWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            StandbyCleanupWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    companion object {
        private const val TAG = "ScreenStateReceiver"
    }
}
