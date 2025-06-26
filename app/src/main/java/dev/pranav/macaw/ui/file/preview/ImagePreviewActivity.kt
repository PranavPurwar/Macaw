package dev.pranav.macaw.ui.file.preview

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.unit.dp
import dev.pranav.macaw.model.FileType
import dev.pranav.macaw.model.getFileType
import dev.pranav.macaw.ui.theme.FileManagerTheme
import dev.pranav.macaw.util.orderedChildren
import me.saket.telephoto.zoomable.OverzoomEffect
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableState
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.isReadable
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

class ImagePreviewActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val file = Path(intent.getStringExtra("file") ?: "")
        var files = emptyList<Path>()
        var imagesInDir = 0
        var fileIndex = 0

        val parentDirectory = file.parent
        if (parentDirectory != null && parentDirectory.isReadable()) {
            files = parentDirectory.orderedChildren()
                .filter { it.isRegularFile() && it.getFileType() == FileType.IMAGE }
            imagesInDir = files.size
            fileIndex = files.indexOfFirst { it.name == file.name }
        }

        if (file.isRegularFile().not() || file.getFileType() != FileType.IMAGE) {
            Toast.makeText(this, "Error: File not found", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setContent {
            FileManagerTheme {
                val pagerState = rememberPagerState(fileIndex) { files.size }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = files[pagerState.currentPage].name,
                                    style = MaterialTheme.typography.titleMediumEmphasized,
                                    fontFamily = FontFamily.SansSerif,
                                    overflow = TextOverflow.MiddleEllipsis,
                                    maxLines = 1
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    Column(
                        Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                    ) {
                        HorizontalPager(
                            modifier = Modifier.weight(1f),
                            state = pagerState,
                            key = { files[it].absolutePathString() },
                            snapPosition = SnapPosition.Center
                        ) { page ->
                            if (page < 0 || page >= files.size) {
                                // Handle out of bounds
                                return@HorizontalPager
                            }
                            val currentFile = files[page]

                            val zoomState = rememberZoomableState(ZoomSpec(
                                maxZoomFactor = 100f,
                                overzoomEffect = OverzoomEffect.NoLimits
                            ))
                            val zoomableState = rememberZoomableImageState(
                                zoomState
                            )

                            ZoomableAsyncImage(
                                modifier = Modifier
                                    .fillMaxSize(),
                                model = currentFile.toFile(),
                                state = zoomableState,
                                contentDescription = currentFile.name
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        ) {
                            Text(
                                text = "Image ${pagerState.currentPage + 1} of $imagesInDir",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}
