package dev.pranav.macaw.ui.home

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Clear
import androidx.compose.material.icons.twotone.ContentPaste
import androidx.compose.material.icons.twotone.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import dev.pranav.macaw.model.FileAction
import dev.pranav.macaw.model.FileInfo
import dev.pranav.macaw.model.TabData
import dev.pranav.macaw.ui.editor.TextEditorActivity
import dev.pranav.macaw.ui.preview.ApkBottomSheet
import dev.pranav.macaw.ui.preview.AudioPreviewDialog
import dev.pranav.macaw.util.BookmarksManager
import dev.pranav.macaw.util.Clipboard
import dev.pranav.macaw.util.ConflictAction
import dev.pranav.macaw.util.clone
import dev.pranav.macaw.util.compress
import dev.pranav.macaw.util.deleteFile
import dev.pranav.macaw.util.details
import dev.pranav.macaw.util.extract
import dev.pranav.macaw.util.orderedChildren
import dev.pranav.macaw.util.rename
import dev.pranav.macaw.util.sortFiles
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private sealed class DialogState {
    object None : DialogState()
    data class BottomSheet(val file: File) : DialogState()
    data class Rename(val file: File) : DialogState()
    data class Details(val file: File) : DialogState()
    data class Apk(val file: File) : DialogState()
    data class Extract(val file: File) : DialogState()
    data class Progress(
        val title: String,
        val message: String = "",
        val progress: Float? = null
    ) : DialogState()

    data class Conflict(
        val file: File,
        val onAction: (action: ConflictAction, applyToAll: Boolean) -> Unit
    ) : DialogState()
}

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
fun HomeScreen(
    modifier: Modifier,
    tabData: TabData,
    onDirectoryChange: (File) -> Unit,
    isCurrent: Boolean
) {
    val files = remember { tabData.files }
    val errorState = remember { mutableStateOf(false) }
    val errorMessage = remember { mutableStateOf("") }
    val context = LocalContext.current
    var loading by remember { mutableStateOf(tabData.isLoading) }
    var filePreviewState by remember { mutableStateOf<FilePreviewState>(FilePreviewState.None) }
    val coroutineScope = rememberCoroutineScope()

    val sheetState = rememberModalBottomSheetState()
    var dialogState by remember { mutableStateOf<DialogState>(DialogState.None) }

    var selectionMode by remember { mutableStateOf(false) }
    val selectedFiles = remember { mutableStateListOf<File>() }
    var showMultiSelectAction by remember { mutableStateOf(false) }

    suspend fun loadFiles(firstLoad: Boolean = false) {
        withContext(Dispatchers.Main) {
            if (!firstLoad) loading = true
            tabData.isLoading = true
        }
        try {
            val children = withContext(Dispatchers.IO) {
                tabData.currentPath.orderedChildren(tabData.sortOrder).map { file ->
                    FileInfo(file, file.details())
                }
            }
            withContext(Dispatchers.Main) {
                files.clear()
                files.addAll(children)
                tabData.loadedPath = tabData.currentPath.absolutePath
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                errorState.value = true
                errorMessage.value = "Error loading files: ${e.message}"
            }
        } finally {
            withContext(Dispatchers.Main) {
                if (!firstLoad) loading = false
                tabData.isLoading = false
            }
        }
    }

    suspend fun handleFileAction(context: Context, file: File, action: FileAction): File? =
        withContext(Dispatchers.IO) {
            var resultFile: File? = null
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
                    withContext(Dispatchers.Main) {
                        dialogState = DialogState.Progress("Cloning")
                    }
                    file.clone()
                    withContext(Dispatchers.Main) {
                        dialogState = DialogState.None
                        Toast.makeText(context, "File cloned", Toast.LENGTH_SHORT).show()
                    }
                }

                FileAction.DELETE -> {
                    withContext(Dispatchers.Main) {
                        dialogState = DialogState.Progress("Deleting")
                    }
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
                    withContext(Dispatchers.Main) {
                        dialogState = DialogState.None
                    }
                }

                FileAction.OPEN_TEXT_EDITOR -> {
                    val intent = Intent(context, TextEditorActivity::class.java).apply {
                        putExtra("file", file)
                    }
                    context.startActivity(intent)
                }

                FileAction.COMPRESS -> {
                    val destination = File(file.parentFile, "${file.nameWithoutExtension}.zip")
                    withContext(Dispatchers.Main) {
                        dialogState = DialogState.Progress("Compressing")
                    }
                    file.compress(destination) { entryName, progress ->
                        coroutineScope.launch(Dispatchers.Main) {
                            dialogState =
                                DialogState.Progress("Compressing", entryName, progress)
                        }
                    }
                    withContext(Dispatchers.Main) {
                        dialogState = DialogState.None
                    }
                    resultFile = destination
                }

                FileAction.EXTRACT -> {
                    withContext(Dispatchers.Main) {
                        dialogState = DialogState.Extract(file)
                    }
                }

                FileAction.CUT -> {
                    Clipboard.cut(file)
                }

                FileAction.COPY -> {
                    Clipboard.copy(file)
                }

                FileAction.PASTE -> {
                    withContext(Dispatchers.Main) {
                        dialogState = DialogState.Progress("Pasting")
                    }
                    Clipboard.paste(tabData.currentPath)
                    withContext(Dispatchers.Main) {
                        dialogState = DialogState.None
                    }
                }

                FileAction.CLEAR_CLIPBOARD -> {
                    Clipboard.clear()
                }

                FileAction.BOOKMARK -> {
                    BookmarksManager.addBookmark(context, file.absolutePath)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Bookmarked", Toast.LENGTH_SHORT).show()
                    }
                }

                FileAction.UNBOOKMARK -> {
                    BookmarksManager.removeBookmark(context, file.absolutePath)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Bookmark removed", Toast.LENGTH_SHORT).show()
                    }
                }

                else -> {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "$action not implemented yet", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
            resultFile
        }

    when (val currentDialog = dialogState) {
        is DialogState.Apk -> ApkBottomSheet(
            file = currentDialog.file,
            onDismiss = { dialogState = DialogState.None }
        )

        is DialogState.BottomSheet -> {
            ModalBottomSheet(
                onDismissRequest = { dialogState = DialogState.None },
                sheetState = sheetState,
            ) {
                FileActionBottomSheet(
                    file = currentDialog.file,
                    onAction = { action ->
                        coroutineScope.launch {
                            when (action) {
                                FileAction.RENAME -> dialogState =
                                    DialogState.Rename(currentDialog.file)

                                FileAction.DETAILS -> dialogState =
                                    DialogState.Details(currentDialog.file)

                                else -> {
                                    dialogState = DialogState.None
                                    val newFile =
                                        handleFileAction(context, currentDialog.file, action)
                                    loadFiles()
                                    newFile?.let {
                                        val index =
                                            files.indexOfFirst { f -> f.file.absolutePath == it.absolutePath }
                                        if (index != -1) {
                                            tabData.lazyListState.animateScrollToItem(index)
                                        }
                                    }
                                }
                            }
                        }
                    },
                    isBookmarked = BookmarksManager.isBookmarked(
                        context,
                        currentDialog.file.absolutePath
                    )
                )
            }
        }

        is DialogState.Details -> DetailsSheet(
            file = currentDialog.file,
            onDismiss = { dialogState = DialogState.None }
        )

        is DialogState.Rename -> RenameDialog(
            file = currentDialog.file,
            onDismiss = { dialogState = DialogState.None },
            onRename = { newName ->
                coroutineScope.launch {
                    withContext(Dispatchers.IO) {
                        currentDialog.file.rename(newName)
                    }
                    loadFiles()
                }
            }
        )

        is DialogState.Extract -> {
            var extractPath by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { dialogState = DialogState.None },
                title = { Text("Extract") },
                text = {
                    Column {
                        Text("Extract to current directory or specify a path")
                        OutlinedTextField(
                            value = extractPath,
                            onValueChange = { extractPath = it },
                            label = { Text("Extraction path (optional)") },
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        dialogState = DialogState.None
                        coroutineScope.launch(Dispatchers.IO) {
                            val destination = if (extractPath.isBlank()) {
                                currentDialog.file.parentFile!!
                            } else {
                                File(extractPath)
                            }
                            val finalDestination =
                                File(destination, currentDialog.file.nameWithoutExtension)

                            var conflictAction: ConflictAction? = null
                            var applyToAll = false

                            withContext(Dispatchers.Main) {
                                dialogState = DialogState.Progress("Extracting")
                            }
                            currentDialog.file.extract(
                                destinationDir = finalDestination,
                                onProgress = { entryName ->
                                    coroutineScope.launch(Dispatchers.Main) {
                                        dialogState =
                                            DialogState.Progress("Extracting", entryName)
                                    }
                                },
                                onConflict = { conflictingFile ->
                                    if (applyToAll && conflictAction != null) {
                                        return@extract conflictAction!!
                                    }
                                    val result =
                                        CompletableDeferred<Pair<ConflictAction, Boolean>>()
                                    coroutineScope.launch(Dispatchers.Main) {
                                        dialogState =
                                            DialogState.Conflict(conflictingFile) { action, all ->
                                                result.complete(action to all)
                                            }
                                    }
                                    val (chosenAction, shouldApplyToAll) = result.await()
                                    if (shouldApplyToAll) {
                                        applyToAll = true
                                        conflictAction = chosenAction
                                    }
                                    return@extract chosenAction
                                }
                            )
                            withContext(Dispatchers.Main) {
                                dialogState = DialogState.None
                            }
                            loadFiles()
                        }
                    }) {
                        Text("Extract")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { dialogState = DialogState.None }) {
                        Text("Cancel")
                    }
                }
            )
        }

        is DialogState.Progress -> {
            AlertDialog(
                onDismissRequest = { /* Not dismissable */ },
                title = { Text(currentDialog.title) },
                text = {
                    Column(Modifier.fillMaxWidth()) {
                        Text(
                            currentDialog.message,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        if (currentDialog.progress == null) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        } else {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                progress = { currentDialog.progress }
                            )
                        }
                    }
                },
                confirmButton = {}
            )
        }

        is DialogState.Conflict -> {
            var applyToAll by remember { mutableStateOf(false) }
            AlertDialog(
                onDismissRequest = { /* Not dismissable */ },
                title = { Text("Conflict") },
                text = {
                    Column {
                        Text("File already exists: ${currentDialog.file.name}")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = applyToAll, onCheckedChange = { applyToAll = it })
                            Text("Apply to all subsequent conflicts")
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        currentDialog.onAction(
                            ConflictAction.OVERWRITE,
                            applyToAll
                        )
                    }) {
                        Text("Overwrite")
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = {
                            currentDialog.onAction(
                                ConflictAction.SKIP,
                                applyToAll
                            )
                        }) {
                            Text("Skip")
                        }
                        TextButton(onClick = {
                            currentDialog.onAction(
                                ConflictAction.ABORT,
                                applyToAll
                            )
                        }) {
                            Text("Cancel")
                        }
                    }
                }
            )
        }

        DialogState.None -> {}
    }

    if (showMultiSelectAction) {
        ModalBottomSheet(
            onDismissRequest = { showMultiSelectAction = false },
            sheetState = sheetState,
        ) {
            MultiFileActionBottomSheet(
                onAction = { action ->
                    coroutineScope.launch {
                        when (action) {
                            FileAction.CUT -> {
                                Clipboard.cut(selectedFiles)
                                selectionMode = false
                                selectedFiles.clear()
                            }

                            FileAction.COPY -> {
                                Clipboard.copy(selectedFiles)
                                selectionMode = false
                                selectedFiles.clear()
                            }

                            FileAction.DELETE -> {
                                dialogState = DialogState.Progress("Deleting")
                                withContext(Dispatchers.IO) {
                                    selectedFiles.forEach { it.deleteFile() }
                                }
                                dialogState = DialogState.None
                                loadFiles()
                                selectionMode = false
                                selectedFiles.clear()
                            }

                            FileAction.COMPRESS -> {
                                val destination = File(
                                    tabData.currentPath,
                                    "${selectedFiles.first().nameWithoutExtension}.zip"
                                )
                                withContext(Dispatchers.Main) {
                                    dialogState = DialogState.Progress("Compressing")
                                }
                                withContext(Dispatchers.IO) {
                                    selectedFiles.compress(destination) { entryName, progress ->
                                        coroutineScope.launch(Dispatchers.Main) {
                                            dialogState =
                                                DialogState.Progress(
                                                    "Compressing",
                                                    entryName,
                                                    progress
                                                )
                                        }
                                    }
                                }
                                withContext(Dispatchers.Main) {
                                    dialogState = DialogState.None
                                }
                                loadFiles()
                            }

                            else -> {}
                        }
                        showMultiSelectAction = false
                    }
                }
            )
        }
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
        if (selectionMode) {
            selectionMode = false
            selectedFiles.clear()
        } else if (tabData.currentPath.parentFile?.canRead() == true && tabData.currentPath.absolutePath != tabData.initialRootDir.absolutePath) {
            onDirectoryChange(tabData.currentPath.parentFile!!)
        } else {
            errorState.value = true
            errorMessage.value = "Cannot read parent directory"
        }
    }

    LaunchedEffect(tabData.currentPath, tabData.sortOrder) {
        if (tabData.loadedPath != tabData.currentPath.absolutePath) {
            loadFiles(true)
        } else {
            val sortedFiles = sortFiles(files, tabData.sortOrder)
            files.clear()
            files.addAll(sortedFiles)
        }
    }

    Box(modifier = modifier) {
        Column {
            if (selectionMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${selectedFiles.size} selected")
                    Row {
                        Icon(
                            Icons.TwoTone.MoreVert,
                            contentDescription = "More actions",
                            modifier = Modifier.clickable { showMultiSelectAction = true }
                        )
                        Icon(
                            Icons.TwoTone.Clear,
                            contentDescription = "Clear selection",
                            modifier = Modifier.clickable {
                                selectionMode = false
                                selectedFiles.clear()
                            }
                        )
                    }
                }
            }
            PullToRefreshBox(
                isRefreshing = loading,
                onRefresh = {
                    coroutineScope.launch {
                        loadFiles()
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) {
                if (loading) {
                    Box(Modifier.fillMaxSize()) {
                        LoadingIndicator(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(48.dp),
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = tabData.lazyListState
                    ) {
                        items(
                            items = files,
                            key = { it.file.absolutePath }
                        ) { fileInfo ->
                            val isSelected = selectedFiles.contains(fileInfo.file)
                            FileItem(
                                fileInfo = fileInfo,
                                isSelected = isSelected,
                                selectionMode = selectionMode,
                                onFileClick = { clickedFile ->
                                    if (selectionMode) {
                                        if (isSelected) {
                                            selectedFiles.remove(clickedFile)
                                        } else {
                                            selectedFiles.add(clickedFile)
                                        }
                                        if (selectedFiles.isEmpty()) {
                                            selectionMode = false
                                        }
                                    } else {
                                        handleFileClick(
                                            file = clickedFile,
                                            context = context,
                                            onDirectoryChange = onDirectoryChange,
                                            onShowApkBottomSheet = {
                                                dialogState = DialogState.Apk(it)
                                            },
                                            onShowAudioPreview = {
                                                filePreviewState = FilePreviewState.Audio(it)
                                            },
                                            onError = {
                                                errorMessage.value = it
                                                errorState.value = true
                                            }
                                        )
                                    }
                                },
                                onFileLongClick = {
                                    if (!selectionMode) {
                                        selectionMode = true
                                        selectedFiles.add(it)
                                    }
                                },
                                onMoreClick = {
                                    dialogState = DialogState.BottomSheet(it)
                                }
                            )
                        }
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
                            withContext(Dispatchers.IO) {
                                Clipboard.paste(tabData.currentPath)
                            }
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
