package com.legion.zone.thermal

import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import com.legion.zone.perf.PerfBackend
import com.legion.zone.perf.PerfController
import com.legion.zone.power.BatteryMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class Level { NORMAL, WARM, HOT, CRITICAL }

data class ThermalState(val level: Level = Level.NORMAL, val headroom: Float = 0f, val battTempC: Float = 0f)

object ThermalBus { val state = MutableStateFlow(ThermalState()) }

/**
 * Steps performance down as heat rises, and back up only after 30 s of calm (hysteresis).
 * Thresholds are starting values — tune them on your own tablet.
 * It never touches the system's own throttling; it only asks for less work earlier.
 */
class ThermalGuard(
    private val ctx: Context,
    private val backend: PerfBackend,
    private val scope: CoroutineScope,
) {
    private val pm = ctx.getSystemService(PowerManager::class.java)
    private var level = Level.NORMAL
    private var lastChange = 0L

    fun start() {
        scope.launch {
            while (isActive) { runCatching { tick() }; delay(5_000) }
        }
    }

    private suspend fun tick() {
        val raw = if (Build.VERSION.SDK_INT >= 30) pm.getThermalHeadroom(10) else Float.NaN
        val headroom = if (raw.isNaN()) 0f else raw          // 1.0 ≈ severe throttling
        val temp = BatteryMonitor.read(ctx).tempC
        val target = when {
            headroom >= 0.95f || temp >= 44f -> Level.CRITICAL
            headroom >= 0.85f || temp >= 41f -> Level.HOT
            headroom >= 0.70f || temp >= 38f -> Level.WARM
            else -> Level.NORMAL
        }
        val now = SystemClock.elapsedRealtime()
        when {
            target.ordinal > level.ordinal -> apply(target)
            target.ordinal < level.ordinal && now - lastChange > 30_000 ->
                apply(Level.values()[level.ordinal - 1])
        }
        ThermalBus.state.value = ThermalState(level, headroom, temp)
    }

    private suspend fun apply(l: Level) {
        val previous = level
        level = l
        lastChange = SystemClock.elapsedRealtime()
        val user = PerfController.profile.value
        if (previous == Level.CRITICAL && l != Level.CRITICAL) backend.setResolutionScale(1f)
        when (l) {
            Level.NORMAL -> backend.setRefreshRate(user.minHz, user.maxHz)
            Level.WARM -> backend.setRefreshRate(60, minOf(user.maxHz, 90))
            Level.HOT -> { backend.setRefreshRate(60, 60); backend.killBackground(emptySet()) }
            Level.CRITICAL -> {
                backend.setRefreshRate(60, 60); backend.killBackground(emptySet())
                backend.setResolutionScale(0.85f)
            }
        }
    }
}
