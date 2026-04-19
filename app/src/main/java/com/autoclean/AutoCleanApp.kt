package com.autoclean

import android.app.Application
import android.content.Intent
import com.autoclean.scheduler.ScreenWatcherService

class AutoCleanApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startForegroundService(Intent(this, ScreenWatcherService::class.java))
    }
}
