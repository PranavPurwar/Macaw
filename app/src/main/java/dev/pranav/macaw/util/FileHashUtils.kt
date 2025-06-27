package dev.pranav.macaw.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Path
import java.security.MessageDigest
import java.util.zip.CRC32
import kotlin.io.path.inputStream

data class Hashes(
    val md5: String,
    val sha1: String,
    val sha256: String,
    val sha512: String,
    val crc32: String
)

// Calculates multiple hashes for a file at once since its more efficient than calculating them separately.
suspend fun Path.getHashes(): Hashes = withContext(Dispatchers.IO) {
    val md5Digest = MessageDigest.getInstance("MD5")
    val sha1Digest = MessageDigest.getInstance("SHA-1")
    val sha256Digest = MessageDigest.getInstance("SHA-256")
    val sha512Digest = MessageDigest.getInstance("SHA-512")
    val crc = CRC32()

    inputStream().use { inputStream ->
        val byteArray = ByteArray(1024)
        var bytesCount = inputStream.read(byteArray)
        while (bytesCount != -1) {
            md5Digest.update(byteArray, 0, bytesCount)
            sha1Digest.update(byteArray, 0, bytesCount)
            sha256Digest.update(byteArray, 0, bytesCount)
            sha512Digest.update(byteArray, 0, bytesCount)
            crc.update(byteArray, 0, bytesCount)
            bytesCount = inputStream.read(byteArray)
        }
    }

    return@withContext Hashes(
        md5 = md5Digest.digest().joinToString("") { "%02x".format(it) },
        sha1 = sha1Digest.digest().joinToString("") { "%02x".format(it) },
        sha256 = sha256Digest.digest().joinToString("") { "%02x".format(it) },
        sha512 = sha512Digest.digest().joinToString("") { "%02x".format(it) },
        crc32 = "%08x".format(crc.value)
    )
}
