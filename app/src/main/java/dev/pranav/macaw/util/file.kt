package dev.pranav.macaw.util

import dev.pranav.macaw.model.FileInfo
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale


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

fun File.getLastModifiedDate(dateFormat: String = "MMM dd, hh:mm a"): String {
    return SimpleDateFormat(dateFormat, Locale.getDefault()).format(lastModified())
}

fun File.sizeString(): String {
    if (length() <= 0) return "0 B"
    return length().sizeString()
}

enum class SortOrder {
    NAME_ASCENDING,
    NAME_DESCENDING,
    DATE_ASCENDING,
    DATE_DESCENDING,
    SIZE_ASCENDING,
    SIZE_DESCENDING,
}

fun sortFiles(files: List<FileInfo>, sortOrder: SortOrder): List<FileInfo> {
    return files.sortedWith(getFileInfoComparator(sortOrder))
}

private fun getFileInfoComparator(sortOrder: SortOrder): Comparator<FileInfo> {
    return compareByDescending<FileInfo> { it.file.isDirectory }.thenComparing { o1, o2 ->
        when (sortOrder) {
            SortOrder.NAME_ASCENDING -> o1.file.name.compareTo(o2.file.name, ignoreCase = true)
            SortOrder.NAME_DESCENDING -> o2.file.name.compareTo(o1.file.name, ignoreCase = true)
            SortOrder.DATE_ASCENDING -> o1.file.lastModified().compareTo(o2.file.lastModified())
            SortOrder.DATE_DESCENDING -> o2.file.lastModified().compareTo(o1.file.lastModified())
            SortOrder.SIZE_ASCENDING -> o1.file.length().compareTo(o2.file.length())
            SortOrder.SIZE_DESCENDING -> o2.file.length().compareTo(o1.file.length())
        }
    }
}

fun File.orderedChildren(sortOrder: SortOrder = SortOrder.NAME_ASCENDING): List<File> {
    val children = listFiles() ?: return emptyList()
    return children.sortedWith(compareByDescending<File> { it.isDirectory }.thenComparing { o1, o2 ->
        when (sortOrder) {
            SortOrder.NAME_ASCENDING -> o1.name.compareTo(o2.name, ignoreCase = true)
            SortOrder.NAME_DESCENDING -> o2.name.compareTo(o1.name, ignoreCase = true)
            SortOrder.DATE_ASCENDING -> o1.lastModified().compareTo(o2.lastModified())
            SortOrder.DATE_DESCENDING -> o2.lastModified().compareTo(o1.lastModified())
            SortOrder.SIZE_ASCENDING -> o1.length().compareTo(o2.length())
            SortOrder.SIZE_DESCENDING -> o2.length().compareTo(o1.length())
        }
    })
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


fun File.nameWithoutExtension(): String {
    return this.nameWithoutExtension.ifEmpty { this.name }
}

fun File.duplicateName(): String {
    val parent = parentFile ?: return nameWithoutExtension()
    var count = 1
    val newName = nameWithoutExtension()
    while (File(parent, "$newName ($count)").exists()) {
        count++
    }
    return "$newName ($count)" + if (extension.isNotEmpty()) ".${extension}" else ""
}

fun File.clone(): File {
    val newFileName = duplicateName()
    return File(this.parentFile, newFileName).let { newFile ->
        // if someone makes a file with the *just calculated* name this very moment, its your fault atp
        if (isFile) {
            copyTo(newFile, overwrite = true)
        } else {
            copyRecursively(newFile, overwrite = true)
        }
        newFile
    }
}
