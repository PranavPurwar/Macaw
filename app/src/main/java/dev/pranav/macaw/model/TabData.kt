package dev.pranav.macaw.model

import android.os.Environment
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import dev.pranav.macaw.util.SortOrder
import java.io.File
import java.util.UUID

data class TabData(
    val initialRootDir: File,
    var currentPath: File = initialRootDir,
    val id: String = UUID.randomUUID().toString(),
    val files: SnapshotStateList<FileInfo> = mutableStateListOf(),
    var loadedPath: String? = null,
    var isLoading: Boolean = false,
    val lazyListState: LazyListState = LazyListState(),
    var sortOrder: SortOrder = SortOrder.NAME_ASCENDING,
)

fun TabData.name(): String {
    return if (currentPath.absolutePath == Environment.getExternalStorageDirectory().absolutePath) {
        "Internal Storage"
    } else {
        currentPath.name.takeIf { it.isNotEmpty() } ?: this.initialRootDir.name
    }
}
