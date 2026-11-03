package com.legion.zone.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.legion.zone.data.AppEntry
import com.legion.zone.data.AppRepo
import kotlinx.coroutines.launch

@Composable
fun AppDrawer(apps: List<AppEntry>, onLaunch: (AppEntry) -> Unit) {
    val grid = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val sections = remember(apps) { apps.map { AppRepo.sectionOf(it.label) }.distinct() }
    val firstIndex = remember(apps) {
        sections.associateWith { s -> apps.indexOfFirst { AppRepo.sectionOf(it.label) == s }.coerceAtLeast(0) }
    }
    var active by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(96.dp),
            state = grid,
            contentPadding = PaddingValues(start = 12.dp, end = 44.dp, top = 12.dp, bottom = 12.dp),
        ) {
            items(apps, key = { it.pkg }) { app -> AppTile(app) { onLaunch(app) } }
        }
        AlphabetIndex(
            letters = sections,
            modifier = Modifier.align(Alignment.CenterEnd),
            onSelect = { s -> active = s; scope.launch { grid.scrollToItem(firstIndex[s] ?: 0) } },
            onEnd = { active = null },
        )
        active?.let {
            Text(
                it, color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center).clip(RoundedCornerShape(24.dp))
                    .background(LZ.DeepRed.copy(alpha = 0.85f)).padding(horizontal = 36.dp, vertical = 20.dp),
            )
        }
    }
}

/** Vertical A–Z strip: tap or drag to jump. Letters come from the actual app labels (Latin + Arabic). */
@Composable
fun AlphabetIndex(letters: List<String>, modifier: Modifier, onSelect: (String) -> Unit, onEnd: () -> Unit) {
    var h by remember { mutableIntStateOf(1) }
    fun pick(y: Float) {
        if (letters.isEmpty()) return
        val i = ((y / h) * letters.size).toInt().coerceIn(0, letters.lastIndex)
        onSelect(letters[i])
    }
    Column(
        modifier.fillMaxHeight().width(36.dp).onSizeChanged { h = it.height.coerceAtLeast(1) }
            .pointerInput(letters) {
                detectTapGestures(onPress = { pick(it.y); tryAwaitRelease(); onEnd() })
            }
            .pointerInput(letters) {
                detectVerticalDragGestures(
                    onDragStart = { pick(it.y) },
                    onDragEnd = { onEnd() },
                    onDragCancel = { onEnd() },
                ) { change, _ -> pick(change.position.y) }
            },
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        letters.forEach { Text(it, color = LZ.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
    }
}
