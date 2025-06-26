package dev.pranav.macaw.util

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.io.path.absolutePathString
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.fileSize
import kotlin.io.path.inputStream
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.outputStream
import kotlin.io.path.relativeTo
import kotlin.io.path.walk


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

fun sortFiles(files: List<FileEntry>, sortOrder: SortOrder): List<FileEntry> {
    return files.sortedWith(getFileInfoComparator(sortOrder))
}

private fun getFileInfoComparator(sortOrder: SortOrder): Comparator<FileEntry> {
    return compareByDescending<FileEntry> { it.isDirectory }.thenComparing { o1, o2 ->
        when (sortOrder) {
            SortOrder.NAME_ASCENDING -> o1.name.compareTo(o2.name, ignoreCase = true)
            SortOrder.NAME_DESCENDING -> o2.name.compareTo(o1.name, ignoreCase = true)
            SortOrder.DATE_ASCENDING -> o1.lastModified.compareTo(o2.lastModified)
            SortOrder.DATE_DESCENDING -> o2.lastModified.compareTo(o1.lastModified)
            SortOrder.SIZE_ASCENDING -> o1.size.compareTo(o2.size)
            SortOrder.SIZE_DESCENDING -> o2.size.compareTo(o1.size)
        }
    }
}

fun Path.orderedChildren(sortOrder: SortOrder = SortOrder.NAME_ASCENDING): List<Path> {
    val children = listDirectoryEntries()
    return children.sortedWith(compareByDescending<Path> { it.isDirectory() }.thenComparing { o1, o2 ->
        when (sortOrder) {
            SortOrder.NAME_ASCENDING -> o1.name.compareTo(o2.name, ignoreCase = true)
            SortOrder.NAME_DESCENDING -> o2.name.compareTo(o1.name, ignoreCase = true)
            SortOrder.DATE_ASCENDING -> o1.getLastModifiedFormattedNative()
                .compareTo(o2.getLastModifiedFormattedNative())

            SortOrder.DATE_DESCENDING -> o2.getLastModifiedFormattedNative()
                .compareTo(o1.getLastModifiedFormattedNative())

            SortOrder.SIZE_ASCENDING -> o1.fileSize().compareTo(o2.fileSize())
            SortOrder.SIZE_DESCENDING -> o2.fileSize().compareTo(o1.fileSize())
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

fun Path.deleteFile(
    onProgress: ((String, Float) -> Unit)? = null,
    shouldContinue: () -> Boolean = { true }
): Boolean {
    return toFile().deleteFile(onProgress, shouldContinue)
}

suspend fun Path.copyRecursivelyWithConflictResolution(
    target: Path,
    onProgress: (fileName: String, progress: Float) -> Unit,
    onConflict: suspend (ConflictInfo) -> ConflictResolution,
    shouldContinue: () -> Boolean = { true }
): Boolean {
    if (!exists() || target.absolutePathString() == absolutePathString()) return false // when some idiot tries to copy a file to its own path itself

    var globalConflictResolution: ConflictResolution? = null

    walk().filter { it.isDirectory() }.forEach {
        if (!shouldContinue()) return false
        val relativePath = it.relativeTo(this)
        val destinationFile = target.resolve(relativePath)

        if (destinationFile.exists() && destinationFile.isRegularFile()) {
            val resolution = globalConflictResolution ?: onConflict(
                ConflictInfo(destinationFile, it.name, it.fileSize(), "copy")
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
                    destinationFile.deleteIfExists()
                }

                ConflictResolution.RENAME -> {
                    return@forEach
                }
            }
        }

        Files.createDirectories(destinationFile)
    }

    val sourceFiles = walk().filter { it.isRegularFile() }.toList()
    val totalSize = sourceFiles.sumOf { it.fileSize() }
    var copiedSize = 0L

    if (totalSize == 0L) {
        onProgress("", 1f)
        return true
    }

    for (sourceFile in sourceFiles) {
        if (!shouldContinue()) return false
        val relativePath = sourceFile.relativeTo(this)
        var destinationFile = target.resolve(relativePath)

        if (destinationFile.exists()) {
            val resolution = globalConflictResolution ?: onConflict(
                ConflictInfo(destinationFile, sourceFile.name, sourceFile.fileSize(), "copy")
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
                    destinationFile.deleteIfExists()
                }

                ConflictResolution.RENAME -> {
                    destinationFile = destinationFile.parent.resolve(
                        sourceFile.duplicateName()
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

fun Path.nameWithoutExtension(): String {
    return if (isDirectory()) name else nameWithoutExtension.ifEmpty { this.name }
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

fun Path.duplicateName(): String {
    val parent = parent ?: return nameWithoutExtension()
    var count = 1
    val newName = nameWithoutExtension()
    while (parent.resolve("$newName ($count)").exists()) {
        count++
    }
    return "$newName ($count)" + if (isRegularFile() && extension.isNotEmpty()) ".${extension}" else ""
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

fun File.orderedChildrenNative(sortOrder: SortOrder = SortOrder.NAME_ASCENDING): List<FileEntry> {
    return try {
        FileJNI.getOrderedChildren(absolutePath, sortOrder.ordinal).toList()
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}

fun Path.orderedChildrenNative(sortOrder: SortOrder = SortOrder.NAME_ASCENDING): List<FileEntry> {
    return try {
        FileJNI.getOrderedChildren(absolutePathString(), sortOrder.ordinal).toList()
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}

fun File.getFileDetailsNative(): String {
    return try {
        FileJNI.getFileDetails(absolutePath)
    } catch (e: Exception) {
        e.printStackTrace()
        "Error fetching details"
    }
}

fun File.getLastModifiedFormattedNative(format: String = "MMM dd, hh:mm a"): String {
    return try {
        FileJNI.getLastModifiedFormatted(absolutePath, format)
    } catch (e: Exception) {
        e.printStackTrace()
        "Error fetching last modified date"
    }
}

fun Path.getLastModifiedFormattedNative(format: String = "MMM dd, hh:mm a"): String {
    return try {
        FileJNI.getLastModifiedFormatted(absolutePathString(), format)
    } catch (e: Exception) {
        e.printStackTrace()
        "Error fetching last modified date"
    }
}
