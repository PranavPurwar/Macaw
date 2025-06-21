package dev.pranav.macaw.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

suspend fun File.getHash(algorithm: String): String = withContext(Dispatchers.IO) {
    val digest = MessageDigest.getInstance(algorithm)
    FileInputStream(this@getHash).use { fis ->
        val byteArray = ByteArray(1024)
        var bytesCount = fis.read(byteArray)
        while (bytesCount != -1) {
            digest.update(byteArray, 0, bytesCount)
            bytesCount = fis.read(byteArray)
        }
    }
    digest.digest().joinToString("") { "%02x".format(it) }
}

