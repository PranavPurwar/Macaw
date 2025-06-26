package dev.pranav.macaw.ui.file.preview

import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.rajat.pdfviewer.compose.PdfRendererViewCompose
import com.rajat.pdfviewer.util.CacheStrategy
import com.rajat.pdfviewer.util.PdfSource
import dev.pranav.macaw.ui.theme.FileManagerTheme
import java.io.File

class PDFPreviewActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
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

        setContent {
            FileManagerTheme {

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = file.name,
                                    style = MaterialTheme.typography.titleMediumEmphasized,
                                    fontFamily = FontFamily.SansSerif,
                                    overflow = TextOverflow.MiddleEllipsis,
                                    maxLines = 1
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back"
                                    )
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    PdfRendererViewCompose(
                        source = PdfSource.LocalFile(file),
                        lifecycleOwner = LocalLifecycleOwner.current,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        cacheStrategy = CacheStrategy.MAXIMIZE_PERFORMANCE
                    )
                }
            }
        }
    }
}
