package com.nathan.milestones.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nathan.milestones.logic.MilestoneCalculator
import com.nathan.milestones.logic.UpcomingMilestone
import com.nathan.milestones.model.Counter
import com.nathan.milestones.model.Memory
import java.time.LocalDate

/** Opens when you tap a card: the big count, the next 5 milestones, and your memories. */
@Composable
fun CounterDetailScreen(
    counter: Counter,
    today: LocalDate,
    memories: List<Memory>,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onAddMemory: () -> Unit,
    onOpenMemory: (Memory) -> Unit
) {
    BackHandler(onBack = onBack)
    val display = MilestoneCalculator.display(counter, today)
    val upcoming = MilestoneCalculator.upcoming(counter, today, 5)

    Scaffold { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top bar: back + edit
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onBack) { Text("← Back") }
                    OutlinedButton(onClick = onEdit) { Text("Edit") }
                }
            }

            // Big summary card (in the date's colour)
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(counter.brush())
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "${counter.emoji} ${counter.title}",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        Text(
                            display.headline,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        display.celebration?.let {
                            Surface(
                                color = Color.White.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    "🎉 $it",
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Text(display.detail, style = MaterialTheme.typography.bodyLarge, color = Color.White)
                        Text(
                            "Started ${MilestoneCalculator.formatDateWithDay(counter.startDate)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // ----- Upcoming -----
            item {
                Text(
                    "Upcoming",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            if (upcoming.isEmpty()) {
                item { Text("No milestones in the next few years.") }
            } else {
                items(upcoming, key = { "up-" + it.date.toString() }) { item ->
                    UpcomingRow(counter, item)
                }
            }

            // ----- Memories -----
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Memories", style = MaterialTheme.typography.titleLarge)
                    OutlinedButton(onClick = onAddMemory) { Text("+ Add") }
                }
            }
            if (memories.isEmpty()) {
                item {
                    Text(
                        "No memories yet. Add what you did for a milestone, with a photo from the day.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                items(memories, key = { "mem-" + it.id }) { memory ->
                    MemoryCard(memory, onClick = { onOpenMemory(memory) })
                }
            }
        }
    }
}

/** One upcoming milestone: "3 months / Sun, Dec 13, 2026 / 🎉 message / In 28 days" */
@Composable
private fun UpcomingRow(counter: Counter, item: UpcomingMilestone) {
    val whenText = if (item.daysUntil == 1L) "Tomorrow" else "In ${item.daysUntil} days"
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    item.milestone.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    MilestoneCalculator.formatDateWithDay(item.date),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "🎉 ${MilestoneCalculator.celebrationText(counter, item.milestone)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                whenText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

/** One memory: photo on top (if any), then title, date and the start of the note. */
@Composable
private fun MemoryCard(memory: Memory, onClick: () -> Unit) {
    Card(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        memory.photoPath?.let { path ->
            MemoryPhoto(
                path,
                Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(memory.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                MilestoneCalculator.formatDateWithDay(memory.date),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
            if (memory.note.isNotBlank()) {
                Text(
                    memory.note,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}