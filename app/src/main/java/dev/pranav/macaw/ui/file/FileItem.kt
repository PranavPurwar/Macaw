package dev.pranav.macaw.ui.file

import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.MoreVert
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import dev.pranav.macaw.util.details
import java.nio.file.Path
import kotlin.io.path.name

@Composable
fun FileItem(
    path: Path,
    isSelected: Boolean,
    selectionMode: Boolean,
    onFileClick: (Path) -> Unit,
    onFileLongClick: (Path) -> Unit,
    onMoreClick: (Path) -> Unit
) {
    val name = remember(path.name) { path.name.ifEmpty { "Unknown" } }
    val details = remember(path.name) { path.details() }

    val painter = rememberAsyncImagePainter(
        path,
        filterQuality = FilterQuality.Low
    )
    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onFileClick(path) },
                onLongClick = { onFileLongClick(path) }
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Image(
            painter = painter,
            contentDescription = path.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(36.dp)
        )

        Column(
            Modifier
                .padding(start = 12.dp)
                .weight(1f)
        ) {
            Text(
                name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2
            )

            Text(
                details,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (selectionMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onFileClick(path) }
            )
        } else {
            IconButton(onClick = { onMoreClick(path) }) {
                Icon(Icons.TwoTone.MoreVert, contentDescription = "More options")
            }
        }
    }
}

