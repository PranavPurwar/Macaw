package dev.pranav.macaw.ui.home

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.twotone.Description
import androidx.compose.material.icons.twotone.Folder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.pranav.macaw.R
import dev.pranav.macaw.ui.theme.FileManagerTheme
import dev.pranav.macaw.util.BookmarksManager
import java.io.File

class BookmarksActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FileManagerTheme {
                BookmarksScreen(
                    onBookmarkClick = { file ->
                        val resultIntent = Intent()
                        resultIntent.putExtra("path", file.absolutePath)
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    },
                    onNavigateUp = {
                        finish()
                    }
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(
    onBookmarkClick: (File) -> Unit,
    onNavigateUp: () -> Unit
) {
    val context = LocalContext.current
    var bookmarkedPaths by remember {
        mutableStateOf(
            BookmarksManager.getBookmarkedPaths(context).toList()
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bookmarks") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues)
        ) {
            items(bookmarkedPaths) { path ->
                val file = File(path)
                ListItem(
                    headlineContent = { Text(file.name) },
                    supportingContent = { Text(file.parent ?: "") },
                    leadingContent = {
                        val icon = if (file.isDirectory) {
                            Icons.TwoTone.Folder
                        } else {
                            Icons.TwoTone.Description
                        }
                        AsyncImage(
                            model = file,
                            placeholder = painterResource(if (file.isDirectory) R.drawable.twotone_folder_24 else R.mipmap.unk),
                            error = painterResource(if (file.isDirectory) R.drawable.twotone_folder_24 else R.mipmap.unk),
                            contentDescription = file.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(36.dp)
                        )
                    },
                    trailingContent = {
                        IconButton(onClick = {
                            BookmarksManager.removeBookmark(context, path)
                            bookmarkedPaths = BookmarksManager.getBookmarkedPaths(context).toList()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove bookmark")
                        }
                    },
                    modifier = Modifier.clickable { onBookmarkClick(file) }
                )
            }
        }
    }
}

