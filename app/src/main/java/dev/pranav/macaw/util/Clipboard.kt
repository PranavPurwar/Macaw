package dev.pranav.macaw.util

import java.io.File

object Clipboard {
    private var file: File? = null
    private var isCut: Boolean = false

    fun copy(file: File) {
        this.file = file
        this.isCut = false
    }

    fun cut(file: File) {
        this.file = file
        this.isCut = true
    }

    fun paste(destination: File): Boolean {
        file?.let {
            return if (isCut) {
                val result = it.renameTo(File(destination, it.name))
                file = null
                result
            } else {
                if (it.isFile) {
                    it.copyTo(File(destination, it.name), true).exists()
                } else {
                    it.copyRecursively(File(destination, it.name), true)
                }
            }
        }
        return false
    }

    fun hasFile(): Boolean {
        return file != null
    }

    fun clear() {
        file = null
        isCut = false
    }
}
