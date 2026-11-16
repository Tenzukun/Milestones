package com.nathan.milestones.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nathan.milestones.model.CounterColors

/**
 * Pick any colour with 3 sliders:
 *  Colour (hue) = where on the rainbow, Vividness (saturation), Brightness.
 * [initial] = the current custom colour, or null to start on pink.
 */
@Composable
fun ColorPickerDialog(
    initial: Long?,
    onPick: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    // Start from the current colour (converted to hue / saturation / brightness)
    val start = remember {
        FloatArray(3).also { android.graphics.Color.colorToHSV((initial ?: 0xFFE0457B).toInt(), it) }
    }
    var hue by remember { mutableFloatStateOf(start[0]) }
    var saturation by remember { mutableFloatStateOf(start[1].coerceAtLeast(0.2f)) }
    var brightness by remember { mutableFloatStateOf(start[2].coerceAtLeast(0.3f)) }

    val picked = CounterColors.fromHsv(hue, saturation, brightness)
    val preview = CounterColors.fromCustom(picked)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pick your colour") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Live preview of the card gradient
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(preview.brush()),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Preview", color = Color.White, fontWeight = FontWeight.Bold)
                }

                // Colour: a rainbow strip above the slider shows what each position means
                Text("Colour", style = MaterialTheme.typography.labelLarge)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp)
                        .height(10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.horizontalGradient(
                                (0..6).map { Color(CounterColors.fromHsv(it * 60f, 0.8f, 0.95f)) }
                            )
                        )
                )
                Slider(value = hue, onValueChange = { hue = it }, valueRange = 0f..359f)

                Text("Vividness", style = MaterialTheme.typography.labelLarge)
                Slider(value = saturation, onValueChange = { saturation = it }, valueRange = 0.2f..1f)

                Text("Brightness", style = MaterialTheme.typography.labelLarge)
                Slider(value = brightness, onValueChange = { brightness = it }, valueRange = 0.3f..1f)
            }
        },
        confirmButton = {
            TextButton(onClick = { onPick(picked) }) { Text("Use this colour") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}