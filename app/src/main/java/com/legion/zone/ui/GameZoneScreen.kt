package com.legion.zone.ui

import android.Manifest
import android.app.AppOpsManager
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Process
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.legion.zone.R
import com.legion.zone.assistant.AssistantEngine
import com.legion.zone.data.AppEntry
import com.legion.zone.data.AppRepo
import com.legion.zone.perf.PerfController
import com.legion.zone.perf.Profile
import com.legion.zone.power.BatteryMonitor
import com.legion.zone.thermal.ThermalBus
import kotlinx.coroutines.launch

@Composable
fun GameZoneScreen(apps: List<AppEntry>) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf(R.string.tab_home, R.string.tab_library, R.string.tab_performance, R.string.tab_tools, R.string.tab_assistant)

    Row(
        Modifier.fillMaxSize()
            .background(Brush.radialGradient(listOf(Color(0xFF2A050C), LZ.Bg)))
            .statusBarsPadding().navigationBarsPadding(),
    ) {
        // The character is the anchor of the Game Zone.
        Box(Modifier.weight(0.42f).fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
            Image(painterResource(R.drawable.girl), null, Modifier.fillMaxHeight(0.98f), contentScale = ContentScale.Fit)
        }
        Column(Modifier.weight(0.58f).fillMaxHeight().padding(16.dp)) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tabs.forEachIndexed { i, r ->
                    FilterChip(selected = tab == i, onClick = { tab = i }, label = { Text(stringResource(r)) })
                }
            }
            Spacer(Modifier.height(12.dp))
            val body = Modifier.weight(1f).fillMaxWidth()
            when (tab) {
                0 -> ZoneHome(body)
                1 -> ZoneLibrary(apps, body)
                2 -> ZonePerformance(body)
                3 -> PermissionsPanel(body)
                else -> ZoneAssistant(body)
            }
        }
    }
}

@Composable
private fun ZoneHome(modifier: Modifier) {
    val ctx = LocalContext.current
    val b by remember { BatteryMonitor.flow(ctx) }.collectAsState(BatteryMonitor.read(ctx))
    val th by ThermalBus.state.collectAsState()
    val prof by PerfController.profile.collectAsState()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(stringResource(R.string.battery), "${b.level}%", Modifier.weight(1f))
            StatCard(stringResource(R.string.temperature), "%.1f°C".format(b.tempC), Modifier.weight(1f))
            StatCard(stringResource(R.string.thermal), th.level.name, Modifier.weight(1f))
        }
        GlassPanel(Modifier.fillMaxWidth()) {
            Text("${stringResource(R.string.profile)}: ${prof.name}", color = LZ.Text, fontSize = 16.sp)
        }
    }
}

@Composable
private fun ZoneLibrary(apps: List<AppEntry>, modifier: Modifier) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val games = remember(apps) { apps.filter { it.isGame } }
    if (games.isEmpty()) {
        Text(stringResource(R.string.no_games), color = LZ.Text.copy(alpha = 0.7f), modifier = modifier)
    } else {
        LazyVerticalGrid(GridCells.Adaptive(96.dp), modifier) {
            items(games, key = { it.pkg }) { g ->
                AppTile(g) {
                    // Apply the chosen profile, then launch.
                    scope.launch { PerfController.apply(ctx, PerfController.profile.value) }
                    AppRepo.launch(ctx, g.pkg)
                }
            }
        }
    }
}

private fun labelOf(p: Profile) = when (p) {
    Profile.COOL -> R.string.profile_cool
    Profile.BALANCED -> R.string.profile_balanced
    Profile.MAX -> R.string.profile_max
}

@Composable
private fun ZonePerformance(modifier: Modifier) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val prof by PerfController.profile.collectAsState()
    var msg by remember { mutableStateOf("") }
    val ok = stringResource(R.string.applied)
    val fail = stringResource(R.string.apply_failed)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Profile.values().forEach { p ->
                FilterChip(
                    selected = p == prof,
                    onClick = { scope.launch { msg = if (PerfController.apply(ctx, p)) ok else fail } },
                    label = { Text(stringResource(labelOf(p))) },
                )
            }
        }
        if (msg.isNotEmpty()) Text(msg, color = LZ.Red)
        GlassPanel(Modifier.fillMaxWidth()) { Text(stringResource(R.string.perf_note), color = LZ.Text) }
    }
}

private class PermItem(val label: Int, val granted: Boolean, val intent: Intent?)

@Composable
private fun PermissionsPanel(modifier: Modifier) {
    val ctx = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }

    val perms = remember(tick) {
        val pkg = Uri.parse("package:${ctx.packageName}")
        val nm = ctx.getSystemService(NotificationManager::class.java)
        val ops = ctx.getSystemService(AppOpsManager::class.java)
        val usage = ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName) == AppOpsManager.MODE_ALLOWED
        listOf(
            PermItem(R.string.perm_overlay, Settings.canDrawOverlays(ctx), Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, pkg)),
            PermItem(R.string.perm_write, Settings.System.canWrite(ctx), Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, pkg)),
            PermItem(R.string.perm_usage, usage, Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)),
            PermItem(R.string.perm_dnd, nm.isNotificationPolicyAccessGranted, Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)),
            PermItem(R.string.perm_notifications, nm.areNotificationsEnabled(),
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)),
            PermItem(R.string.perm_secure,
                ctx.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED, null),
        )
    }

    LazyColumn(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(perms) { p ->
            GlassPanel(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(p.label), color = LZ.Text, modifier = Modifier.weight(1f))
                    if (p.granted) Text(stringResource(R.string.granted), color = LZ.Red)
                    else if (p.intent != null) Button(onClick = { ctx.startActivity(p.intent) }) { Text(stringResource(R.string.grant)) }
                }
                if (!p.granted && p.intent == null) Text(stringResource(R.string.perm_secure_cmd), color = LZ.Text.copy(alpha = 0.7f), fontSize = 11.sp)
            }
        }
        item {
            Button(onClick = { ctx.startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }) { Text(stringResource(R.string.set_default_home)) }
        }
    }
}

private class Msg(val fromUser: Boolean, val text: String)

@Composable
private fun ZoneAssistant(modifier: Modifier) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    val msgs = remember { mutableStateListOf<Msg>() }
    Column(modifier) {
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(msgs) { m ->
                GlassPanel(Modifier.fillMaxWidth()) {
                    Text(m.text, color = if (m.fromUser) LZ.Text else LZ.Red)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.assistant_hint)) }, singleLine = true,
            )
            Button(onClick = {
                val t = input.trim()
                if (t.isNotEmpty()) {
                    input = ""
                    msgs += Msg(true, t)
                    scope.launch { msgs += Msg(false, AssistantEngine.reply(ctx, t).text) }
                }
            }) { Text(stringResource(R.string.send)) }
        }
    }
}
