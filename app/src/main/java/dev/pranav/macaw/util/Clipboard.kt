package dev.pranav.macaw.util

import dev.pranav.macaw.model.CopyAction
import dev.pranav.macaw.model.MoveAction
import dev.pranav.macaw.service.ActionManager
import java.nio.file.Path

object Clipboard {
    private var files: List<Path> = emptyList()
    private var isCut: Boolean = false

    fun copy(files: List<Path>) {
        this.files = files
        this.isCut = false
    }

    fun cut(files: List<Path>) {
        this.files = files
        this.isCut = true
    }

    fun copy(file: Path) {
        copy(listOf(file))
    }

    fun cut(file: Path) {
        cut(listOf(file))
    }

    fun paste(destination: Path) {
        paste(destination, null)
    }

    fun paste(
        destination: Path,
        onConflict: (suspend (ConflictInfo) -> ConflictResolution)? = null
    ) {
        val action = if (isCut) {
            MoveAction(
                id = System.currentTimeMillis(),
                files = files,
                destination = destination,
                onConflict = onConflict ?: { ConflictResolution.OVERWRITE }
            )
        } else {
            CopyAction(
                id = System.currentTimeMillis(),
                files = files,
                destination = destination,
                onConflict = onConflict ?: { ConflictResolution.OVERWRITE }
            )
        }
        ActionManager.addAction(action)
        if (isCut) clear()
    }

    fun hasFile(): Boolean {
        return files.isNotEmpty()
    }

    fun clear() {
        files = emptyList()
        isCut = false
    }
}
