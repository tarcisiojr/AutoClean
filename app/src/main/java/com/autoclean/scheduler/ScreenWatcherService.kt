package com.autoclean.scheduler

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.autoclean.R

/**
 * Foreground Service que mantém o ScreenStateReceiver registrado
 * dinamicamente — necessário a partir do Android 8 para receber
 * ACTION_SCREEN_OFF. Ocioso: só reage quando a TV entra em standby.
 */
class ScreenWatcherService : Service() {

    private val receiver = ScreenStateReceiver()
    private var receiverRegistered = false

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "ScreenWatcherService iniciado")
        startInForeground()
        registerScreenReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // START_STICKY: o sistema recria o service se for morto
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        if (receiverRegistered) {
            try {
                unregisterReceiver(receiver)
            } catch (e: Exception) {
                Log.w(TAG, "Falha ao desregistrar receiver: ${e.message}")
            }
            receiverRegistered = false
        }
        Log.i(TAG, "ScreenWatcherService encerrado")
        super.onDestroy()
    }

    private fun registerScreenReceiver() {
        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        registerReceiver(receiver, filter)
        receiverRegistered = true
    }

    private fun startInForeground() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.service_channel_name),
            NotificationManager.IMPORTANCE_MIN
        )
        nm.createNotificationChannel(channel)

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.service_notification_title))
            .setContentText(getString(R.string.service_notification_text))
            .setSmallIcon(R.drawable.banner)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val TAG = "ScreenWatcherService"
        private const val CHANNEL_ID = "autoclean_standby_watcher"
        private const val NOTIFICATION_ID = 1001
    }
}
