package com.legion.zone.power

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.abs

data class BatteryInfo(
    val level: Int = 0,
    val pluggedIn: Boolean = false,
    val tempC: Float = 0f,
    val watts: Float = 0f,
)

object BatteryMonitor {
    fun read(ctx: Context): BatteryInfo {
        val i = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        return if (i == null) BatteryInfo() else parse(ctx, i)
    }

    fun flow(ctx: Context): Flow<BatteryInfo> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) { trySend(parse(c, i)) }
        }
        val sticky = ctx.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        sticky?.let { trySend(parse(ctx, it)) }
        awaitClose { ctx.unregisterReceiver(receiver) }
    }

    private fun parse(ctx: Context, i: Intent): BatteryInfo {
        val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
        val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) * 100 / scale
        val plugged = i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
        val tempC = i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f
        val mv = i.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
        // CURRENT_NOW is in microamps; its sign convention differs between devices -> abs().
        val ua = ctx.getSystemService(BatteryManager::class.java)
            .getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val watts = if (plugged) abs(ua) / 1_000_000f * mv / 1000f else 0f
        return BatteryInfo(level, plugged, tempC, watts)
    }
}
