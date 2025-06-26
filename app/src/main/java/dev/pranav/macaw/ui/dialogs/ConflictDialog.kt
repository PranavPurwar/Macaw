package dev.pranav.macaw.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import dev.pranav.macaw.util.ConflictInfo
import dev.pranav.macaw.util.ConflictResolution
import dev.pranav.macaw.util.details
import dev.pranav.macaw.util.sizeString
import kotlin.io.path.fileSize

@Composable
fun ConflictDialog(
    conflictInfo: ConflictInfo,
    onResolution: (ConflictResolution, Boolean) -> Unit
) {
    var applyToAll by remember { mutableStateOf(false) }

    AlertDialog(
        modifier = Modifier.fillMaxWidth(0.9f),
        onDismissRequest = { /* Cannot be dismissed */ },
        title = {
            Text(
                text = "File Conflict",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "A file with the same name already exists:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = conflictInfo.newFileName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Existing file:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = conflictInfo.existingFile.fileSize().sizeString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = conflictInfo.existingFile.toFile().details(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "New file:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = conflictInfo.newFileSize.sizeString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${conflictInfo.operation} operation",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = applyToAll,
                        onCheckedChange = { applyToAll = it }
                    )
                    Text(
                        text = "Apply to all conflicts",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = {
                        onResolution(ConflictResolution.SKIP, applyToAll)
                    }
                ) {
                    Text("Skip")
                }

                TextButton(
                    onClick = {
                        onResolution(ConflictResolution.RENAME, applyToAll)
                    }
                ) {
                    Text("Rename")
                }

                TextButton(
                    onClick = {
                        onResolution(ConflictResolution.OVERWRITE, applyToAll)
                    }
                ) {
                    Text("Replace")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onResolution(ConflictResolution.ABORT, false)
                }
            ) {
                Text("Cancel")
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    )
}
