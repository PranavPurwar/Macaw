package dev.pranav.macaw.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteIfExists
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.fileSize
import kotlin.io.path.inputStream
import kotlin.io.path.isDirectory
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.outputStream

sealed class ConflictResolution {
    object OVERWRITE : ConflictResolution()
    object SKIP : ConflictResolution()
    object RENAME : ConflictResolution()
    object SKIP_ALL : ConflictResolution()
    object OVERWRITE_ALL : ConflictResolution()
    object ABORT : ConflictResolution()
}

data class ConflictInfo(
    val existingFile: Path,
    val newFileName: String,
    val newFileSize: Long,
    val operation: String // "extract", "copy", "move"
)

@OptIn(ExperimentalPathApi::class)
suspend fun Path.extract(
    destinationDir: Path,
    onProgress: (entryName: String, progress: Float) -> Unit,
    onConflict: suspend (ConflictInfo) -> ConflictResolution,
    shouldContinue: () -> Boolean = { true }
) = withContext(Dispatchers.IO) {
    if (!destinationDir.exists()) Files.createDirectories(destinationDir)

    var globalConflictResolution: ConflictResolution? = null

    try {
        val stream: ArchiveInputStream<out ArchiveEntry> =
            ArchiveStreamFactory().createArchiveInputStream(inputStream().buffered())
        val totalSize = fileSize()

        stream.use { archiveStream ->
            while (true) {
                if (!shouldContinue()) break
                val entry = archiveStream.nextEntry ?: break
                onProgress(entry.name, archiveStream.bytesRead.toFloat() / totalSize)
                var outFile = destinationDir.resolve(entry.name)

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
                            if (outFile.isDirectory()) {
                                outFile.deleteRecursively()
                            } else {
                                outFile.deleteIfExists()
                            }
                        }

                        ConflictResolution.RENAME -> {
                            var counter = 1
                            val nameWithoutExt = outFile.nameWithoutExtension
                            val extension =
                                if (outFile.extension.isNotEmpty()) ".${outFile.extension}" else ""
                            var renamedFile: Path
                            do {
                                renamedFile =
                                    outFile.parent.resolve("${nameWithoutExt} ($counter)$extension")
                                counter++
                            } while (renamedFile.exists())
                            outFile = renamedFile
                        }
                    }
                }

                if (entry.isDirectory) {
                    Files.createDirectories(outFile)
                } else {
                    Files.createDirectories(outFile.parent)
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
