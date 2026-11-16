package com.nathan.milestones.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nathan.milestones.update.UpdateInfo

/** "🎉 Milestones v1.2 is available" with Download / Later. */
@Composable
fun UpdateDialog(
    info: UpdateInfo,
    currentVersion: String,
    onDownload: () -> Unit,
    onLater: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onLater,
        title = { Text("🎉 Update available") },
        text = {
            Column(
                Modifier
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Milestones v${info.version} is out. You have v$currentVersion.")
                Text(
                    "Tap Download, then open the file to install. Your dates and memories are kept.",
                    style = MaterialTheme.typography.bodySmall
                )
                if (info.notes.isNotBlank()) {
                    Text("What's new", style = MaterialTheme.typography.titleSmall)
                    Text(info.notes.take(1000), style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDownload) { Text("Download") } },
        dismissButton = { TextButton(onClick = onLater) { Text("Later") } }
    )
}