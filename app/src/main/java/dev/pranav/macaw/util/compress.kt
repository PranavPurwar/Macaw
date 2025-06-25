package dev.pranav.macaw.util

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private const val DEFAULT_BUFFER_SIZE = 8 * 1024

fun File.compress(destination: File, onProgress: (String, Float) -> Unit) {
    val zipOutputStream = ZipOutputStream(FileOutputStream(destination))
    zipOutputStream.use { zos ->
        if (this.isDirectory) {
            val filesToZip = this.walkTopDown().filter { it.parent != null }.toList()
            val total = filesToZip.size
            filesToZip.forEachIndexed { index, file ->
                val entryName = this.toPath().relativize(file.toPath()).toString()
                onProgress(entryName, (index + 1).toFloat() / total.toFloat())
                if (entryName.isEmpty()) return@forEachIndexed

                if (file.isDirectory) {
                    zos.putNextEntry(ZipEntry("$entryName/"))
                    zos.closeEntry()
                } else {
                    zos.putNextEntry(ZipEntry(entryName))
                    FileInputStream(file).use { it.copyTo(zos) }
                    zos.closeEntry()
                }
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
                    zos.write(buffer, 0, len)
                    writtenBytes += len
                    onProgress(this.name, writtenBytes.toFloat() / totalSize.toFloat())
                }
            }
            zos.closeEntry()
            onProgress(this.name, 1f)
        }
    }
}

fun List<File>.compress(destination: File, onProgress: (String, Float) -> Unit) {
    val zipOutputStream = ZipOutputStream(FileOutputStream(destination))
    zipOutputStream.use { zos ->
        val allFiles = mutableListOf<File>()
        this.forEach { file ->
            if (file.isDirectory) {
                allFiles.addAll(file.walkTopDown())
            } else {
                allFiles.add(file)
            }
        }

        val total = allFiles.size
        allFiles.forEachIndexed { index, file ->
            val root = this.find { root -> file.absolutePath.startsWith(root.absolutePath) }
                ?: file.parentFile
            val entryName = root.parentFile.toPath().relativize(file.toPath()).toString()

            onProgress(entryName, (index + 1).toFloat() / total.toFloat())

            if (file.isDirectory) {
                if (entryName.isNotEmpty()) {
                    zos.putNextEntry(ZipEntry("$entryName/"))
                    zos.closeEntry()
                }
            } else {
                zos.putNextEntry(ZipEntry(entryName))
                FileInputStream(file).use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }
    }
}
