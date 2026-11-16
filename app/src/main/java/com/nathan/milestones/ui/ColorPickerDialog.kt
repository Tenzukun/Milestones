package com.nathan.milestones.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.nathan.milestones.model.CounterColors

/**
 * Pick any colour, two ways (they stay in sync):
 *  - 3 sliders: Colour (hue), Vividness (saturation), Brightness
 *  - or type a hex code, e.g. #E0457B
 * [initial] = the current custom colour, or null to start on pink.
 */
@Composable
fun ColorPickerDialog(
    initial: Long?,
    onPick: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val startColor = initial ?: 0xFFE0457B
    // Start from the current colour (converted to hue / saturation / brightness)
    val start = remember {
        FloatArray(3).also { android.graphics.Color.colorToHSV(startColor.toInt(), it) }
    }
    var hue by remember { mutableFloatStateOf(start[0]) }
    var saturation by remember { mutableFloatStateOf(start[1]) }
    var brightness by remember { mutableFloatStateOf(start[2]) }
    var hexText by remember { mutableStateOf(toHex(startColor)) }

    val picked = CounterColors.fromHsv(hue, saturation, brightness)
    val preview = CounterColors.fromCustom(picked)
    val hexIsValid = parseHex(hexText) != null

    /** Moving a slider rewrites the hex box to match. */
    fun syncHexFromSliders() {
        hexText = toHex(CounterColors.fromHsv(hue, saturation, brightness))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pick your colour") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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

                // Hex code box: typing a full code moves the sliders to match
                OutlinedTextField(
                    value = hexText,
                    onValueChange = { input ->
                        // Keep only # and 0-9 / A-F, max "#RRGGBB"
                        val cleaned = "#" + input.uppercase()
                            .filter { it in "0123456789ABCDEF" }
                            .take(6)
                        hexText = cleaned
                        parseHex(cleaned)?.let { color ->
                            val hsv = FloatArray(3)
                            android.graphics.Color.colorToHSV(color.toInt(), hsv)
                            hue = hsv[0]
                            saturation = hsv[1]
                            brightness = hsv[2]
                        }
                    },
                    label = { Text("Hex code") },
                    placeholder = { Text("#E0457B") },
                    singleLine = true,
                    isError = !hexIsValid,
                    supportingText = { if (!hexIsValid) Text("Use 6 characters, e.g. #E0457B") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.fillMaxWidth()
                )

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
                Slider(
                    value = hue,
                    onValueChange = { hue = it; syncHexFromSliders() },
                    valueRange = 0f..359f
                )

                Text("Vividness", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = saturation,
                    onValueChange = { saturation = it; syncHexFromSliders() },
                    valueRange = 0f..1f
                )

                Text("Brightness", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = brightness,
                    onValueChange = { brightness = it; syncHexFromSliders() },
                    valueRange = 0f..1f
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onPick(parseHex(hexText) ?: picked) }, // exact hex if typed
                enabled = hexIsValid
            ) { Text("Use this colour") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/** 0xFFE0457B -> "#E0457B" */
private fun toHex(color: Long): String = "#%06X".format(color and 0xFFFFFFL)

/** "#E0457B" or "E0457B" -> 0xFFE0457B, or null if it isn't a full 6-character code */
private fun parseHex(text: String): Long? {
    val digits = text.removePrefix("#")
    if (digits.length != 6) return null
    return digits.toLongOrNull(16)?.let { it or 0xFF000000L }
}