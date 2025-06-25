package dev.pranav.macaw.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Compress
import androidx.compose.material.icons.twotone.ContentCopy
import androidx.compose.material.icons.twotone.ContentCut
import androidx.compose.material.icons.twotone.Delete
import androidx.compose.material.icons.twotone.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.pranav.macaw.model.FileAction

@Composable
fun MultiFileActionBottomSheet(
    onAction: (FileAction) -> Unit,
) {
    val actions = listOf(
        FileAction.CUT to "Cut",
        FileAction.COPY to "Copy",
        FileAction.DELETE to "Delete",
        FileAction.COMPRESS to "Compress",
    )

    LazyColumn {
        items(actions) { (action, label) ->
            val icon = when (action) {
                FileAction.CUT -> Icons.TwoTone.ContentCut
                FileAction.COPY -> Icons.TwoTone.ContentCopy
                FileAction.DELETE -> Icons.TwoTone.Delete
                FileAction.COMPRESS -> Icons.TwoTone.Compress
                else -> Icons.TwoTone.Info
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAction(action) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )

                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp)
                )
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }
    }
}

