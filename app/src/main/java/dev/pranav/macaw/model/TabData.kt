package dev.pranav.macaw.model

import android.os.Environment
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import dev.pranav.macaw.util.SortOrder
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.absolutePathString
import kotlin.io.path.name

data class TabData(
    val initialRootDir: Path,
    var currentPath: Path = initialRootDir,
    val id: String = UUID.randomUUID().toString(),
    val files: SnapshotStateList<Path> = mutableStateListOf(),
    var loadedPath: String? = null,
    var isLoading: Boolean = false,
    val lazyListState: LazyListState = LazyListState(),
    var sortOrder: SortOrder = SortOrder.NAME_ASCENDING,
)

fun TabData.name(): String {
    return if (currentPath.absolutePathString() == Environment.getExternalStorageDirectory().absolutePath) {
        "Internal Storage"
    } else {
        currentPath.name.takeIf { it.isNotEmpty() } ?: this.initialRootDir.name
    }
}
