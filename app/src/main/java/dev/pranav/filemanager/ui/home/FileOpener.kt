package dev.pranav.filemanager.ui.home

import android.content.Context
import android.content.Intent
import android.widget.Toast
import dev.pranav.filemanager.model.AUDIO_FORMATS
import dev.pranav.filemanager.model.FileType
import dev.pranav.filemanager.model.getFileType
import dev.pranav.filemanager.ui.editor.TextEditorActivity
import dev.pranav.filemanager.ui.preview.ImagePreviewActivity
import dev.pranav.filemanager.ui.preview.VideoPreviewActivity
import java.io.File

fun openFileByType(context: Context, file: File) {
    when (file.getFileType()) {
        FileType.TEXT, FileType.CODE -> {
            val intent = Intent(context, TextEditorActivity::class.java)
            intent.putExtra("file", file)
            context.startActivity(intent)
        }
        FileType.IMAGE -> {
            val intent = Intent(context, ImagePreviewActivity::class.java)
            intent.putExtra("file", file)
            context.startActivity(intent)
        }
        FileType.VIDEO -> {
            val intent = Intent(context, VideoPreviewActivity::class.java)
            intent.putExtra("file", file)
            context.startActivity(intent)
        }
        else -> {
            openFileWithSystemApp(context, file)
        }
    }
}

fun handleFileClick(context: Context, file: File, onDialogFileSelected: (File) -> Unit = {}) {
    when {
        file.extension in AUDIO_FORMATS -> {
            onDialogFileSelected(file)
        }
        else -> {
            openFileByType(context, file)
        }
    }
}

private fun openFileWithSystemApp(context: Context, file: File) {
    try {
        val intent = Intent(Intent.ACTION_VIEW)
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        intent.setDataAndType(uri, getMimeType(file))
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "No app found to open this file type", Toast.LENGTH_SHORT).show()
    }
}

private fun getMimeType(file: File): String {
    return when (file.getFileType()) {
        FileType.TEXT -> "text/plain"
        FileType.CODE -> "text/plain"
        FileType.IMAGE -> "image/*"
        FileType.VIDEO -> "video/*"
        FileType.AUDIO -> "audio/*"
        FileType.ARCHIVE -> "application/zip"
        else -> "*/*"
    }
}

