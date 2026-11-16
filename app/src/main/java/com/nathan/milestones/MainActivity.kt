package com.nathan.milestones

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.nathan.milestones.data.CounterRepository
import com.nathan.milestones.model.Counter
import com.nathan.milestones.ui.CounterDetailScreen
import com.nathan.milestones.ui.CounterListScreen
import com.nathan.milestones.ui.EditCounterScreen
import com.nathan.milestones.ui.theme.MilestonesTheme
import java.time.LocalDate

import com.nathan.milestones.widget.WidgetUpdater

/** Which screen is showing. */
private sealed interface Screen {
    data object Home : Screen
    data class Detail(val counterId: String) : Screen
    /** existing = null means creating a new date */
    data class Edit(val existing: Counter?) : Screen
}

class MainActivity : ComponentActivity() {

    private lateinit var repo: CounterRepository
    private var counters by mutableStateOf(listOf<Counter>())
    private var today by mutableStateOf(LocalDate.now())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = CounterRepository(this)
        enableEdgeToEdge()

        setContent {
            MilestonesTheme {
                var screen by remember { mutableStateOf<Screen>(Screen.Home) }

                when (val s = screen) {
                    is Screen.Home -> CounterListScreen(
                        counters = counters,
                        today = today,
                        onAdd = { screen = Screen.Edit(null) },
                        onOpen = { screen = Screen.Detail(it.id) } // tap a card = details
                    )

                    is Screen.Detail -> {
                        val counter = counters.firstOrNull { it.id == s.counterId }
                        if (counter == null) {
                            // The date was deleted, go back to the list
                            LaunchedEffect(Unit) { screen = Screen.Home }
                        } else {
                            CounterDetailScreen(
                                counter = counter,
                                today = today,
                                onBack = { screen = Screen.Home },
                                onEdit = { screen = Screen.Edit(counter) }
                            )
                        }
                    }

                    is Screen.Edit -> EditCounterScreen(
                        existing = s.existing,
                        onSave = { counter ->
                            repo.save(counter)
                            refresh()
                            screen = Screen.Detail(counter.id) // show the saved date
                        },
                        onDelete = s.existing?.let { existing ->
                            {
                                repo.delete(existing.id)
                                refresh()
                                screen = Screen.Home
                            }
                        },
                        onCancel = {
                            // Cancel goes back to wherever you came from
                            screen = s.existing?.let { Screen.Detail(it.id) } ?: Screen.Home
                        }
                    )
                }
            }
        }
    }

    /** Runs every time you come back to the app, so the count is always for today. */
    override fun onResume() {
        super.onResume()
        refresh()
        WidgetUpdater.scheduleMidnightUpdate(this)
    }

    private fun refresh() {
        counters = repo.getAll()
        today = LocalDate.now()
        WidgetUpdater.updateAll(this) // keep home screen widgets in sync
    }
}clear
git add . ; git commit -m "Add home screen widget" ; git status
git push
