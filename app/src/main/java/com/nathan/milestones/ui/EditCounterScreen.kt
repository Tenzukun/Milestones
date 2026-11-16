package com.nathan.milestones.ui

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nathan.milestones.logic.MilestoneCalculator
import com.nathan.milestones.model.CountMode
import com.nathan.milestones.model.CountUnit
import com.nathan.milestones.model.Counter
import com.nathan.milestones.model.CounterColors
import com.nathan.milestones.model.EMOJI_CHOICES
import java.time.LocalDate
import java.util.UUID

/**
 * Create or edit a date. existing = null means "new".
 * onDelete = null hides the delete button.
 */
@Composable
fun EditCounterScreen(
    existing: Counter?,
    onSave: (Counter) -> Unit,
    onDelete: (() -> Unit)?,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var startDate by remember { mutableStateOf(existing?.startDate ?: LocalDate.now()) }
    var mode by remember { mutableStateOf(existing?.mode ?: CountMode.MILESTONE) }
    var unit by remember { mutableStateOf(existing?.unit ?: CountUnit.MONTHS) }
    var customMessage by remember { mutableStateOf(existing?.customMessage ?: "") }
    var notify by remember { mutableStateOf(existing?.notify ?: true) }
    var notifyDayBefore by remember { mutableStateOf(existing?.notifyDayBefore ?: false) }
    var emoji by remember { mutableStateOf(existing?.emoji ?: "❤️") }
    var colorIndex by remember { mutableStateOf(existing?.colorIndex ?: 0) }
    var confirmDelete by remember { mutableStateOf(false) }

    BackHandler(onBack = onCancel)

    // Live preview of what the card and widget will show
    val preview = Counter(
        id = existing?.id ?: "preview",
        title = title.ifBlank { "Untitled" },
        startDate = startDate,
        mode = mode,
        unit = unit,
        customMessage = customMessage,
        emoji = emoji,
        colorIndex = colorIndex
    )
    val display = MilestoneCalculator.display(preview, LocalDate.now())

    Scaffold { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                if (existing == null) "New date" else "Edit date",
                style = MaterialTheme.typography.headlineMedium
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Name (e.g. Us)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Column {
                Text("Start date", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(
                    onClick = {
                        // Android's built-in date picker. Months are 0-based there.
                        DatePickerDialog(
                            context,
                            { _, y, m, d -> startDate = LocalDate.of(y, m + 1, d) },
                            startDate.year, startDate.monthValue - 1, startDate.dayOfMonth
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(MilestoneCalculator.formatDate(startDate))
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Look", style = MaterialTheme.typography.titleMedium)

                // Emoji quick picks
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EMOJI_CHOICES.forEach { e ->
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (emoji == e) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { emoji = e },
                            contentAlignment = Alignment.Center
                        ) { Text(e, fontSize = 22.sp) }
                    }
                }
                OutlinedTextField(
                    value = emoji,
                    onValueChange = { emoji = it.take(8) },
                    label = { Text("Or type any emoji") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Colour circles
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CounterColors.all.forEachIndexed { i, c ->
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(c.brush())
                                .border(
                                    if (colorIndex == i) 3.dp else 0.dp,
                                    MaterialTheme.colorScheme.onSurface,
                                    CircleShape
                                )
                                .clickable { colorIndex = i }
                        )
                    }
                }
            }

            Column {
                Text("How should it count?", style = MaterialTheme.typography.titleMedium)
                OptionRow(
                    selected = mode == CountMode.LIVE,
                    text = "Live: updates every day",
                    subtext = "e.g. \"2 months, 1 day\"",
                    onClick = { mode = CountMode.LIVE }
                )
                OptionRow(
                    selected = mode == CountMode.MILESTONE,
                    text = "Milestones only: updates once per ${unit.singular}",
                    subtext = "e.g. \"2 ${unit.plural}\"",
                    onClick = { mode = CountMode.MILESTONE }
                )
            }

            Column {
                Text("Count in", style = MaterialTheme.typography.titleMedium)
                CountUnit.entries.chunked(2).forEach { rowUnits ->
                    Row(Modifier.fillMaxWidth()) {
                        rowUnits.forEach { u ->
                            Row(
                                Modifier
                                    .weight(1f)
                                    .selectable(
                                        selected = unit == u,
                                        onClick = { unit = u },
                                        role = Role.RadioButton
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = unit == u, onClick = null)
                                Text(u.label, Modifier.padding(start = 8.dp, top = 12.dp, bottom = 12.dp))
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Celebration message", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = customMessage,
                    onValueChange = { customMessage = it },
                    label = { Text("Optional, e.g. Happy {n} {unit}, babe! ❤️") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "{n} = the number, {unit} = month/months, {title} = the name. Leave blank for \"Happy 2 months!\"",
                    style = MaterialTheme.typography.bodySmall
                )
                // Shows what the next milestone's message will look like
                val sample = MilestoneCalculator.upcoming(preview, LocalDate.now(), 1).firstOrNull()
                if (sample != null) {
                    Text(
                        "On ${MilestoneCalculator.formatDate(sample.date)}: 🎉 ${MilestoneCalculator.celebrationText(preview, sample.milestone)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Column {
                Text("Notifications", style = MaterialTheme.typography.titleMedium)
                SwitchRow(
                    text = "Notify me on milestone days",
                    subtext = "Around 9 AM, e.g. \"🎉 Happy 2 months!\"",
                    checked = notify,
                    onChange = { notify = it }
                )
                SwitchRow(
                    text = "Also remind me the day before",
                    subtext = "So you have time to plan something",
                    checked = notifyDayBefore,
                    enabled = notify,
                    onChange = { notifyDayBefore = it }
                )
            }

            // Preview card (in your chosen colour)
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(preview.brush())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Preview", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.8f))
                    Text("${preview.emoji} ${preview.title}", color = Color.White)
                    Text(
                        display.headline,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    display.celebration?.let { Text("🎉 $it", color = Color.White) }
                    Text(display.detail, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                    display.nextMilestone?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.9f))
                    }
                }
            }

            Button(
                onClick = {
                    onSave(
                        Counter(
                            id = existing?.id ?: UUID.randomUUID().toString(),
                            title = title.trim(),
                            startDate = startDate,
                            mode = mode,
                            unit = unit,
                            customMessage = customMessage.trim(),
                            notify = notify,
                            notifyDayBefore = notifyDayBefore && notify,
                            emoji = emoji.trim().ifEmpty { "❤️" },
                            colorIndex = colorIndex
                        )
                    )
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save") }

            TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }

            if (onDelete != null) {
                TextButton(
                    onClick = { confirmDelete = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Delete") }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmDelete && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete \"${existing?.title}\"?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun OptionRow(selected: Boolean, text: String, subtext: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(Modifier.padding(start = 12.dp)) {
            Text(text)
            Text(subtext, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SwitchRow(
    text: String,
    subtext: String,
    checked: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(text)
            Text(subtext, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}