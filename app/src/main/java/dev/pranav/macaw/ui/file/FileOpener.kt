package dev.pranav.macaw.ui.file

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import dev.pranav.macaw.model.AUDIO_FORMATS
import dev.pranav.macaw.model.FileAction
import dev.pranav.macaw.model.FileType
import dev.pranav.macaw.model.getFileType
import dev.pranav.macaw.ui.editor.TextEditorActivity
import dev.pranav.macaw.ui.file.preview.ImagePreviewActivity
import dev.pranav.macaw.ui.file.preview.PDFPreviewActivity
import dev.pranav.macaw.ui.file.preview.VideoPreviewActivity
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.isReadable
import kotlin.io.path.name

fun handleFileClick(
    file: Path,
    context: Context,
    onDirectoryChange: (Path) -> Unit,
    onShowApkBottomSheet: (Path) -> Unit,
    onShowAudioPreview: (Path) -> Unit,
    onError: (String) -> Unit
) {
    if (file.isDirectory()) {
        if (file.isReadable()) {
            onDirectoryChange(file)
        } else {
            onError("Cannot read directory ${file.name}")
        }
    } else {
        when (val action = determineFileAction(file)) {
            FileAction.HANDLE_AUDIO -> onShowAudioPreview(file)
            FileAction.OPEN_APK_DETAILS -> onShowApkBottomSheet(file)
            else -> executeFileAction(context, file, action)
        }
    }
}

fun determineFileAction(file: Path): FileAction {
    return when (file.extension.lowercase()) {
        in AUDIO_FORMATS -> FileAction.HANDLE_AUDIO

        "apk" -> FileAction.OPEN_APK_DETAILS
        "pdf" -> FileAction.OPEN_PDF_PREVIEW
        else -> when (file.getFileType()) {
            FileType.TEXT, FileType.CODE -> FileAction.OPEN_TEXT_EDITOR
            FileType.IMAGE -> FileAction.OPEN_IMAGE_PREVIEW
            FileType.VIDEO -> FileAction.OPEN_VIDEO_PREVIEW
            else -> FileAction.OPEN_WITH_SYSTEM
        }
    }
}

fun executeFileAction(context: Context, file: Path, action: FileAction) {
    when (action) {
        FileAction.OPEN_TEXT_EDITOR -> {
            val intent = Intent(context, TextEditorActivity::class.java)
            intent.putExtra("file", file.absolutePathString())
            context.startActivity(intent)
        }

        FileAction.OPEN_IMAGE_PREVIEW -> {
            val intent = Intent(context, ImagePreviewActivity::class.java)
            intent.putExtra("file", file.absolutePathString())
            context.startActivity(intent)
        }

        FileAction.OPEN_VIDEO_PREVIEW -> {
            val intent = Intent(context, VideoPreviewActivity::class.java)
            intent.putExtra("file", file.absolutePathString())
            context.startActivity(intent)
        }

        FileAction.OPEN_PDF_PREVIEW -> {
            val intent = Intent(context, PDFPreviewActivity::class.java)
            intent.putExtra("file", file.absolutePathString())
            context.startActivity(intent)
        }

        FileAction.OPEN_WITH_SYSTEM -> openWithSystemApp(context, file)
        FileAction.HANDLE_AUDIO -> {}
        else -> {}
    }
}

private fun openWithSystemApp(context: Context, file: Path) {
    try {
        val intent = Intent(Intent.ACTION_VIEW)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file.toFile()
        )
        intent.setDataAndType(uri, null)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "No app found to open this file type", Toast.LENGTH_SHORT).show()
    }
}

