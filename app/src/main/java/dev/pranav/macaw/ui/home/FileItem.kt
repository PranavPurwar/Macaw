package dev.pranav.macaw.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.pranav.macaw.R
import dev.pranav.macaw.model.FileInfo
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileItem(
    fileInfo: FileInfo,
    isSelected: Boolean,
    selectionMode: Boolean,
    onFileClick: (File) -> Unit,
    onFileLongClick: (File) -> Unit,
    onMoreClick: (File) -> Unit
) {
    val file = fileInfo.file
    val name = remember(file.absolutePath) { file.name }

    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onFileClick(file) },
                onLongClick = { onFileLongClick(file) }
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        AsyncImage(
            model = file,
            placeholder = painterResource(if (file.isDirectory) R.drawable.twotone_folder_24 else R.mipmap.unk),
            error = painterResource(if (file.isDirectory) R.drawable.twotone_folder_24 else R.mipmap.unk),
            contentDescription = file.name,
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
                fileInfo.details,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (selectionMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onFileClick(file) }
            )
        } else {
            IconButton(onClick = { onMoreClick(file) }) {
                Icon(Icons.TwoTone.MoreVert, contentDescription = "More options")
            }
        }
    }
}
