package dev.pranav.macaw.util.coil

import android.content.Context
import android.media.MediaMetadataRetriever
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.core.content.ContextCompat
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.fetch.SourceFetchResult
import coil3.map.Mapper
import coil3.request.Options
import dev.pranav.macaw.R
import dev.pranav.macaw.model.FileType
import dev.pranav.macaw.model.IMAGE_FORMATS
import dev.pranav.macaw.model.VIDEO_FORMATS
import dev.pranav.macaw.model.getFileType
import okio.FileSystem
import okio.buffer
import okio.source
import java.io.File

data class AudioFile(val file: File)

class FileMapper : Mapper<File, Any> {
    override fun map(data: File, options: Options): Any {
        if (data.isDirectory) return R.drawable.twotone_folder_24

        if (data.extension in IMAGE_FORMATS || data.extension in VIDEO_FORMATS || data.extension == "apk") {
            return data
        }

        return when (data.extension) {
            "zip" -> R.drawable.file_type_zip
            "pdf" -> R.drawable.file_type_pdf
            "json" -> R.drawable.file_type_json
            else -> when (data.getFileType()) {
                FileType.AUDIO -> AudioFile(data)
                FileType.ARCHIVE -> Icons.Default.Archive
                FileType.TEXT -> R.drawable.file_type_text
                FileType.CODE -> R.drawable.file_type_code
                else -> R.mipmap.unk
            }
        }
    }
}

class AudioFileFetcherFactory(private val context: Context) : Fetcher.Factory<AudioFile> {
    override fun create(data: AudioFile, options: Options, imageLoader: ImageLoader): Fetcher {
        return object : Fetcher {
            override suspend fun fetch(): FetchResult {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(data.file.absolutePath)
                    val art = retriever.embeddedPicture
                    if (art != null) {
                        return SourceFetchResult(
                            source = ImageSource(
                                art.inputStream().source().buffer(),
                                FileSystem.SYSTEM
                            ),
                            mimeType = "image/*",
                            dataSource = DataSource.DISK
                        )
                    }
                } catch (_: Exception) {
                } finally {
                    retriever.release()
                }

                val placeholder =
                    ContextCompat.getDrawable(context, R.drawable.rounded_music_note_24)!!
                return ImageFetchResult(
                    image = placeholder.asImage(),
                    dataSource = DataSource.DISK,
                    isSampled = false
                )
            }
        }
    }
}
