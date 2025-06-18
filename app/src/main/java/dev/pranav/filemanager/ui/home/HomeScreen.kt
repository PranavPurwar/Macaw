package dev.pranav.filemanager.ui.home

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.pranav.filemanager.util.details
import dev.pranav.filemanager.util.openFile
import dev.pranav.filemanager.util.orderedChildren
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier,
    initialDirectory: File,
    onDirectoryChange: (File) -> Unit
) {
    val directory = remember { mutableStateOf(initialDirectory) }
    val files = remember { mutableStateListOf<File>() }
    val errorState = remember { mutableStateOf(false) }
    val errorMessage = remember { mutableStateOf("") }
    val context = LocalContext.current

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

    BackHandler {
        if (directory.value.parentFile?.canRead() == true) {
            directory.value = directory.value.parentFile!!
        } else {
            errorState.value = true
            errorMessage.value = "Cannot read parent directory"
        }
    }

    LaunchedEffect(directory.value.absolutePath) {
        onDirectoryChange(directory.value)
        files.clear()
        files.addAll(directory.value.orderedChildren())
    }

    Box(modifier = modifier) {
        LazyColumn(
        ) {
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
                                context.openFile(file)
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
    }
}
