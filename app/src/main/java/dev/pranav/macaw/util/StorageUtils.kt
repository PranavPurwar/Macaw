package dev.pranav.macaw.util

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import java.io.File

data class StorageVolumeInfo(
    val file: File,
    val name: String,
    val isPrimary: Boolean,
    val isRemovable: Boolean
)

fun getStorageVolumes(context: Context): List<StorageVolumeInfo> {
    val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
    return storageManager.storageVolumes.mapNotNull { volume ->
        getVolumeFile(volume)?.let { file ->
            if (volume.state == Environment.MEDIA_MOUNTED) {
                val name = volume.getDescription(context) ?: file.name
                StorageVolumeInfo(file, name, volume.isPrimary, volume.isRemovable)
            } else {
                null
            }
        }
    }.distinctBy { it.file.absolutePath }
}

private fun getVolumeFile(volume: StorageVolume): File? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        volume.directory
    } else {
        try {
            volume::class.java.getMethod("getPathFile").invoke(volume) as? File
        } catch (_: Exception) {
            null
        }
    }
}

