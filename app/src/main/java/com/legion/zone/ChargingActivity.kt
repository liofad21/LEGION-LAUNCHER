package com.legion.zone

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.legion.zone.power.BatteryMonitor
import com.legion.zone.ui.ChargingScene
import com.legion.zone.ui.LegionTheme

/** Full-screen charging scene. Opens on plug-in (needs overlay permission) or from the Home button. */
class ChargingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        setContent {
            LegionTheme {
                val b by remember { BatteryMonitor.flow(this@ChargingActivity) }
                    .collectAsState(BatteryMonitor.read(this@ChargingActivity))
                // Close automatically when the charger is unplugged (opened manually it stays: see README).
                var wasPlugged by remember { mutableStateOf(b.pluggedIn) }
                LaunchedEffect(b.pluggedIn) {
                    if (wasPlugged && !b.pluggedIn) finish()
                    wasPlugged = b.pluggedIn
                }
                Box(Modifier.fillMaxSize().clickable { finish() }) { ChargingScene(b) }
            }
        }
    }
}
