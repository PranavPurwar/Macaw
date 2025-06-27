package dev.pranav.macaw.ui.editor

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.pranav.macaw.ui.theme.FileManagerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.io.path.writeText

class TextEditorActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val file = Path(intent.getStringExtra("file") ?: "")

        if (!file.exists() || !file.isRegularFile()) {
            Toast.makeText(this, "Error: File not found", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        val state = CodeEditorState()

        setContent {
            FileManagerTheme {
                var loading by remember { mutableStateOf(true) }
                val editor = remember {
                    setCodeEditorFactory(
                        context = this,
                        state = state
                    )
                }

                LaunchedEffect(Unit) {
                    val content = withContext(Dispatchers.IO) {
                        file.readText()
                    }
                    state.editor!!.setText(content)
                    editor.applyEditorSettings(file)
                    loading = false
                }

                if (loading) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        Alignment.Center
                    ) {
                        LoadingIndicator()
                    }
                } else {
                    EditorScreen(
                        file = file,
                        editor = editor,
                        onSave = {
                            try {
                                file.writeText(editor.text.toString())
                                Toast.makeText(this, "File saved", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(
                                    this,
                                    "Error saving file: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    )
                }
            }
        }
    }
}
