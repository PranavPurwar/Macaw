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

private const val DEFAULT_BUFFER_SIZE = 8 * 1024

fun File.deleteFile(
    onProgress: ((String, Float) -> Unit)? = null,
    shouldContinue: () -> Boolean = { true }
): Boolean {
    if (!exists()) {
        onProgress?.invoke(name, 1.0f)
        return true
    }
    if (isDirectory) {
        val files = walkBottomUp().toList()
        val total = files.size
        if (total == 0) {
            onProgress?.invoke(name, 1.0f)
            return delete()
        }
        for ((index, file) in files.withIndex()) {
            if (!shouldContinue()) return false
            if (!file.delete()) {
                return false
            }
            onProgress?.invoke(file.name, (index + 1).toFloat() / total)
        }
        return true
    } else {
        if (!shouldContinue()) return false
        val result = delete()
        if (result) {
            onProgress?.invoke(name, 1f)
        }
        return result
    }
}

suspend fun File.copyRecursivelyWithConflictResolution(
    target: File,
    onProgress: (fileName: String, progress: Float) -> Unit,
    onConflict: suspend (ConflictInfo) -> ConflictResolution,
    shouldContinue: () -> Boolean = { true }
): Boolean {
    if (!exists() || target.absolutePath == absolutePath) return false // when some idiot tries to copy a file to its own path itself

    var globalConflictResolution: ConflictResolution? = null

    walkTopDown().filter { it.isDirectory }.forEach {
        if (!shouldContinue()) return false
        val relativePath = it.relativeTo(this)
        val destinationFile = File(target, relativePath.path)

        if (destinationFile.exists() && destinationFile.isFile) {
            val resolution = globalConflictResolution ?: onConflict(
                ConflictInfo(destinationFile, it.name, it.length(), "copy")
            )

            when (resolution) {
                ConflictResolution.ABORT -> return false
                ConflictResolution.SKIP, ConflictResolution.SKIP_ALL -> {
                    if (resolution == ConflictResolution.SKIP_ALL) {
                        globalConflictResolution = ConflictResolution.SKIP_ALL
                    }
                    return@forEach
                }

                ConflictResolution.OVERWRITE, ConflictResolution.OVERWRITE_ALL -> {
                    if (resolution == ConflictResolution.OVERWRITE_ALL) {
                        globalConflictResolution = ConflictResolution.OVERWRITE_ALL
                    }
                    destinationFile.delete()
                }

                ConflictResolution.RENAME -> {
                    return@forEach
                }
            }
        }

        destinationFile.mkdirs()
    }

    val sourceFiles = walkTopDown().filter { it.isFile }.toList()
    val totalSize = sourceFiles.sumOf { it.length() }
    var copiedSize = 0L

    if (totalSize == 0L) {
        onProgress("", 1f)
        return true
    }

    for (sourceFile in sourceFiles) {
        if (!shouldContinue()) return false
        val relativePath = sourceFile.relativeTo(this)
        var destinationFile = File(target, relativePath.path)

        if (destinationFile.exists()) {
            val resolution = globalConflictResolution ?: onConflict(
                ConflictInfo(destinationFile, sourceFile.name, sourceFile.length(), "copy")
            )

            when (resolution) {
                ConflictResolution.ABORT -> return false
                ConflictResolution.SKIP, ConflictResolution.SKIP_ALL -> {
                    if (resolution == ConflictResolution.SKIP_ALL) {
                        globalConflictResolution = ConflictResolution.SKIP_ALL
                    }
                    continue
                }

                ConflictResolution.OVERWRITE, ConflictResolution.OVERWRITE_ALL -> {
                    if (resolution == ConflictResolution.OVERWRITE_ALL) {
                        globalConflictResolution = ConflictResolution.OVERWRITE_ALL
                    }
                    destinationFile.delete()
                }

                ConflictResolution.RENAME -> {
                    destinationFile = File(
                        destinationFile.parent,
                        destinationFile.duplicateName()
                    )
                }
            }
        }

        try {
            sourceFile.inputStream().use { input ->
                destinationFile.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var length: Int
                    while (input.read(buffer).also { length = it } > 0) {
                        if (!shouldContinue()) return false
                        output.write(buffer, 0, length)
                        copiedSize += length
                        onProgress(
                            sourceFile.name,
                            copiedSize.toFloat() / totalSize.toFloat()
                        )
                    }
                }
            }
        } catch (_: Exception) {
            return false
        }
    }
    return true
}

fun File.nameWithoutExtension(): String {
    return if (isDirectory) name else nameWithoutExtension.ifEmpty { this.name }
}

fun File.duplicateName(): String {
    val parent = parentFile ?: return nameWithoutExtension()
    var count = 1
    val newName = nameWithoutExtension()
    while (File(parent, "$newName ($count)").exists()) {
        count++
    }
    return "$newName ($count)" + if (isFile && extension.isNotEmpty()) ".${extension}" else ""
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
