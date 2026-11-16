package com.nathan.milestones.ui

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nathan.milestones.data.MemoryRepository
import com.nathan.milestones.logic.MilestoneCalculator
import com.nathan.milestones.model.Counter
import com.nathan.milestones.model.Memory
import java.io.File
import java.time.LocalDate
import java.util.UUID

/**
 * Add or edit a memory for [counter]. existing = null means "new".
 * [prefillTitle] fills the title for new memories, e.g. "3 months" on a milestone day.
 */
@Composable
fun EditMemoryScreen(
    counter: Counter,
    existing: Memory?,
    prefillTitle: String,
    repo: MemoryRepository,
    onSave: (Memory) -> Unit,
    onDelete: (() -> Unit)?,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val memoryId = remember { existing?.id ?: UUID.randomUUID().toString() }
    var date by remember { mutableStateOf(existing?.date ?: LocalDate.now()) }
    var title by remember { mutableStateOf(existing?.title ?: prefillTitle) }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var photoPath by remember { mutableStateOf(existing?.photoPath) }
    var confirmDelete by remember { mutableStateOf(false) }

    /** Deletes a photo picked in this session that won't be kept (not the saved one). */
    fun discardIfNew(path: String?) {
        if (path != null && path != existing?.photoPath) File(path).delete()
    }

    // Android's photo picker: no storage permission needed
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val newPath = repo.copyPhoto(uri, memoryId)
            if (newPath != null) {
                discardIfNew(photoPath)
                photoPath = newPath
            }
        }
    }

    val cancel = {
        discardIfNew(photoPath)
        onCancel()
    }
    BackHandler(onBack = cancel)

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
                if (existing == null) "New memory" else "Edit memory",
                style = MaterialTheme.typography.headlineMedium
            )
            Text("${counter.emoji} ${counter.title}", style = MaterialTheme.typography.titleMedium)

            Column {
                Text("Date", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(
                    onClick = {
                        DatePickerDialog(
                            context,
                            { _, y, m, d -> date = LocalDate.of(y, m + 1, d) },
                            date.year, date.monthValue - 1, date.dayOfMonth
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(MilestoneCalculator.formatDateWithDay(date)) }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title (e.g. 3 months 🎉)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("What did you do? (optional)") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            // Photo
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Photo", style = MaterialTheme.typography.titleMedium)
                photoPath?.let { path ->
                    MemoryPhoto(
                        path,
                        Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) { Text(if (photoPath == null) "Choose photo" else "Change photo") }
                    if (photoPath != null) {
                        TextButton(onClick = {
                            discardIfNew(photoPath)
                            photoPath = null
                        }) { Text("Remove photo") }
                    }
                }
            }

            Button(
                onClick = {
                    onSave(
                        Memory(
                            id = memoryId,
                            counterId = counter.id,
                            date = date,
                            title = title.trim(),
                            note = note.trim(),
                            photoPath = photoPath
                        )
                    )
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save") }

            TextButton(onClick = cancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }

            if (onDelete != null) {
                TextButton(
                    onClick = { confirmDelete = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Delete memory") }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmDelete && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this memory?") },
            text = { Text("The note and photo will be removed from the app.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    discardIfNew(photoPath)
                    onDelete()
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            }
        )
    }
}