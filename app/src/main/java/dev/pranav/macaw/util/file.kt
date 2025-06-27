package dev.pranav.macaw.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.io.path.absolutePathString
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.fileSize
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.isDirectory
import kotlin.io.path.isReadable
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.relativeTo
import kotlin.io.path.walk

fun Path.details(): String {
    if (isRegularFile()) {
        return "${getLastModified()} | ${fileSize().sizeString()}"
    }
    if (!isReadable()) return "Insufficient Permissions"
    val children = listDirectoryEntries()
    val filesNo = children.count { it.isRegularFile() }
    val foldersNo = children.size - filesNo

    val foldersStr = "$foldersNo ${if (foldersNo == 1) "folder" else "folders"}"
    val filesStr = "$filesNo ${if (filesNo == 1) "file" else "files"}"

    return "$foldersStr, $filesStr"
}

enum class SortOrder {
    NAME_ASCENDING,
    NAME_DESCENDING,
    DATE_ASCENDING,
    DATE_DESCENDING,
    SIZE_ASCENDING,
    SIZE_DESCENDING,
}

fun sortFiles(paths: List<Path>, sortOrder: SortOrder): List<Path> {
    return paths.sortedWith(getFileInfoComparator(sortOrder))
}

private fun getFileInfoComparator(sortOrder: SortOrder): Comparator<Path> {
    return compareByDescending<Path> { it.isDirectory() }.thenComparing { o1, o2 ->
        when (sortOrder) {
            SortOrder.NAME_ASCENDING -> o1.name.compareTo(o2.name, ignoreCase = true)
            SortOrder.NAME_DESCENDING -> o2.name.compareTo(o1.name, ignoreCase = true)
            SortOrder.DATE_ASCENDING -> o1.getLastModifiedTime().compareTo(o2.getLastModifiedTime())
            SortOrder.DATE_DESCENDING -> o2.getLastModifiedTime()
                .compareTo(o1.getLastModifiedTime())
            SortOrder.SIZE_ASCENDING -> o1.fileSize().compareTo(o2.fileSize())
            SortOrder.SIZE_DESCENDING -> o2.fileSize().compareTo(o1.fileSize())
        }
    }
}

fun Path.orderedChildren(sortOrder: SortOrder = SortOrder.NAME_ASCENDING): List<Path> {
    val children = listDirectoryEntries()
    return children.sortedWith(compareByDescending<Path> { it.isDirectory() }.thenComparing { o1, o2 ->
        when (sortOrder) {
            SortOrder.NAME_ASCENDING -> o1.name.compareTo(o2.name, ignoreCase = true)
            SortOrder.NAME_DESCENDING -> o2.name.compareTo(o1.name, ignoreCase = true)
            SortOrder.DATE_ASCENDING -> o1.getLastModifiedTime()
                .compareTo(o2.getLastModifiedTime())

            SortOrder.DATE_DESCENDING -> o2.getLastModifiedTime()
                .compareTo(o1.getLastModifiedTime())

            SortOrder.SIZE_ASCENDING -> o1.fileSize().compareTo(o2.fileSize())
            SortOrder.SIZE_DESCENDING -> o2.fileSize().compareTo(o1.fileSize())
        }
    })
}

fun Path.deleteFile(
    onProgress: ((String, Float) -> Unit)? = null,
    shouldContinue: () -> Boolean = { true }
): Boolean {
    if (!exists()) {
        onProgress?.invoke(name, 1f)
        return true
    }
    if (isDirectory()) {
        var deleted = 0f
        val total = walk().count().toFloat()
        Files.walkFileTree(this, object : SimpleFileVisitor<Path>() {
            override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
                if (!shouldContinue()) return FileVisitResult.TERMINATE
                val result = dir.deleteIfExists()
                if (result) {
                    deleted++
                    onProgress?.invoke(dir.name, deleted / total)
                }
                return FileVisitResult.CONTINUE
            }

            override fun visitFile(file: Path, attrs: BasicFileAttributes?): FileVisitResult {
                if (!shouldContinue()) return FileVisitResult.TERMINATE
                val result = file.deleteIfExists()
                if (result) {
                    deleted++
                    onProgress?.invoke(file.name, deleted / total)
                }
                return FileVisitResult.CONTINUE
            }
        })
        return true
    } else {
        if (!shouldContinue()) return false
        val result = deleteIfExists()
        if (result) {
            onProgress?.invoke(name, 1f)
        }
        return result
    }
}

