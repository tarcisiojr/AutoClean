# Preserva Worker, Receiver, Service e Application (instanciados via reflexão pelo sistema)
-keep class com.autoclean.AutoCleanApp
-keep class com.autoclean.StartActivity
-keep class com.autoclean.scheduler.BootReceiver
-keep class com.autoclean.scheduler.ScreenStateReceiver
-keep class com.autoclean.scheduler.ScreenWatcherService
-keep class com.autoclean.scheduler.StandbyCleanupWorker {
    <init>(android.content.Context, androidx.work.WorkerParameters);
}

# WorkManager
-keep class androidx.work.impl.** { *; }
