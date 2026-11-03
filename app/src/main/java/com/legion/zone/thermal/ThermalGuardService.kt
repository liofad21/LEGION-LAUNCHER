package com.legion.zone.thermal

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import com.legion.zone.ChargingActivity
import com.legion.zone.R
import com.legion.zone.perf.PerfController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class ThermalGuardService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val powerReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            // Starting an activity from the background needs the overlay permission.
            if (Settings.canDrawOverlays(c)) {
                c.startActivity(Intent(c, ChargingActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("guard", "Thermal Guard", NotificationManager.IMPORTANCE_MIN))
        val n = Notification.Builder(this, "guard")
            .setSmallIcon(android.R.drawable.ic_lock_idle_low_battery)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.thermal))
            .build()
        if (Build.VERSION.SDK_INT >= 34) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(1, n)

        registerReceiver(powerReceiver, IntentFilter(Intent.ACTION_POWER_CONNECTED))
        ThermalGuard(this, PerfController.backend(this), scope).start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() { unregisterReceiver(powerReceiver); scope.cancel(); super.onDestroy() }
}
