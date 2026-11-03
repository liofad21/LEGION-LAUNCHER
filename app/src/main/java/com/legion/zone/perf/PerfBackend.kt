package com.legion.zone.perf

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext

enum class Profile(val minHz: Int, val maxHz: Int) {
    COOL(60, 60),
    BALANCED(60, 90),
    MAX(120, 120),
}

/** Every control returns whether it actually worked, so the UI can be honest about it. */
interface PerfBackend {
    suspend fun setRefreshRate(minHz: Int, maxHz: Int): Boolean
    suspend fun killBackground(except: Set<String>): Int
    suspend fun setResolutionScale(scale: Float): Boolean
}

/**
 * Level A: works with normal permissions (WRITE_SETTINGS + KILL_BACKGROUND_PROCESSES).
 * Refresh-rate keys are AOSP names; Lenovo ZUI may ignore or override them.
 */
class SettingsBackend(private val ctx: Context) : PerfBackend {
    override suspend fun setRefreshRate(minHz: Int, maxHz: Int): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            if (!Settings.System.canWrite(ctx)) return@runCatching false
            Settings.System.putFloat(ctx.contentResolver, "min_refresh_rate", minHz.toFloat())
            Settings.System.putFloat(ctx.contentResolver, "peak_refresh_rate", maxHz.toFloat())
        }.getOrDefault(false)
    }

    override suspend fun killBackground(except: Set<String>): Int = withContext(Dispatchers.IO) {
        val am = ctx.getSystemService(ActivityManager::class.java)
        val q = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        ctx.packageManager.queryIntentActivities(q, 0)
            .map { it.activityInfo.packageName }.distinct()
            .filter { it != ctx.packageName && it !in except }
            .onEach { am.killBackgroundProcesses(it) }
            .size
    }

    // Changing resolution needs shell (wm size). Not possible at this level.
    override suspend fun setResolutionScale(scale: Float): Boolean = false
}

/**
 * Level B (TODO): Shizuku UserService running shell commands, e.g.
 *   wm size <W>x<H>                     (resolution scale)
 *   cmd game set --mode performance <pkg>   (GameManager mode)
 *   settings put system peak_refresh_rate 120
 *   dumpsys thermalservice              (all sensors)
 * Until implemented it delegates to Level A.
 */
class ShizukuBackend(ctx: Context) : PerfBackend by SettingsBackend(ctx)

object PerfController {
    @Volatile private var backend: PerfBackend? = null
    val profile = MutableStateFlow(Profile.BALANCED)

    fun backend(ctx: Context): PerfBackend =
        backend ?: SettingsBackend(ctx.applicationContext).also { backend = it }

    fun setBackend(b: PerfBackend) { backend = b }

    suspend fun apply(ctx: Context, p: Profile): Boolean {
        profile.value = p
        return backend(ctx).setRefreshRate(p.minHz, p.maxHz)
    }
}
