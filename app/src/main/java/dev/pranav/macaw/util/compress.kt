package dev.pranav.macaw.util

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.absolutePathString
import kotlin.io.path.fileSize
import kotlin.io.path.inputStream
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.outputStream
import kotlin.io.path.walk

private const val DEFAULT_BUFFER_SIZE = 8 * 1024

fun File.compress(
    destination: File, onProgress: (String, Float) -> Unit,
    shouldContinue: () -> Boolean = { true }
) {
    val zipOutputStream = ZipOutputStream(FileOutputStream(destination))
    zipOutputStream.use { zos ->
        if (this.isDirectory) {
            val filesToZip = this.walkTopDown().filter { it.isFile }.toList()
            val totalSize = filesToZip.sumOf { it.length() }
            var writtenBytes = 0L

            this.walkTopDown().filter { it.isDirectory }.forEach { dir ->
                if (!shouldContinue()) return@use
                val entryName = this.toPath().relativize(dir.toPath()).toString()
                if (entryName.isNotEmpty()) {
                    zos.putNextEntry(ZipEntry("$entryName/"))
                    zos.closeEntry()
                }
            }

            filesToZip.forEach { file ->
                if (!shouldContinue()) return@use
                val entryName = this.toPath().relativize(file.toPath()).toString()
                onProgress(
                    entryName,
                    if (totalSize > 0) writtenBytes.toFloat() / totalSize.toFloat() else 0f
                )
                if (entryName.isEmpty()) return@forEach

                zos.putNextEntry(ZipEntry(entryName))
                FileInputStream(file).use { fis ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var len: Int
                    while (fis.read(buffer).also { len = it } > 0) {
                        if (!shouldContinue()) return@use
                        zos.write(buffer, 0, len)
                        writtenBytes += len
                        onProgress(
                            entryName,
                            if (totalSize > 0) writtenBytes.toFloat() / totalSize.toFloat() else 0f
                        )
                    }
                }
                zos.closeEntry()
            }
        } else {
            onProgress(this.name, 0f)
            val totalSize = this.length()
            var writtenBytes = 0L
            zos.putNextEntry(ZipEntry(this.name))
            FileInputStream(this).use { fis ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var len: Int
                while (fis.read(buffer).also { len = it } > 0) {
                    if (!shouldContinue()) return@use
                    zos.write(buffer, 0, len)
                    writtenBytes += len
                    onProgress(
                        this.name,
                        if (totalSize > 0) writtenBytes.toFloat() / totalSize.toFloat() else 0f
                    )
                }
            }
            zos.closeEntry()
            onProgress(this.name, 1f)
        }
    }
}

fun List<Path>.compress(
    destination: Path, onProgress: (String, Float) -> Unit,
    shouldContinue: () -> Boolean = { true }
) {
    val zipOutputStream = ZipOutputStream(destination.outputStream())
    zipOutputStream.use { zos ->
        val allFiles = mutableListOf<Path>()
        this.forEach { file ->
            if (file.isDirectory()) {
                allFiles.addAll(file.walk())
            } else {
                allFiles.add(file)
            }
        }
        val distinctFiles = allFiles.distinctBy { it.absolutePathString() }

        val filesToZip = distinctFiles.filter { it.isRegularFile() }
        val totalSize = filesToZip.sumOf { it.fileSize() }
        var writtenBytes = 0L

        distinctFiles.filter { it.isDirectory() }.forEach { dir ->
            if (!shouldContinue()) return@use
            val root = this.find { root -> dir.startsWith(root) }
                ?: dir.parent
            val entryName = root.parent.relativize(dir).toString()
            if (entryName.isNotEmpty()) {
                zos.putNextEntry(ZipEntry("$entryName/"))
                zos.closeEntry()
            }
        }

        filesToZip.forEach { file ->
            if (!shouldContinue()) return@use
            val root = this.find { root -> file.startsWith(root) }
                ?: file.parent
            val entryName = root.parent.relativize(file).toString()

            onProgress(
                entryName,
                if (totalSize > 0) writtenBytes.toFloat() / totalSize.toFloat() else 0f
            )

            zos.putNextEntry(ZipEntry(entryName))
            file.inputStream().use { fis ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var len: Int
                while (fis.read(buffer).also { len = it } > 0) {
                    if (!shouldContinue()) return@use
                    zos.write(buffer, 0, len)
                    writtenBytes += len
                    onProgress(
                        entryName,
                        if (totalSize > 0) writtenBytes.toFloat() / totalSize.toFloat() else 0f
                    )
                }
            }
            zos.closeEntry()
        }
    }
}
