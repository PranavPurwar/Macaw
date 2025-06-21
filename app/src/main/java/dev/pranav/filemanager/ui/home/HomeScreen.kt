package dev.pranav.filemanager.ui.home

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.pranav.filemanager.ui.preview.AudioPreviewDialog
import dev.pranav.filemanager.util.details
import dev.pranav.filemanager.util.orderedChildren
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    modifier: Modifier,
    initialDirectory: File,
    onDirectoryChange: (File) -> Unit,
    isCurrent: Boolean
) {
    val directory = remember { mutableStateOf(initialDirectory) }
    val files = remember { mutableStateListOf<File>() }
    val errorState = remember { mutableStateOf(false) }
    val errorMessage = remember { mutableStateOf("") }
    val context = LocalContext.current
    var loading by remember { mutableStateOf(true) }
    var filePreviewState by remember { mutableStateOf<FilePreviewState>(FilePreviewState.None) }

    if (errorState.value) {
        Snackbar {
            Text(
                AnnotatedString(errorMessage.value),
                color = MaterialTheme.colorScheme.error
            )
        }

        Toast.makeText(context, errorMessage.value, Toast.LENGTH_SHORT).show()
        errorState.value = false
    }

    BackHandler(enabled = isCurrent) {
        if (directory.value.parentFile?.canRead() == true) {
            directory.value = directory.value.parentFile!!
        } else {
            errorState.value = true
            errorMessage.value = "Cannot read parent directory"
        }
    }

    if (loading) {
        Box(Modifier.fillMaxSize()) {
            LoadingIndicator(
                modifier = Modifier.align(Alignment.Center).size(48.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }

    LaunchedEffect(directory.value.absolutePath) {
        loading = true
        onDirectoryChange(directory.value)
        val children = withContext(Dispatchers.IO) {
            directory.value.orderedChildren()
        }
        files.clear()
        files.addAll(children)
        loading = false
    }

    Box(modifier = modifier) {
        LazyColumn {
            items(files) { file ->

                Row (
                    Modifier.fillMaxWidth()
                        .clickable(true) {
                            if (file.isDirectory) {
                                if (file.canRead()) {
                                    directory.value = file
                                } else {
                                    errorState.value = true
                                    errorMessage.value = "Cannot read directory ${file.name}"
                                }
                            } else {
                                handleFileClick(context, file) { dialogFile ->
                                    filePreviewState = FilePreviewState.Audio(dialogFile)
                                }
                            }
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val imageModifier = Modifier.size(36.dp)

                    AsyncImage(
                        model = file,
                        contentDescription = file.name,
                        contentScale = ContentScale.Fit,
                        modifier = imageModifier
                    )

                    Column(
                        Modifier.padding(start = 12.dp)
                    ) {
                        Text(
                            file.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2
                        )

                        Text(
                            file.details(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        when (filePreviewState) {
            is FilePreviewState.Audio -> {
                val audioFile = (filePreviewState as FilePreviewState.Audio).file
                AudioPreviewDialog(audioFile = audioFile) {
                    filePreviewState = FilePreviewState.None
                }
            }
            FilePreviewState.None -> {}
        }
    }
}
