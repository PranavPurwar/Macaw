package dev.pranav.macaw.ui.home

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.rajat.pdfviewer.PdfViewerActivity
import com.rajat.pdfviewer.util.saveTo
import dev.pranav.macaw.model.AUDIO_FORMATS
import dev.pranav.macaw.model.FileType
import dev.pranav.macaw.model.getFileType
import dev.pranav.macaw.ui.editor.TextEditorActivity
import dev.pranav.macaw.ui.preview.ImagePreviewActivity
import dev.pranav.macaw.ui.preview.VideoPreviewActivity
import java.io.File

enum class FileAction {
    OPEN_TEXT_EDITOR,
    OPEN_APK_DETAILS,
    OPEN_IMAGE_PREVIEW,
    OPEN_VIDEO_PREVIEW,
    OPEN_PDF_PREVIEW,
    HANDLE_AUDIO,
    OPEN_WITH_SYSTEM,
    SHARE,
    OPEN_WITH,
    CLONE,
    RENAME,
    DELETE,
    EDIT_WITH_CODE_EDITOR,
    COMPRESS,
    EXTRACT, // added extract action
    DETAILS,
    CUT,
    COPY,
    PASTE,
    CLEAR_CLIPBOARD
}

fun determineFileAction(file: File): FileAction {
    return when (file.extension) {
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

fun handleFileClick(context: Context, file: File, onDialogFileSelected: (File) -> Unit = {}) {
    when (determineFileAction(file)) {
        FileAction.HANDLE_AUDIO, FileAction.OPEN_APK_DETAILS -> onDialogFileSelected(file)

        else -> executeFileAction(context, file, determineFileAction(file))
    }
}

fun executeFileAction(context: Context, file: File, action: FileAction) {
    when (action) {
        FileAction.OPEN_TEXT_EDITOR -> {
            val intent = Intent(context, TextEditorActivity::class.java)
            intent.putExtra("file", file)
            context.startActivity(intent)
        }

        FileAction.OPEN_IMAGE_PREVIEW -> {
            val intent = Intent(context, ImagePreviewActivity::class.java)
            intent.putExtra("file", file)
            context.startActivity(intent)
        }

        FileAction.OPEN_VIDEO_PREVIEW -> {
            val intent = Intent(context, VideoPreviewActivity::class.java)
            intent.putExtra("file", file)
            context.startActivity(intent)
        }

        FileAction.OPEN_PDF_PREVIEW -> {
            PdfViewerActivity.launchPdfFromPath(
                context = context,
                path = file.absolutePath,
                pdfTitle = file.nameWithoutExtension,
                saveTo = saveTo.ASK_EVERYTIME,
            )
        }

        FileAction.OPEN_WITH_SYSTEM -> openWithSystemApp(context, file)
        FileAction.HANDLE_AUDIO -> {}
        else -> {}
    }
}

private fun openWithSystemApp(context: Context, file: File) {
    try {
        val intent = Intent(Intent.ACTION_VIEW)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        intent.setDataAndType(uri, null)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "No app found to open this file type", Toast.LENGTH_SHORT).show()
    }
}
