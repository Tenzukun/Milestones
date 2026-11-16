package com.nathan.milestones.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nathan.milestones.logic.MilestoneCalculator
import com.nathan.milestones.logic.UpcomingMilestone
import com.nathan.milestones.model.CountUnit
import com.nathan.milestones.model.Counter
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Home screen:
 *  - soft gradient background with a greeting and today's date
 *  - a 🎉 banner if any date has a milestone today
 *  - a "Next up" card showing the closest milestone across all your dates
 *  - one card per date, each with a progress bar to its next milestone
 */
@Composable
fun CounterListScreen(
    counters: List<Counter>,
    today: LocalDate,
    onAdd: () -> Unit,
    onOpen: (Counter) -> Unit,
    onBackup: () -> Unit
) {
    // Soft tint at the top fading into the normal background (works in light and dark mode)
    val backgroundBrush = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
            MaterialTheme.colorScheme.background
        )
    )

    // Dates with a milestone today, and the closest upcoming milestone across every date
    val celebratingToday = counters.mapNotNull { c ->
        MilestoneCalculator.celebrationOn(c, today)?.let { c to it }
    }
    val nextUp: Pair<Counter, UpcomingMilestone>? = counters
        .mapNotNull { c -> MilestoneCalculator.upcoming(c, today, 1).firstOrNull()?.let { c to it } }
        .minByOrNull { it.second.daysUntil }

    Box(
        Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        Scaffold(
            containerColor = Color.Transparent, // let the gradient show through
            floatingActionButton = {
                ExtendedFloatingActionButton(onClick = onAdd) { Text("+ New date") }
            }
        ) { padding ->
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ----- Header: greeting + date + Backup -----
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                greeting(),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = onBackup) { Text("Backup") }
                    }
                }

                if (counters.isEmpty()) {
                    item { EmptyState() }
                } else {
                    // ----- 🎉 Today banner -----
                    celebratingToday.forEach { (counter, message) ->
                        item(key = "today-" + counter.id) {
                            TodayBanner(counter, message, onClick = { onOpen(counter) })
                        }
                    }

                    // ----- Next up -----
                    nextUp?.let { (counter, upcoming) ->
                        item(key = "next-up") {
                            NextUpCard(counter, upcoming, onClick = { onOpen(counter) })
                        }
                    }

                    // ----- Your dates -----
                    item {
                        Text(
                            "Your dates",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    items(counters, key = { it.id }) { counter ->
                        CounterCard(counter, today, onClick = { onOpen(counter) })
                    }
                }
            }
        }
    }
}

/** "Good morning ☀️" / "Good afternoon 🌤️" / "Good evening 🌙" */
private fun greeting(): String {
    val hour = LocalTime.now().hour
    return when {
        hour < 12 -> "Good morning ☀️"
        hour < 18 -> "Good afternoon 🌤️"
        else -> "Good evening 🌙"
    }
}

/**
 * How far along you are to the next milestone, from 0.0 to 1.0.
 * Example: started Sep 13, today Nov 28 -> halfway from 2 months (Nov 13) to 3 months (Dec 13) = 0.5
 */
private fun progressToNext(counter: Counter, today: LocalDate): Float {
    val start = counter.startDate
    if (today.isBefore(start)) return 0f
    val (previous, next) = if (counter.unit == CountUnit.DAYS) {
        // Days counters aim for the next 100 days
        val days = ChronoUnit.DAYS.between(start, today)
        val hundreds = days / 100
        start.plusDays(hundreds * 100) to start.plusDays((hundreds + 1) * 100)
    } else {
        val n = MilestoneCalculator.completedUnits(start, today, counter.unit)
        MilestoneCalculator.milestoneDate(start, counter.unit, n) to
                MilestoneCalculator.milestoneDate(start, counter.unit, n + 1)
    }
    val total = ChronoUnit.DAYS.between(previous, next).toFloat()
    if (total <= 0f) return 0f
    return (ChronoUnit.DAYS.between(previous, today) / total).coerceIn(0f, 1f)
}

@Composable
private fun EmptyState() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("💕", fontSize = 64.sp)
        Text(
            "No dates yet",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Tap \"+ New date\" to add one, like the day you started dating.\n\n" +
                    "New phone? Tap Backup to restore your old dates.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Big banner on milestone days: "🎉 Happy 3 months! / ❤️ Us" */
@Composable
private fun TodayBanner(counter: Counter, message: String, onClick: () -> Unit) {
    Card(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(counter.brush())
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🎉", fontSize = 40.sp)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    "Today",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.85f)
                )
                Text(
                    message,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "${counter.emoji} ${counter.title}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

/** "Next up: 3 months / ❤️ Us / Sun, Dec 13 / 28 days" */
@Composable
private fun NextUpCard(counter: Counter, upcoming: UpcomingMilestone, onClick: () -> Unit) {
    Card(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "NEXT UP",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    upcoming.milestone.label,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${counter.emoji} ${counter.title} · ${MilestoneCalculator.formatDateWithDay(upcoming.date)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // Countdown bubble in the date's colour
            Box(
                Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(counter.brush()),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${upcoming.daysUntil}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        if (upcoming.daysUntil == 1L) "day" else "days",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/** One date: emoji bubble, name, big count, and a progress bar to the next milestone. */
@Composable
private fun CounterCard(counter: Counter, today: LocalDate, onClick: () -> Unit) {
    val display = MilestoneCalculator.display(counter, today)
    // Animates the bar filling up when the screen opens
    val progress by animateFloatAsState(
        targetValue = progressToNext(counter, today),
        animationSpec = tween(durationMillis = 900),
        label = "progress"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(counter.brush()) // the date's gradient
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Emoji in a soft white bubble
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) { Text(counter.emoji, fontSize = 26.sp) }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        counter.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.9f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        display.headline,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

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

            // Progress bar to the next milestone (only once the date has started)
            if (!today.isBefore(counter.startDate)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.3f))
                    ) {
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progress)
                                .clip(RoundedCornerShape(50))
                                .background(Color.White)
                        )
                    }
                    Row(Modifier.fillMaxWidth()) {
                        Text(
                            display.nextShort?.let { "Next: $it" } ?: display.detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            } else {
                Text(display.detail, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.9f))
            }
        }
    }
}