package com.legion.zone

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.legion.zone.data.AppEntry
import com.legion.zone.data.AppRepo
import com.legion.zone.thermal.ThermalGuardService
import com.legion.zone.ui.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        startForegroundService(Intent(this, ThermalGuardService::class.java))
        setContent { LegionTheme { LauncherRoot() } }
    }
}

@Composable
fun LauncherRoot() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var apps by remember { mutableStateOf(emptyList<AppEntry>()) }
    LaunchedEffect(Unit) { apps = AppRepo.load(ctx) }

    // page 0 = Feed (left-most), 1 = Home, 2 = Game Zone
    val pager = rememberPagerState(initialPage = 1) { 3 }
    var drawer by remember { mutableStateOf(false) }
    BackHandler(drawer) { drawer = false }

    Box(Modifier.fillMaxSize().background(LZ.Bg)) {
        HorizontalPager(pager) { page ->
            when (page) {
                0 -> FeedScreen()
                1 -> HomeScreen(apps, onOpenDrawer = { drawer = true },
                    onOpenGameZone = { scope.launch { pager.animateScrollToPage(2) } })
                else -> GameZoneScreen(apps)
            }
        }
        AnimatedVisibility(drawer, enter = slideInVertically { it }, exit = slideOutVertically { it }) {
            Box(Modifier.fillMaxSize().background(LZ.Bg.copy(alpha = 0.96f))) {
                AppDrawer(apps) { AppRepo.launch(ctx, it.pkg); drawer = false }
            }
        }
    }
}
