package dev.pranav.macaw.ui.home

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.OpenInNew
import androidx.compose.material.icons.twotone.Clear
import androidx.compose.material.icons.twotone.Compress
import androidx.compose.material.icons.twotone.ContentCopy
import androidx.compose.material.icons.twotone.ContentCut
import androidx.compose.material.icons.twotone.ContentPaste
import androidx.compose.material.icons.twotone.Delete
import androidx.compose.material.icons.twotone.DriveFileRenameOutline
import androidx.compose.material.icons.twotone.Edit
import androidx.compose.material.icons.twotone.FileCopy
import androidx.compose.material.icons.twotone.Info
import androidx.compose.material.icons.twotone.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import dev.pranav.macaw.R
import dev.pranav.macaw.ui.editor.TextEditorActivity
import dev.pranav.macaw.ui.preview.ApkBottomSheet
import dev.pranav.macaw.ui.preview.AudioPreviewDialog
import dev.pranav.macaw.util.Clipboard
import dev.pranav.macaw.util.cloneFile
import dev.pranav.macaw.util.compress
import dev.pranav.macaw.util.deleteFile
import dev.pranav.macaw.util.details
import dev.pranav.macaw.util.getHash
import dev.pranav.macaw.util.getLastModifiedDate
import dev.pranav.macaw.util.orderedChildren
import dev.pranav.macaw.util.rename
import dev.pranav.macaw.util.sizeString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFileAttributes
import java.nio.file.attribute.PosixFilePermissions

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class,
)
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
    var loading by remember { mutableStateOf(false) }
    var filePreviewState by remember { mutableStateOf<FilePreviewState>(FilePreviewState.None) }
    val coroutineScope = rememberCoroutineScope()

    var showBottomSheet by remember { mutableStateOf(false) }
    var longClickedFile by remember { mutableStateOf<File?>(null) }
    val sheetState = rememberModalBottomSheetState()
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var showApkBottomSheet by remember { mutableStateOf(false) }

    suspend fun loadFiles(firstLoad: Boolean = false) {
        if (!firstLoad) loading = true
        try {
            val children = withContext(Dispatchers.IO) {
                directory.value.orderedChildren()
            }
            files.clear()
            files.addAll(children)
        } catch (e: Exception) {
            errorState.value = true
            errorMessage.value = "Error loading files: ${e.message}"
        } finally {
            if (!firstLoad) loading = false
        }
    }

    suspend fun handleFileAction(context: Context, file: File, action: FileAction) {
        withContext(Dispatchers.IO) {
            when (action) {
                FileAction.SHARE -> {
                    val uri =
                        FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        type = context.contentResolver.getType(uri)
                        putExtra(Intent.EXTRA_STREAM, uri)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share File"))
                }

                FileAction.OPEN_WITH -> {
                    val uri =
                        FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        setDataAndType(uri, context.contentResolver.getType(uri))
                    }
                    context.startActivity(Intent.createChooser(intent, "Open File"))
                }

                FileAction.CLONE -> {
                    file.cloneFile()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "File cloned", Toast.LENGTH_SHORT).show()
                    }
                }

                FileAction.DELETE -> {
                    if (file.deleteFile()) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "File deleted", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Failed to delete file", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }

                FileAction.EDIT_WITH_CODE_EDITOR -> {
                    val intent = Intent(context, TextEditorActivity::class.java).apply {
                        putExtra("file", file)
                    }
                    context.startActivity(intent)
                }

                FileAction.COMPRESS -> {
                    val destination = File(file.parentFile, "${file.nameWithoutExtension}.zip")
                    file.compress(destination)
                }

                FileAction.CUT -> {
                    Clipboard.cut(file)
                }

                FileAction.COPY -> {
                    Clipboard.copy(file)
                }

                FileAction.PASTE -> {
                    Clipboard.paste(directory.value)
                }

                FileAction.CLEAR_CLIPBOARD -> {
                    Clipboard.clear()
                }

                else -> {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "$action not implemented yet", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        }
    }

    if (showRenameDialog) {
        RenameDialog(
            file = longClickedFile!!,
            onDismiss = { showRenameDialog = false },
            onRename = {
                coroutineScope.launch {
                    withContext(Dispatchers.IO) {
                        longClickedFile!!.rename(it)
                    }
                    loadFiles()
                }
            }
        )
    }

    if (showDetailsDialog) {
        DetailsSheet(
            file = longClickedFile!!,
            onDismiss = { showDetailsDialog = false }
        )
    }

    if (showApkBottomSheet && longClickedFile != null) {
        ApkBottomSheet(
            file = longClickedFile!!,
            onDismiss = { showApkBottomSheet = false }
        )
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
        ) {
            FileActionBottomSheet(
                onDismiss = { showBottomSheet = false },
                onAction = {
                    coroutineScope.launch {
                        when (it) {
                            FileAction.RENAME -> showRenameDialog = true
                            FileAction.DETAILS -> {
                                if (longClickedFile?.extension?.lowercase() == "apk") {
                                    showApkBottomSheet = true
                                } else {
                                    showDetailsDialog = true
                                }
                                showBottomSheet = false
                            }

                            else -> {
                                handleFileAction(context, longClickedFile!!, it)
                                loadFiles()
                            }
                        }
                    }
                }
            )
        }
    }

    val currentPath by remember(directory.value) {
        derivedStateOf { directory.value.absolutePath }
    }

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

    LaunchedEffect(currentPath) {
        onDirectoryChange(directory.value)
        loadFiles(true)
    }

    Box(modifier = modifier) {
        PullToRefreshBox(
            isRefreshing = loading,
            onRefresh = {
                coroutineScope.launch {
                    loadFiles()
                    onDirectoryChange(directory.value)
                }
            },
            modifier = Modifier.fillMaxSize()
        ) {
            if (loading && files.isEmpty()) {
                Box(Modifier.fillMaxSize()) {
                    LoadingIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(48.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                LazyColumn {
                    items(
                        items = files,
                        key = { it.absolutePath }
                    ) { file ->
                        FileItem(
                            file = file,
                            onFileClick = { clickedFile ->
                                if (clickedFile.isDirectory) {
                                    if (clickedFile.canRead()) {
                                        directory.value = clickedFile
                                    } else {
                                        errorState.value = true
                                        errorMessage.value =
                                            "Cannot read directory ${clickedFile.name}"
                                    }
                                } else {
                                    handleFileClick(context, clickedFile) { dialogFile ->
                                        filePreviewState = FilePreviewState.Audio(dialogFile)
                                    }
                                }
                            },
                            onFileLongClick = {
                                longClickedFile = it
                                showBottomSheet = true
                            }
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
        if (Clipboard.hasFile()) {
            Box(modifier = Modifier.fillMaxSize()) {
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            handleFileAction(context, longClickedFile!!, FileAction.PASTE)
                            loadFiles()
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Icon(Icons.TwoTone.ContentPaste, contentDescription = "Paste")
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileItem(
    file: File,
    onFileClick: (File) -> Unit,
    onFileLongClick: (File) -> Unit
) {
    val name = remember(file.absolutePath) { file.name }
    var details by remember(file.absolutePath) { mutableStateOf("Loading...") }

    LaunchedEffect(file.absolutePath) {
        details = withContext(Dispatchers.IO) {
            file.details()
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onFileClick(file) },
                onLongClick = { onFileLongClick(file) }
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        AsyncImage(
            model = file,
            placeholder = painterResource(if (file.isDirectory) R.drawable.twotone_folder_24 else R.mipmap.unk),
            error = painterResource(if (file.isDirectory) R.drawable.twotone_folder_24 else R.mipmap.unk),
            contentDescription = file.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(36.dp)
        )

        Column(
            Modifier.padding(start = 12.dp)
        ) {
            Text(
                name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2
            )

            Text(
                details,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FileActionBottomSheet(
    onDismiss: () -> Unit,
    onAction: (FileAction) -> Unit
) {
    val actions = listOfNotNull(
        FileAction.OPEN_WITH to "Open with",
        FileAction.CUT to "Cut",
        FileAction.COPY to "Copy",
        if (Clipboard.hasFile()) FileAction.PASTE to "Paste" else null,
        FileAction.DELETE to "Delete",
        FileAction.RENAME to "Rename",
        FileAction.SHARE to "Share",
        FileAction.DETAILS to "Details",
        FileAction.COMPRESS to "Compress",
        FileAction.CLONE to "Clone",
        FileAction.EDIT_WITH_CODE_EDITOR to "Edit with code editor",
        if (Clipboard.hasFile()) FileAction.CLEAR_CLIPBOARD to "Clear clipboard" else null
    )

    LazyColumn {
        items(actions) { (action, label) ->
            val icon = when (action) {
                FileAction.SHARE -> Icons.TwoTone.Share
                FileAction.OPEN_WITH -> Icons.AutoMirrored.TwoTone.OpenInNew
                FileAction.CLONE -> Icons.TwoTone.FileCopy
                FileAction.RENAME -> Icons.TwoTone.DriveFileRenameOutline
                FileAction.DELETE -> Icons.TwoTone.Delete
                FileAction.EDIT_WITH_CODE_EDITOR -> Icons.TwoTone.Edit
                FileAction.COMPRESS -> Icons.TwoTone.Compress
                FileAction.DETAILS -> Icons.TwoTone.Info
                FileAction.CUT -> Icons.TwoTone.ContentCut
                FileAction.COPY -> Icons.TwoTone.ContentCopy
                FileAction.PASTE -> Icons.TwoTone.ContentPaste
                FileAction.CLEAR_CLIPBOARD -> Icons.TwoTone.Clear
                else -> Icons.TwoTone.Info
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onAction(action)
                        onDismiss()
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )

                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp)
                )
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }
    }
}

@Composable
fun RenameDialog(
    file: File,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    var newName by remember { mutableStateOf(file.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename") },
        text = {
            androidx.compose.material3.OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("New name") }
            )
        },
        confirmButton = {
            ElevatedButton(
                onClick = {
                    onRename(newName)
                    onDismiss()
                }
            ) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsSheet(file: File, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    var md5 by remember { mutableStateOf<String?>(null) }
    var sha1 by remember { mutableStateOf<String?>(null) }
    var sha256 by remember { mutableStateOf<String?>(null) }
    var sha512 by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(file) {
        if (file.isFile) {
            coroutineScope.launch {
                md5 = file.getHash("MD5")
                sha1 = file.getHash("SHA-1")
                sha256 = file.getHash("SHA-256")
                sha512 = file.getHash("SHA-512")
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        SelectionContainer {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = file,
                        placeholder = painterResource(if (file.isDirectory) R.drawable.twotone_folder_24 else R.mipmap.unk),
                        error = painterResource(if (file.isDirectory) R.drawable.twotone_folder_24 else R.mipmap.unk),
                        contentDescription = file.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(48.dp)
                    )
                    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                        Text(file.name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            file.absolutePath,
                            modifier = Modifier.padding(top = 8.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                LazyColumn(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (file.isFile) {
                        item { DetailItem("Extension", file.extension) }
                    }
                    item { DetailItem("Last modified", file.getLastModifiedDate()) }
                    item { DetailItem("Size", file.length().sizeString()) }

                    try {
                        val attrs =
                            Files.readAttributes(file.toPath(), PosixFileAttributes::class.java)
                        item {
                            DetailItem(
                                "Permissions",
                                PosixFilePermissions.toString(attrs.permissions())
                            )
                        }
                        item { DetailItem("Owner", attrs.owner().name) }
                        item { DetailItem("Group", attrs.group().name) }
                    } catch (_: Exception) {
                        item { DetailItem("Permissions", "Not available") }
                        item { DetailItem("Owner", "Not available") }
                        item { DetailItem("Group", "Not available") }
                    }

                    if (file.isFile) {
                        item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                HashDetailItem("MD5", md5 ?: "Calculating...")
                                HashDetailItem("SHA1", sha1 ?: "Calculating...")
                                HashDetailItem("SHA256", sha256 ?: "Calculating...")
                                HashDetailItem("SHA512", sha512 ?: "Calculating...")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(text = value)
    }
}

@Composable
fun HashDetailItem(label: String, value: String) {
    Column {
        Text(text = label, fontWeight = FontWeight.SemiBold)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}
