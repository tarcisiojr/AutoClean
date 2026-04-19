package com.autoclean.scheduler

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.autoclean.ram.StandbyTargets

/**
 * Worker one-shot acionado quando a TV entra em standby (SCREEN_OFF).
 * Itera uma lista de pacotes-alvo conhecidos e chama killBackgroundProcesses
 * em cada um — fire-and-forget, sem dependências de scan/UI.
 */
class StandbyCleanupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val activityManager = applicationContext
            .getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        var killed = 0
        for (pkg in StandbyTargets.all) {
            activityManager.killBackgroundProcesses(pkg)
            killed++
        }
        Log.i(TAG, "Limpeza de standby: $killed pacotes processados")
        return Result.success()
    }

    companion object {
        private const val TAG = "StandbyCleanupWorker"
        const val WORK_NAME = "autoclean_standby_cleanup"
    }
}
