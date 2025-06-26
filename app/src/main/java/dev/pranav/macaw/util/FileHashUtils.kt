package dev.pranav.macaw.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.inputStream

suspend fun File.getHash(algorithm: String): String = withContext(Dispatchers.IO) {
    val digest = MessageDigest.getInstance(algorithm)
    inputStream().use { fis ->
        val byteArray = ByteArray(1024)
        var bytesCount = fis.read(byteArray)
        while (bytesCount != -1) {
            digest.update(byteArray, 0, bytesCount)
            bytesCount = fis.read(byteArray)
        }
    }
    digest.digest().joinToString("") { "%02x".format(it) }
}

suspend fun Path.getHash(algorithm: String): String = withContext(Dispatchers.IO) {
    val digest = MessageDigest.getInstance(algorithm)
    inputStream().use { inputStream ->
        val byteArray = ByteArray(1024)
        var bytesCount = inputStream.read(byteArray)
        while (bytesCount != -1) {
            digest.update(byteArray, 0, bytesCount)
            bytesCount = inputStream.read(byteArray)
        }
    }

    digest.digest().joinToString("") { "%02x".format(it) }
}

