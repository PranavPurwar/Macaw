package dev.pranav.macaw.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import java.io.File

sealed class ConflictAction {
    object OVERWRITE : ConflictAction()
    object SKIP : ConflictAction()
    object ABORT : ConflictAction()
}

suspend fun File.extract(
    destinationDir: File,
    onProgress: (String) -> Unit,
    onConflict: suspend (File) -> ConflictAction
) = withContext(Dispatchers.IO) {
    if (!destinationDir.exists()) destinationDir.mkdirs()
    try {
        val stream: ArchiveInputStream<out ArchiveEntry> =
            ArchiveStreamFactory().createArchiveInputStream(inputStream().buffered())

        stream.use { archiveStream ->
            while (true) {
                val entry = archiveStream.nextEntry ?: break
                onProgress(entry.name)
                val outFile = File(destinationDir, entry.name)
                if (outFile.exists()) {
                    when (onConflict(outFile)) {
                        ConflictAction.SKIP -> continue
                        ConflictAction.ABORT -> break
                        ConflictAction.OVERWRITE -> {
                            if (outFile.isDirectory) {
                                outFile.deleteRecursively()
                            } else {
                                outFile.delete()
                            }
                        }
                    }
                }

                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    outFile.outputStream().use { output ->
                        archiveStream.copyTo(output)
                    }
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
