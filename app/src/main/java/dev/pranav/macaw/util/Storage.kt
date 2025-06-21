package dev.pranav.macaw.util

import android.annotation.SuppressLint
import java.io.File
import java.text.SimpleDateFormat

fun File.details(): String {
    if (isFile) {
        return "${getLastModifiedDate()} | ${sizeString()}"
    }
    if (!canRead()) return "Insufficient Permissions"
    val children = listFiles() ?: return "0 files"
    val filesNo = children.count { it.isFile }
    val foldersNo = children.size - filesNo

    val foldersStr = "$foldersNo ${if (foldersNo == 1) "folder" else "folders"}"
    val filesStr = "$filesNo ${if (filesNo == 1) "file" else "files"}"

    return "$foldersStr, $filesStr"
}

@SuppressLint("SimpleDateFormat")
fun File.getLastModifiedDate(dateFormat: String = "MMM dd, hh:mm a"): String {
    return SimpleDateFormat(dateFormat).format(lastModified())
}

fun File.sizeString(): String {
    if (length() <= 0) return "0 B"
    return length().sizeString()
}

fun File.orderedChildren(): List<File> {
    val children = listFiles() ?: return emptyList()
    return children.sortedWith(compareByDescending<File> { it.isDirectory }.thenBy { it.name.lowercase() })
}

fun File.deleteFile(): Boolean {
    return if (isDirectory) {
        deleteRecursively()
    } else {
        delete()
    }
}

fun File.rename(newName: String): Boolean {
    val newFile = File(parent, newName)
    return renameTo(newFile)
}

fun File.cloneFile(): File {
    val parent = this.parentFile ?: return this
    val newFile = if (isFile) {
        val nameWithoutExt = nameWithoutExtension
        val extension = extension
        var i = 1
        var newName = "$nameWithoutExt (copy).$extension"
        var newFile = File(parent, newName)
        while (newFile.exists()) {
            newName = "$nameWithoutExt (copy ${i++}).$extension"
            newFile = File(parent, newName)
        }
        newFile
    } else {
        var i = 1
        var newName = "$name (copy)"
        var newFile = File(parent, newName)
        while (newFile.exists()) {
            newName = "$name (copy ${i++})"
            newFile = File(parent, newName)
        }
        newFile
    }
    this.copyRecursively(newFile)
    return newFile
}

fun File.compress(destination: File) {
    java.util.zip.ZipOutputStream(java.io.BufferedOutputStream(java.io.FileOutputStream(destination)))
        .use { out ->
            if (this.isDirectory) {
                this.walkTopDown().forEach { file ->
                    val zipFileName =
                        file.absolutePath.removePrefix(this.absolutePath).removePrefix("/")
                    if (zipFileName.isNotEmpty()) {
                        val entry =
                            java.util.zip.ZipEntry(zipFileName + if (file.isDirectory) "/" else "")
                        out.putNextEntry(entry)
                        if (file.isFile) {
                            java.io.FileInputStream(file).use { fi ->
                                java.io.BufferedInputStream(fi).use { origin ->
                                    origin.copyTo(out, 1024)
                                }
                            }
                        }
                    }
                }
            } else {
                val entry = java.util.zip.ZipEntry(this.name)
                out.putNextEntry(entry)
                java.io.FileInputStream(this).use { fi ->
                    java.io.BufferedInputStream(fi).use { origin ->
                        origin.copyTo(out, 1024)
                    }
                }
            }
        }
}
