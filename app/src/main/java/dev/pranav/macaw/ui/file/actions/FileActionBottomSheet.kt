package dev.pranav.macaw.ui.file.actions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.pranav.macaw.model.FileAction
import dev.pranav.macaw.model.FileType
import dev.pranav.macaw.model.getFileType
import dev.pranav.macaw.util.Clipboard
import java.nio.file.Path
import kotlin.io.path.isRegularFile

@Composable
fun FileActionBottomSheet(
    path: Path,
    onAction: (FileAction) -> Unit,
    isBookmarked: Boolean
) {
    val actions = listOfNotNull(
        FileAction.CUT to "Cut",
        FileAction.COPY to "Copy",
        FileAction.RENAME to "Rename",
        if (path.getFileType() == FileType.ARCHIVE) FileAction.EXTRACT to "Extract" else null,
        if (Clipboard.hasFile()) FileAction.PASTE to "Paste" else null,
        FileAction.DELETE to "Delete",
        FileAction.DETAILS to "Details",
        if (path.isRegularFile()) FileAction.OPEN_WITH to "Open with" else null,
        if (path.isRegularFile()) FileAction.SHARE to "Share" else null, // dont share folders
        FileAction.COMPRESS to "Compress",
        if (path.isRegularFile()) FileAction.OPEN_TEXT_EDITOR to "Edit" else null,
        FileAction.CLONE to "Clone",
        if (isBookmarked) FileAction.UNBOOKMARK to "Remove" else FileAction.BOOKMARK to "Bookmark",
        if (Clipboard.hasFile()) FileAction.CLEAR_CLIPBOARD to "Clear" else null
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            contentPadding = PaddingValues(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
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

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAction(action) },
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            color = when (action) {
                                FileAction.DELETE -> MaterialTheme.colorScheme.errorContainer
                                FileAction.EXTRACT -> MaterialTheme.colorScheme.tertiaryContainer
                                FileAction.COMPRESS -> MaterialTheme.colorScheme.secondaryContainer
                                else -> MaterialTheme.colorScheme.primaryContainer
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                icon,
                                contentDescription = label,
                                modifier = Modifier.padding(8.dp),
                                tint = when (action) {
                                    FileAction.DELETE -> MaterialTheme.colorScheme.onErrorContainer
                                    FileAction.EXTRACT -> MaterialTheme.colorScheme.onTertiaryContainer
                                    FileAction.COMPRESS -> MaterialTheme.colorScheme.onSecondaryContainer
                                    else -> MaterialTheme.colorScheme.onPrimaryContainer
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
