package com.nathan.milestones.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nathan.milestones.logic.MilestoneCalculator
import com.nathan.milestones.model.Counter
import java.time.LocalDate

/** Home screen: a header with Backup, one card per date, and the "+ New date" button. */
@Composable
fun CounterListScreen(
    counters: List<Counter>,
    today: LocalDate,
    onAdd: () -> Unit,
    onOpen: (Counter) -> Unit,
    onBackup: () -> Unit
) {
    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onAdd) { Text("+ New date") }
        }
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header (always shown, so you can restore on a brand new phone)
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Milestones", style = MaterialTheme.typography.headlineMedium)
                    TextButton(onClick = onBackup) { Text("Backup") }
                }
            }

            if (counters.isEmpty()) {
                item {
                    Text(
                        "No dates yet.\nTap \"+ New date\" to add one, like the day you started dating.\n\n" +
                                "New phone? Tap Backup to restore your old dates.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 64.dp)
                    )
                }
            } else {
                items(counters, key = { it.id }) { counter ->
                    CounterCard(counter, today, onClick = { onOpen(counter) })
                }
            }
        }
    }
}

@Composable
private fun CounterCard(counter: Counter, today: LocalDate, onClick: () -> Unit) {
    val display = MilestoneCalculator.display(counter, today)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(counter.brush()) // the date's gradient
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "${counter.emoji} ${counter.title}",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Text(
                display.headline,
                style = MaterialTheme.typography.headlineMedium,
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
            Text(display.detail, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
            display.nextMilestone?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.9f))
            }
            Text(display.modeLabel, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
        }
    }
}