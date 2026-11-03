package com.legion.zone.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.legion.zone.data.AppEntry

object LZ {
    val Bg = Color(0xFF0A0306)
    val Red = Color(0xFFFF1E3C)
    val DeepRed = Color(0xFF7A0A1E)
    val Text = Color(0xFFFFE9EC)
}

@Composable
fun LegionTheme(content: @Composable () -> Unit) = MaterialTheme(
    colorScheme = darkColorScheme(
        primary = LZ.Red, background = LZ.Bg, surface = Color(0xFF14070B), onSurface = LZ.Text,
    ),
    content = content,
)

@Composable
fun GlassPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0x99300008), Color(0x99100003))))
            .border(BorderStroke(1.dp, Brush.linearGradient(listOf(LZ.Red, LZ.DeepRed))), shape)
            .padding(14.dp),
        content = content,
    )
}

@Composable
fun GlassButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    GlassPanel(modifier.clickable(onClick = onClick)) {
        Text(text, color = LZ.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    GlassPanel(modifier) {
        Text(label, color = LZ.Text.copy(alpha = 0.7f), fontSize = 12.sp)
        Text(value, color = LZ.Red, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AppTile(app: AppEntry, modifier: Modifier = Modifier, showLabel: Boolean = true, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(app.icon, app.label, Modifier.size(56.dp))
        if (showLabel) Text(app.label, color = LZ.Text, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
