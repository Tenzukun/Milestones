package com.nathan.milestones.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.sin
import kotlin.random.Random

/** One falling piece of confetti. All numbers are picked randomly so every piece moves differently. */
private data class ConfettiPiece(
    val startX: Float,      // 0..1 across the screen
    val delay: Float,       // 0..0.35, so pieces don't all start at once
    val speed: Float,       // how far down it falls
    val sway: Float,        // how far it drifts left/right (dp)
    val swaySpeed: Float,
    val phase: Float,
    val rotation: Float,
    val spin: Float,        // degrees it turns while falling
    val width: Float,       // dp
    val height: Float,      // dp
    val color: Color
)

private val ConfettiColors = listOf(
    Color(0xFFE0457B), Color(0xFFFF9671), Color(0xFFFFC94A), Color(0xFF7ED957),
    Color(0xFF4DD0E1), Color(0xFF4F7BFF), Color(0xFFC471FF), Color(0xFFFFFFFF)
)

private fun randomPiece() = ConfettiPiece(
    startX = Random.nextFloat(),
    delay = Random.nextFloat() * 0.35f,
    speed = 0.8f + Random.nextFloat() * 0.5f,
    sway = 10f + Random.nextFloat() * 30f,
    swaySpeed = 4f + Random.nextFloat() * 8f,
    phase = Random.nextFloat() * 6.28f,
    rotation = Random.nextFloat() * 360f,
    spin = 360f + Random.nextFloat() * 720f,
    width = 6f + Random.nextFloat() * 6f,
    height = 10f + Random.nextFloat() * 8f,
    color = ConfettiColors.random()
)

/**
 * Full-screen confetti that falls once (about 4 seconds), then calls [onFinished].
 * It only draws on top: taps still go through to the screen underneath.
 */
@Composable
fun ConfettiOverlay(onFinished: () -> Unit, durationMillis: Int = 4000) {
    val pieces = remember { List(140) { randomPiece() } }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(durationMillis, easing = LinearEasing))
        onFinished()
    }

    Canvas(Modifier.fillMaxSize()) {
        val t = progress.value
        // Fade everything out during the last 20%
        val alpha = if (t > 0.8f) ((1f - t) / 0.2f).coerceIn(0f, 1f) else 1f

        pieces.forEach { p ->
            // Each piece starts after its own small delay
            val local = ((t - p.delay) / (1f - p.delay)).coerceIn(0f, 1f)
            if (local <= 0f) return@forEach

            val w = p.width.dp.toPx()
            val h = p.height.dp.toPx()
            val x = size.width * p.startX + sin(local * p.swaySpeed + p.phase) * p.sway.dp.toPx()
            val y = -h + (size.height + 2 * h) * local * p.speed

            rotate(degrees = p.rotation + local * p.spin, pivot = Offset(x, y)) {
                drawRect(
                    color = p.color,
                    topLeft = Offset(x - w / 2, y - h / 2),
                    size = Size(w, h),
                    alpha = alpha
                )
            }
        }
    }
}