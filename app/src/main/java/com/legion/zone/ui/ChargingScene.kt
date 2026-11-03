package com.legion.zone.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.legion.zone.R
import com.legion.zone.power.BatteryInfo
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * Charging animation: breathing character, a progress ring that fills with the battery level,
 * and red lightning whose density follows the real charging wattage.
 * Upgrade path: split the art into layers (hair / body / face) and swap the face by Emotion.
 */
@Composable
fun ChargingScene(b: BatteryInfo, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "fx")
    val breathe by t.animateFloat(
        1f, 1.012f, infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "breathe")
    val pulse by t.animateFloat(
        0.35f, 1f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "pulse")
    val phase by t.animateFloat(
        0f, 1f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "phase")

    val full = b.level >= 100
    val hot = b.tempC >= 41f
    val intensity = if (full) 0f else (b.watts / 30f).coerceIn(0.25f, 1f)
    val glow = if (hot) Color(0xFFFF6A1F) else LZ.Red

    Box(
        modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0xFF3A0610), LZ.Bg))),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height * 0.5f
            val r = min(size.width, size.height) * 0.42f
            val stroke = 14.dp.toPx()
            drawCircle(LZ.DeepRed.copy(alpha = 0.35f), radius = r, center = Offset(cx, cy), style = Stroke(stroke))
            drawArc(
                brush = Brush.sweepGradient(listOf(LZ.DeepRed, glow, LZ.DeepRed), center = Offset(cx, cy)),
                startAngle = -90f,
                sweepAngle = 360f * b.level.coerceIn(0, 100) / 100f,
                useCenter = false,
                topLeft = Offset(cx - r, cy - r),
                size = Size(2 * r, 2 * r),
                style = Stroke(stroke, cap = StrokeCap.Round),
                alpha = 0.6f + 0.4f * pulse,
            )
            if (intensity > 0f) {
                val rnd = Random((phase * 6f).toInt())
                repeat((3 + 6 * intensity).toInt()) {
                    val a = rnd.nextFloat() * 6.2831855f
                    var x = cx + cos(a) * r
                    var y = cy + sin(a) * r
                    val path = Path().apply { moveTo(x, y) }
                    repeat(6) {
                        x += cos(a) * r * 0.07f + (rnd.nextFloat() - 0.5f) * 36f
                        y += sin(a) * r * 0.07f + (rnd.nextFloat() - 0.5f) * 36f
                        path.lineTo(x, y)
                    }
                    drawPath(path, glow.copy(alpha = (0.35f + 0.65f * pulse) * intensity),
                        style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
                }
            }
        }

        Image(
            painterResource(R.drawable.girl), null,
            Modifier.fillMaxHeight(0.94f).graphicsLayer {
                scaleY = breathe
                transformOrigin = TransformOrigin(0.5f, 1f)
            },
            contentScale = ContentScale.Fit,
        )

        val status = when {
            full -> stringResource(R.string.fully_charged)
            hot -> stringResource(R.string.too_warm)
            b.watts >= 15f -> "${stringResource(R.string.fast_charging)} · ${b.watts.roundToInt()}W"
            else -> stringResource(R.string.charging)
        }
        Column(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("${b.level}%", color = LZ.Text, fontSize = 48.sp, fontWeight = FontWeight.Bold)
            Text(status, color = glow, fontSize = 18.sp)
        }
    }
}
