package dev.pranav.macaw.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import java.io.File

sealed class ConflictResolution {
    object OVERWRITE : ConflictResolution()
    object SKIP : ConflictResolution()
    object RENAME : ConflictResolution()
    object SKIP_ALL : ConflictResolution()
    object OVERWRITE_ALL : ConflictResolution()
    object ABORT : ConflictResolution()
}

data class ConflictInfo(
    val existingFile: File,
    val newFileName: String,
    val newFileSize: Long,
    val operation: String // "extract", "copy", "move"
)

suspend fun File.extract(
    destinationDir: File,
    onProgress: (entryName: String, progress: Float) -> Unit,
    onConflict: suspend (ConflictInfo) -> ConflictResolution,
    shouldContinue: () -> Boolean = { true }
) = withContext(Dispatchers.IO) {
    if (!destinationDir.exists()) destinationDir.mkdirs()

    var globalConflictResolution: ConflictResolution? = null

    try {
        val stream: ArchiveInputStream<out ArchiveEntry> =
            ArchiveStreamFactory().createArchiveInputStream(inputStream().buffered())
        val totalSize = length()

        stream.use { archiveStream ->
            while (true) {
                if (!shouldContinue()) break
                val entry = archiveStream.nextEntry ?: break
                onProgress(entry.name, archiveStream.bytesRead.toFloat() / totalSize)
                val outFile = File(destinationDir, entry.name)

                if (outFile.exists()) {
                    val resolution = globalConflictResolution ?: onConflict(
                        ConflictInfo(outFile, entry.name, entry.size, "extract")
                    )

                    when (resolution) {
                        ConflictResolution.SKIP, ConflictResolution.SKIP_ALL -> {
                            if (resolution == ConflictResolution.SKIP_ALL) {
                                globalConflictResolution = ConflictResolution.SKIP_ALL
                            }
                            continue
                        }

                        ConflictResolution.ABORT -> break
                        ConflictResolution.OVERWRITE, ConflictResolution.OVERWRITE_ALL -> {
                            if (resolution == ConflictResolution.OVERWRITE_ALL) {
                                globalConflictResolution = ConflictResolution.OVERWRITE_ALL
                            }
                            if (outFile.isDirectory) {
                                outFile.deleteRecursively()
                            } else {
                                outFile.delete()
                            }
                        }

                        ConflictResolution.RENAME -> {
                            var counter = 1
                            val nameWithoutExt = outFile.nameWithoutExtension
                            val extension =
                                if (outFile.extension.isNotEmpty()) ".${outFile.extension}" else ""

                            do {
                                val newFile =
                                    File(outFile.parent, "${nameWithoutExt} ($counter)$extension")
                                counter++
                                if (!newFile.exists()) {
                                    // Update outFile to the new name
                                    val newEntry = File(destinationDir, newFile.name)
                                    break
                                }
                            } while (true)
                        }
                    }
                }

                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    outFile.outputStream().use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var length: Int
                        while (archiveStream.read(buffer).also { length = it } > 0) {
                            if (!shouldContinue()) break
                            output.write(buffer, 0, length)
                        }
                    }
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
