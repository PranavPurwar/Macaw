package dev.pranav.filemanager.util

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import dev.pranav.filemanager.model.FileType
import dev.pranav.filemanager.model.getFileType
import dev.pranav.filemanager.ui.editor.TextEditorActivity
import dev.pranav.filemanager.ui.preview.ImagePreviewActivity
import dev.pranav.filemanager.ui.preview.VideoPreviewActivity
import java.io.File

@Composable
fun Context.OpenFile(file: File) {
    when (file.getFileType()) {
        FileType.AUDIO -> {

        }
        FileType.TEXT, FileType.CODE -> {
            val intent = Intent(this, TextEditorActivity::class.java)
            intent.putExtra("file", file)
            startActivity(intent)
        }
        FileType.IMAGE -> {
            val intent = Intent(this, ImagePreviewActivity::class.java)
            intent.putExtra("file", file)
            startActivity(intent)
        }

        FileType.VIDEO -> {
            val intent = Intent(this, VideoPreviewActivity::class.java)
            intent.putExtra("file", file)
            startActivity(intent)
        }

        else -> {}
    }
}
