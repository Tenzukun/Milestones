package com.nathan.milestones.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nathan.milestones.logic.MilestoneCalculator
import com.nathan.milestones.model.Counter
import java.time.LocalDate

/** Home screen: one card per date, plus the "+ New date" button. */
@Composable
fun CounterListScreen(
    counters: List<Counter>,
    today: LocalDate,
    onAdd: () -> Unit,
    onOpen: (Counter) -> Unit
) {
    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onAdd) { Text("+ New date") }
        }
    ) { padding ->
        if (counters.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No dates yet.\nTap \"+ New date\" to add one, like the day you started dating.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("Milestones", style = MaterialTheme.typography.headlineMedium)
                }
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
    val celebrating = display.celebration != null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        // The card turns a different colour on milestone days
        colors = if (celebrating)
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        else CardDefaults.cardColors()
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(counter.title, style = MaterialTheme.typography.titleMedium)
            Text(
                display.headline,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            display.celebration?.let {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        "🎉 $it",
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
            Text(display.detail, style = MaterialTheme.typography.bodyMedium)
            display.nextMilestone?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            Text(
                display.modeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}