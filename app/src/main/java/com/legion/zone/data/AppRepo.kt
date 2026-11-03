package com.legion.zone.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Collator

data class AppEntry(val label: String, val pkg: String, val icon: ImageBitmap, val isGame: Boolean)

object AppRepo {
    suspend fun load(ctx: Context): List<AppEntry> = withContext(Dispatchers.IO) {
        val pm = ctx.packageManager
        val q = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(q, 0)
            .filter { it.activityInfo.packageName != ctx.packageName }
            .distinctBy { it.activityInfo.packageName }
            .map { ri ->
                val info = ri.activityInfo.applicationInfo
                @Suppress("DEPRECATION")
                val game = info.category == ApplicationInfo.CATEGORY_GAME ||
                    (info.flags and ApplicationInfo.FLAG_IS_GAME) != 0
                AppEntry(
                    ri.loadLabel(pm).toString(), ri.activityInfo.packageName,
                    ri.loadIcon(pm).toBitmap(128, 128).asImageBitmap(), game,
                )
            }
            .sortedWith(compareBy<AppEntry, String>(Collator.getInstance()) { it.label })
    }

    fun launch(ctx: Context, pkg: String) {
        ctx.packageManager.getLaunchIntentForPackage(pkg)?.let { ctx.startActivity(it) }
    }

    /** Works for Latin and Arabic first letters; everything else goes under '#'. */
    fun sectionOf(label: String): String {
        val c = label.trim().firstOrNull() ?: return "#"
        return if (c.isLetter()) c.uppercaseChar().toString() else "#"
    }

    private val dockTokens = listOf("dialer", "chrome", "camera", "messag", "vending", "settings")
    fun defaultDock(apps: List<AppEntry>): List<AppEntry> =
        dockTokens.mapNotNull { t -> apps.firstOrNull { it.pkg.contains(t) } }.distinct()
}
