package dev.pranav.macaw.util

import dev.pranav.macaw.model.CopyAction
import dev.pranav.macaw.model.MoveAction
import dev.pranav.macaw.service.ActionManager
import java.io.File

object Clipboard {
    private var files: List<File> = emptyList()
    private var isCut: Boolean = false

    fun copy(files: List<File>) {
        this.files = files
        this.isCut = false
    }

    fun cut(files: List<File>) {
        this.files = files
        this.isCut = true
    }

    fun copy(file: File) {
        copy(listOf(file))
    }

    fun cut(file: File) {
        cut(listOf(file))
    }

    fun paste(destination: File) {
        paste(destination, null)
    }

    fun paste(
        destination: File,
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
