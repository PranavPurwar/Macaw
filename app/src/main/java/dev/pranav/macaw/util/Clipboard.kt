package dev.pranav.macaw.util

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
        files.forEach { file ->
            val destinationFile = File(destination, file.name)
            if (isCut) {
                file.renameTo(destinationFile)
            } else {
                if (file.isFile) {
                    file.copyTo(destinationFile, overwrite = true)
                } else {
                    file.copyRecursively(destinationFile, overwrite = true)
                }
            }
        }
        if (isCut) {
            clear()
        }
    }

    fun hasFile(): Boolean {
        return files.isNotEmpty()
    }

    fun clear() {
        files = emptyList()
        isCut = false
    }
}
