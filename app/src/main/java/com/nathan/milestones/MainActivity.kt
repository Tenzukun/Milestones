package com.nathan.milestones

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.nathan.milestones.data.CounterRepository
import com.nathan.milestones.data.MemoryRepository
import com.nathan.milestones.logic.MilestoneCalculator
import com.nathan.milestones.model.Counter
import com.nathan.milestones.model.Memory
import com.nathan.milestones.notify.MilestoneNotifier
import com.nathan.milestones.ui.CounterDetailScreen
import com.nathan.milestones.ui.CounterListScreen
import com.nathan.milestones.ui.EditCounterScreen
import com.nathan.milestones.ui.EditMemoryScreen
import com.nathan.milestones.ui.theme.MilestonesTheme
import com.nathan.milestones.widget.WidgetUpdater
import java.time.LocalDate

/** Which screen is showing. */
private sealed interface Screen {
    data object Home : Screen
    data class Detail(val counterId: String) : Screen
    /** existing = null means creating a new date */
    data class Edit(val existing: Counter?) : Screen
    /** existing = null means creating a new memory for counterId */
    data class EditMemory(val counterId: String, val existing: Memory?) : Screen
}

class MainActivity : ComponentActivity() {

    private lateinit var repo: CounterRepository
    private lateinit var memoryRepo: MemoryRepository
    private var counters by mutableStateOf(listOf<Counter>())
    private var memories by mutableStateOf(listOf<Memory>())
    private var today by mutableStateOf(LocalDate.now())

    // Android 13+ shows an "Allow notifications?" popup. Nothing extra to do with the answer.
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = CounterRepository(this)
        memoryRepo = MemoryRepository(this)
        MilestoneNotifier.createChannel(this)
        enableEdgeToEdge()

        // Ask for notification permission if any date has notifications turned on
        if (repo.getAll().any { it.notify }) askForNotificationPermission()

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
                                memories = memories
                                    .filter { it.counterId == counter.id }
                                    .sortedByDescending { it.date },
                                onBack = { screen = Screen.Home },
                                onEdit = { screen = Screen.Edit(counter) },
                                onAddMemory = { screen = Screen.EditMemory(counter.id, null) },
                                onOpenMemory = { screen = Screen.EditMemory(counter.id, it) }
                            )
                        }
                    }

                    is Screen.Edit -> EditCounterScreen(
                        existing = s.existing,
                        onSave = { counter ->
                            repo.save(counter)
                            refresh()
                            if (counter.notify) askForNotificationPermission()
                            screen = Screen.Detail(counter.id) // show the saved date
                        },
                        onDelete = s.existing?.let { existing ->
                            {
                                repo.delete(existing.id)
                                memoryRepo.deleteAllFor(existing.id) // its memories and photos too
                                refresh()
                                screen = Screen.Home
                            }
                        },
                        onCancel = {
                            // Cancel goes back to wherever you came from
                            screen = s.existing?.let { Screen.Detail(it.id) } ?: Screen.Home
                        }
                    )

                    is Screen.EditMemory -> {
                        val counter = counters.firstOrNull { it.id == s.counterId }
                        if (counter == null) {
                            LaunchedEffect(Unit) { screen = Screen.Home }
                        } else {
                            // On a milestone day, a new memory starts with e.g. "3 months 🎉"
                            val todayMilestone = MilestoneCalculator.milestoneOn(counter.startDate, today, counter.unit)
                            EditMemoryScreen(
                                counter = counter,
                                existing = s.existing,
                                prefillTitle = todayMilestone?.let { "${it.label} 🎉" } ?: "",
                                repo = memoryRepo,
                                onSave = { memory ->
                                    memoryRepo.save(memory)
                                    refresh()
                                    screen = Screen.Detail(counter.id)
                                },
                                onDelete = s.existing?.let { existing ->
                                    {
                                        memoryRepo.delete(existing)
                                        refresh()
                                        screen = Screen.Detail(counter.id)
                                    }
                                },
                                onCancel = { screen = Screen.Detail(counter.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    /** Runs every time you come back to the app, so the count is always for today. */
    override fun onResume() {
        super.onResume()
        refresh()
        WidgetUpdater.scheduleMidnightUpdate(this)
        MilestoneNotifier.scheduleDailyCheck(this)
    }

    /** Reloads dates and memories from storage and keeps home screen widgets in sync. */
    private fun refresh() {
        counters = repo.getAll()
        memories = memoryRepo.getAll()
        today = LocalDate.now()
        WidgetUpdater.updateAll(this)
    }

    private fun askForNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !MilestoneNotifier.hasPermission(this)
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}