suspend fun Path.copyRecursively(
    target: Path,
    onProgress: (fileName: String, progress: Float) -> Unit,
    onConflict: suspend (ConflictInfo) -> ConflictResolution,
    shouldContinue: () -> Boolean = { true }
): Boolean = withContext(Dispatchers.IO) {
    if (!exists() || absolutePathString() == target.absolutePathString()) {
        return@withContext false // When some idiot tries to copy a file to its own path
    }

    var globalConflictResolution: ConflictResolution? = null

    walk().filter { it.isDirectory() }.forEach { sourceDir ->
        coroutineContext.ensureActive()
        if (!shouldContinue()) return@withContext false

        val relativePath = sourceDir.relativeTo(this@copyRecursively)
        val destinationDir = target.resolve(relativePath)

        if (destinationDir.exists()) {
            if (destinationDir.isRegularFile()) {
                val resolution = globalConflictResolution ?: onConflict(
                    ConflictInfo(destinationDir, sourceDir.name, -1, "create directory")
                )

                when (resolution) {
                    ConflictResolution.ABORT -> return@withContext false
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
                        destinationDir.deleteIfExists() // Delete the conflicting file
                    }
                    ConflictResolution.RENAME -> {
                        // Renaming a directory in this context is complex and usually not desired.
                        // For simplicity, if RENAME is chosen for a directory conflict (source is dir, dest is file),
                        // we'll treat it as SKIP for the directory itself,
                        // assuming the user wants to keep the existing file and not create a directory there.
                        return@forEach
                    }
                }
            }
        }
        Files.createDirectories(destinationDir)
    }

    val sourceFiles = walk().filter { it.isRegularFile() }.toList()
    val totalSize = sourceFiles.sumOf { Files.readAttributes(it, BasicFileAttributes::class.java).size() }
    var copiedSize = 0L

    if (totalSize == 0L) {
        onProgress("", 1f)
        return@withContext true
    }

    for (sourceFile in sourceFiles) {
        coroutineContext.ensureActive() // Check for coroutine cancellation
        if (!shouldContinue()) return@withContext false

        val relativePath = sourceFile.relativeTo(this@copyRecursively)
        var destinationFile = target.resolve(relativePath)

        if (destinationFile.exists()) {
            val resolution = globalConflictResolution ?: onConflict(
                ConflictInfo(destinationFile, sourceFile.name, sourceFile.fileSize(), "copy")
            )

            when (resolution) {
                ConflictResolution.ABORT -> return@withContext false
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
                }

                ConflictResolution.RENAME -> {
                    destinationFile = destinationFile.parent.resolve(sourceFile.duplicateName())
                }
            }
        }

        try {
            val fileSize = sourceFile.fileSize()
            Files.copy(
                sourceFile,
                destinationFile,
                StandardCopyOption.REPLACE_EXISTING // Handles OVERWRITE case
            )
            copiedSize += fileSize
            onProgress(
                sourceFile.name,
                copiedSize.toFloat() / totalSize.toFloat()
            )
        } catch (e: Exception) {
            Log.e("Failed to copy file: ${sourceFile.absolutePathString()}", e.message!!)
            return@withContext false
        }
    }
    return@withContext true
}

fun Path.nameWithoutExtension(): String {
    return if (isDirectory()) name else nameWithoutExtension.ifEmpty { this.name }
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

fun Path.duplicate(
    onProgress: (fileName: String, progress: Float) -> Unit,
    shouldContinue: () -> Boolean = { true }
): Path {
    if (!exists()) return this
    val newName = duplicateName()
    val newPath = parent?.resolve(newName) ?: return this

    if (newPath.exists()) {
        onProgress(this.name, 1f)
        return newPath
    }

    try {
        if (isDirectory()) {
            Files.createDirectories(newPath)
            val paths = walk()
            val totalFiles = paths.count().toFloat()
            paths.forEachIndexed { pos, file ->
                if (!shouldContinue()) return this
                val relativePath = file.relativeTo(this)
                val targetFile = newPath.resolve(relativePath)
                onProgress(file.nameWithoutExtension, pos.toFloat() / totalFiles)
                Files.createDirectories(targetFile.parent)
                Files.copy(file, targetFile)
            }
        } else {
            Files.copy(this, newPath)
            onProgress(this.name, 1f)
        }
        return newPath
    } catch (e: Exception) {
        e.printStackTrace()
        return this
    }
}


fun Path.getLastModified(): String {
    return try {
        SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(
            getLastModifiedTime().toMillis()
        )
    } catch (e: Exception) {
        e.printStackTrace()
        "Error fetching last modified date"
    }
}
