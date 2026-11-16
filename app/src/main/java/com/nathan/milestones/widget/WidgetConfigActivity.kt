package com.nathan.milestones.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nathan.milestones.data.CounterRepository
import com.nathan.milestones.logic.MilestoneCalculator
import com.nathan.milestones.model.Counter
import com.nathan.milestones.ui.EditCounterScreen
import com.nathan.milestones.ui.theme.MilestonesTheme
import java.time.LocalDate

/**
 * Opens when you add a Milestones widget (or long-press it > Settings).
 * Pick which date the widget shows, or create a new one.
 */
class WidgetConfigActivity : ComponentActivity() {

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // If you back out without picking, the widget isn't added
        setResult(RESULT_CANCELED)

        widgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val repo = CounterRepository(this)
        enableEdgeToEdge()
        setContent {
            MilestonesTheme {
                val counters = remember { repo.getAll() }
                var creatingNew by remember { mutableStateOf(counters.isEmpty()) }

                if (creatingNew) {
                    EditCounterScreen(
                        existing = null,
                        onSave = { counter ->
                            repo.save(counter)
                            pick(repo, counter)
                        },
                        onDelete = null,
                        onCancel = { if (counters.isEmpty()) finish() else creatingNew = false }
                    )
                } else {
                    Scaffold { padding ->
                        Column(
                            Modifier
                                .fillMaxSize()
                                .padding(padding)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Which date should this widget show?",
                                style = MaterialTheme.typography.titleLarge
                            )
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(counters, key = { it.id }) { counter ->
                                    val display = MilestoneCalculator.display(counter, LocalDate.now())
                                    Card(
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable { pick(repo, counter) }
                                    ) {
                                        Column(Modifier.padding(16.dp)) {
                                            Text(counter.title, style = MaterialTheme.typography.titleMedium)
                                            Text(display.headline, style = MaterialTheme.typography.bodyLarge)
                                        }
                                    }
                                }
                            }
                            Button(onClick = { creatingNew = true }, modifier = Modifier.fillMaxWidth()) {
                                Text("+ New date")
                            }
                        }
                    }
                }
            }
        }
    }

    /** Save the choice, draw the widget, and tell the home screen we're done. */
    private fun pick(repo: CounterRepository, counter: Counter) {
        repo.setWidgetCounter(widgetId, counter.id)
        WidgetUpdater.updateWidget(this, AppWidgetManager.getInstance(this), widgetId)
        WidgetUpdater.scheduleMidnightUpdate(this)
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        finish()
    }
}