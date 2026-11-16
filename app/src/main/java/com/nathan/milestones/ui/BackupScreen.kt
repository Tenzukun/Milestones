package com.nathan.milestones.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nathan.milestones.data.BackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/** Save everything to one .zip file, or restore from one. */
@Composable
fun BackupScreen(onBack: () -> Unit, onRestored: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<String?>(null) }
    var working by remember { mutableStateOf(false) }
    var pendingRestore by remember { mutableStateOf<Uri?>(null) }

    // "Save as" dialog: you pick where the .zip goes (phone storage, Google Drive...)
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            working = true
            status = "Saving backup..."
            scope.launch {
                status = try {
                    // Runs in the background so the app doesn't freeze with lots of photos
                    val (dates, memories) = withContext(Dispatchers.IO) { BackupManager.export(context, uri) }
                    "✅ Backup saved: $dates dates, $memories memories."
                } catch (e: Exception) {
                    "❌ Backup failed: ${e.message}"
                }
                working = false
            }
        }
    }

    // "Open" dialog: you pick a backup file to restore
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) pendingRestore = uri // ask "are you sure?" first
    }

    BackHandler(enabled = !working, onBack = onBack)

    Scaffold { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TextButton(onClick = onBack, enabled = !working) { Text("← Back") }
            Text("Backup & restore", style = MaterialTheme.typography.headlineMedium)
            Text(
                "A backup is one .zip file with all your dates, memories and photos. " +
                        "Save it to Google Drive or send it to yourself, so nothing is lost if you change phones.",
                style = MaterialTheme.typography.bodyMedium
            )

            Button(
                onClick = { exportLauncher.launch("Milestones-backup-${LocalDate.now()}.zip") },
                enabled = !working,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save a backup") }

            OutlinedButton(
                onClick = {
                    restoreLauncher.launch(
                        arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")
                    )
                },
                enabled = !working,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Restore from a backup") }

            Text(
                "Restoring replaces everything currently in the app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )

            status?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
        }
    }

    pendingRestore?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text("Replace everything?") },
            text = { Text("Your current dates, memories and photos will be replaced by the ones in this backup.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingRestore = null
                    working = true
                    status = "Restoring..."
                    scope.launch {
                        status = try {
                            val (dates, memories) = withContext(Dispatchers.IO) { BackupManager.restore(context, uri) }
                            onRestored()
                            "✅ Restored $dates dates and $memories memories."
                        } catch (e: Exception) {
                            "❌ Restore failed: ${e.message}"
                        }
                        working = false
                    }
                }) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestore = null }) { Text("Cancel") }
            }
        )
    }
}