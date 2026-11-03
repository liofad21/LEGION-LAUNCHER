package com.legion.zone.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.legion.zone.ChargingActivity
import com.legion.zone.R
import com.legion.zone.data.AppEntry
import com.legion.zone.data.AppRepo
import com.legion.zone.power.BatteryMonitor
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(apps: List<AppEntry>, onOpenDrawer: () -> Unit, onOpenGameZone: () -> Unit) {
    val ctx = LocalContext.current
    val b by remember { BatteryMonitor.flow(ctx) }.collectAsState(BatteryMonitor.read(ctx))
    val now by produceState(LocalDateTime.now()) { while (true) { value = LocalDateTime.now(); delay(1000) } }
    var drag by remember { mutableFloatStateOf(0f) }
    val dock = remember(apps) { AppRepo.defaultDock(apps) }

    Box(
        Modifier.fillMaxSize().pointerInput(Unit) {
            // Swipe up anywhere on Home -> app drawer.
            detectVerticalDragGestures(
                onDragEnd = { if (drag < -140f) onOpenDrawer(); drag = 0f },
                onDragCancel = { drag = 0f },
            ) { _, dy -> drag += dy }
        },
    ) {
        // The wallpaper already contains the character, so Home shows it as-is.
        Image(painterResource(R.drawable.wallpaper), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)

        Column(
            Modifier.align(Alignment.TopStart).statusBarsPadding().padding(16.dp).width(300.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GlassPanel(Modifier.fillMaxWidth()) {
                Text(now.format(DateTimeFormatter.ofPattern("HH:mm")), color = LZ.Text, fontSize = 56.sp, fontWeight = FontWeight.Light)
                Text(now.format(DateTimeFormatter.ofPattern("EEE, MMM d")), color = LZ.Text.copy(alpha = 0.8f), fontSize = 16.sp)
            }
            GlassPanel(Modifier.fillMaxWidth()) {
                Text("${b.level}%", color = LZ.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { b.level / 100f }, color = LZ.Red, trackColor = LZ.DeepRed.copy(alpha = 0.4f), modifier = Modifier.fillMaxWidth())
                if (b.pluggedIn) Text(stringResource(R.string.charging), color = LZ.Red, fontSize = 13.sp)
            }
        }

        Column(
            Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(16.dp).width(260.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GlassButton(stringResource(R.string.game_zone), Modifier.fillMaxWidth(), onOpenGameZone)
            GlassButton(stringResource(R.string.all_apps), Modifier.fillMaxWidth(), onOpenDrawer)
            GlassButton(stringResource(R.string.charging_preview), Modifier.fillMaxWidth()) {
                ctx.startActivity(Intent(ctx, ChargingActivity::class.java))
            }
            GlassButton(stringResource(R.string.settings), Modifier.fillMaxWidth()) {
                ctx.startActivity(Intent(Settings.ACTION_SETTINGS))
            }
        }

        // Dock (swipe-up to expand is the next step; for now it opens the drawer)
        val pill = RoundedCornerShape(32.dp)
        Row(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(12.dp)
                .clip(pill).background(Color(0xAA1A0308))
                .border(BorderStroke(1.dp, Brush.linearGradient(listOf(LZ.Red, LZ.DeepRed))), pill)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            dock.forEach { app -> AppTile(app, showLabel = false) { AppRepo.launch(ctx, app.pkg) } }
            Text("⌃", color = LZ.Red, fontSize = 28.sp, modifier = Modifier.clip(RoundedCornerShape(12.dp)).padding(horizontal = 12.dp)
                .pointerInput(Unit) { detectVerticalDragGestures(onDragEnd = { onOpenDrawer() }) { _, _ -> } })
        }
    }
}

@Composable
fun FeedScreen() {
    Column(
        Modifier.fillMaxSize().background(LZ.Bg).statusBarsPadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.feed_title), color = LZ.Text, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        GlassPanel(Modifier.fillMaxWidth()) { Text(stringResource(R.string.feed_weather), color = LZ.Text) }
        GlassPanel(Modifier.fillMaxWidth()) { Text(stringResource(R.string.feed_tasks), color = LZ.Text) }
        GlassPanel(Modifier.fillMaxWidth()) { Text(stringResource(R.string.feed_assistant), color = LZ.Text) }
    }
}
