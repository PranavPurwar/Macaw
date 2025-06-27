package dev.pranav.macaw.ui.actions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import dev.pranav.macaw.model.Action
import dev.pranav.macaw.service.ActionManager

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ActionProgressDialogInternal(
    action: Action,
    onHide: () -> Unit,
    onCancel: () -> Unit
) {
    val state = action.state.value
    LaunchedEffect(state) {
        if (state is ActionState.Completed || state is ActionState.Failed) {
            onHide()
        }
    }

    AlertDialog(
        modifier = Modifier.fillMaxWidth(0.8f),
        onDismissRequest = onHide,
        title = { Text(text = action::class.java.simpleName.replace("Action", "")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (val s = state) {
                    is ActionState.InProgress -> {

                        LinearProgressIndicator(
                            progress = { s.progress },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = s.message,
                            style = MaterialTheme.typography.bodySmall
                        )

                    }

                    is ActionState.Pending -> {
                        Text(text = "Pending...")
                    }

                    is ActionState.Completed -> {
                        Text(text = s.message)
                    }

                    is ActionState.Failed -> {
                        Text(text = s.reason)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onHide) {
                Text("Hide")
            }
        },
        dismissButton = {
            if (state !is ActionState.Completed && state !is ActionState.Failed) {
                TextButton(onClick = onCancel) {
                    Text("Cancel")
                }
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    )
}

@Composable
fun ActionProgressDialog() {
    val action by ActionManager.activeDialogAction.collectAsState()
    action?.let {
        ActionProgressDialogInternal(
            action = it,
            onHide = { ActionManager.hideDialog() },
            onCancel = { ActionManager.cancelAction(it) }
        )
    }
}
