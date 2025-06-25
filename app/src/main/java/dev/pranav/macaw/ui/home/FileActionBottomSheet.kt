package dev.pranav.macaw.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.OpenInNew
import androidx.compose.material.icons.twotone.Bookmark
import androidx.compose.material.icons.twotone.Clear
import androidx.compose.material.icons.twotone.Compress
import androidx.compose.material.icons.twotone.ContentCopy
import androidx.compose.material.icons.twotone.ContentCut
import androidx.compose.material.icons.twotone.ContentPaste
import androidx.compose.material.icons.twotone.Delete
import androidx.compose.material.icons.twotone.DriveFileRenameOutline
import androidx.compose.material.icons.twotone.Edit
import androidx.compose.material.icons.twotone.FileCopy
import androidx.compose.material.icons.twotone.Info
import androidx.compose.material.icons.twotone.Share
import androidx.compose.material.icons.twotone.Unarchive
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.pranav.macaw.model.FileAction
import dev.pranav.macaw.model.FileType
import dev.pranav.macaw.model.getFileType
import dev.pranav.macaw.util.Clipboard
import java.io.File

@Composable
fun FileActionBottomSheet(
    file: File,
    onAction: (FileAction) -> Unit,
    isBookmarked: Boolean
) {
    val actions = listOfNotNull(
        if (file.getFileType() == FileType.ARCHIVE) FileAction.EXTRACT to "Extract" else null, // on top because it is the most common action for archives
        if (file.isFile) FileAction.OPEN_WITH to "Open with" else null, // opening directory makes no sense
        FileAction.CUT to "Cut",
        FileAction.COPY to "Copy",
        if (Clipboard.hasFile()) FileAction.PASTE to "Paste" else null,
        FileAction.DELETE to "Delete",
        FileAction.RENAME to "Rename",
        FileAction.DETAILS to "Details",
        FileAction.COMPRESS to "Compress",
        FileAction.SHARE to "Share",
        if (file.isFile) FileAction.OPEN_TEXT_EDITOR to "Edit with code editor" else null, // only show for files, not directories
        FileAction.CLONE to "Clone",
        if (isBookmarked) FileAction.UNBOOKMARK to "Unbookmark" else FileAction.BOOKMARK to "Bookmark",
        if (Clipboard.hasFile()) FileAction.CLEAR_CLIPBOARD to "Clear clipboard" else null
    )

    LazyColumn {
        items(actions) { (action, label) ->
            val icon = when (action) {
                FileAction.SHARE -> Icons.TwoTone.Share
                FileAction.OPEN_WITH -> Icons.AutoMirrored.TwoTone.OpenInNew
                FileAction.CLONE -> Icons.TwoTone.FileCopy
                FileAction.RENAME -> Icons.TwoTone.DriveFileRenameOutline
                FileAction.DELETE -> Icons.TwoTone.Delete
                FileAction.OPEN_TEXT_EDITOR -> Icons.TwoTone.Edit
                FileAction.COMPRESS -> Icons.TwoTone.Compress
                FileAction.EXTRACT -> Icons.TwoTone.Unarchive
                FileAction.DETAILS -> Icons.TwoTone.Info
                FileAction.CUT -> Icons.TwoTone.ContentCut
                FileAction.COPY -> Icons.TwoTone.ContentCopy
                FileAction.PASTE -> Icons.TwoTone.ContentPaste
                FileAction.CLEAR_CLIPBOARD -> Icons.TwoTone.Clear
                FileAction.BOOKMARK -> Icons.TwoTone.Bookmark
                FileAction.UNBOOKMARK -> Icons.TwoTone.Bookmark
                else -> Icons.TwoTone.Info
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAction(action) }
                    .padding(horizontal = 20.dp, vertical = 12.dp),
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

