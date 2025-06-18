package dev.pranav.filemanager.ui.editor

import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import dev.pranav.filemanager.ui.theme.FileManagerTheme
import io.github.rosemoe.sora.text.Content
import java.io.File

class TextEditorActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val file = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("file", File::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("file") as? File
        }

        if (file == null) {
            Toast.makeText(this, "Error: File not found", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        val state = CodeEditorState(initialContent = Content(file.readText()))

        setContent {
            FileManagerTheme {
                val editor = remember {
                    setCodeEditorFactory(
                        context = this,
                        state = state
                    )
                }

                EditorScreen(
                    file = file,
                    editor = editor,
                    onSave = {
                        try {
                            file.writeText(editor.text.toString())
                            Toast.makeText(this, "File saved", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(this, "Error saving file: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                )
            }
        }
    }
}
