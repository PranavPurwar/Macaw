package dev.pranav.filemanager.util.coil

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import coil3.map.Mapper
import coil3.request.Options
import dev.pranav.filemanager.R
import dev.pranav.filemanager.model.FileType
import dev.pranav.filemanager.model.IMAGE_FORMATS
import dev.pranav.filemanager.model.VIDEO_FORMATS
import dev.pranav.filemanager.model.getFileType
import java.io.File

class FileMapper : Mapper<File, Any> {
    override fun map(data: File, options: Options): Any? {
        val fileType = data.getFileType()

        if (data.isDirectory) return R.drawable.twotone_folder_24

        if (data.extension in IMAGE_FORMATS || data.extension in VIDEO_FORMATS || data.extension == "apk") {
            return data
        }

        when (data.extension) {
            "zip" -> return R.drawable.file_type_zip
            "pdf" -> return R.drawable.file_type_pdf
            "json" -> return R.drawable.file_type_json
        }

        return when (fileType) {
            FileType.ARCHIVE ->  Icons.Default.Archive
            FileType.AUDIO -> extractAudioThumbnail(data.absolutePath) ?: R.drawable.rounded_music_note_24
            FileType.TEXT -> R.drawable.file_type_text
            FileType.CODE -> R.drawable.file_type_code
            else -> R.mipmap.unk
        }
    }


    private fun extractAudioThumbnail(filePath: String): Bitmap? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(filePath)
            val embeddedPicture = retriever.embeddedPicture
            retriever.release()

            embeddedPicture?.let {
                BitmapFactory.decodeByteArray(it, 0, it.size)
            }
        } catch (_: Exception) {
            null
        }
    }

}

