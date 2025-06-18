package dev.pranav.filemanager.util

import android.content.Context
import android.content.Intent
import dev.pranav.filemanager.model.FileType
import dev.pranav.filemanager.model.getFileType
import dev.pranav.filemanager.ui.editor.TextEditorActivity
import java.io.File

fun Context.openFile(file: File) {
    when (file.getFileType()) {
        FileType.TEXT, FileType.CODE -> {
            val intent = Intent(this, TextEditorActivity::class.java)
            intent.putExtra("file", file)
            startActivity(intent)
        }
        else -> {}
    }
}
