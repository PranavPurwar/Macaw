package dev.pranav.macaw.ui.home

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Clear
import androidx.compose.material.icons.twotone.ContentPaste
import androidx.compose.material.icons.twotone.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import dev.pranav.macaw.model.CloneAction
import dev.pranav.macaw.model.CompressAction
import dev.pranav.macaw.model.DeleteAction
import dev.pranav.macaw.model.ExtractAction
import dev.pranav.macaw.model.FileAction
import dev.pranav.macaw.model.RenameAction
import dev.pranav.macaw.model.TabData
import dev.pranav.macaw.service.ActionManager
import dev.pranav.macaw.ui.actions.ActionProgressDialog
import dev.pranav.macaw.ui.actions.ActionState
import dev.pranav.macaw.ui.dialogs.ConflictDialog
import dev.pranav.macaw.ui.editor.TextEditorActivity
import dev.pranav.macaw.ui.file.DetailsSheet
import dev.pranav.macaw.ui.file.FileItem
import dev.pranav.macaw.ui.file.FilePreviewState
import dev.pranav.macaw.ui.file.actions.FileActionBottomSheet
import dev.pranav.macaw.ui.file.actions.MultiFileActionBottomSheet
import dev.pranav.macaw.ui.file.actions.RenameDialog
import dev.pranav.macaw.ui.file.handleFileClick
import dev.pranav.macaw.ui.file.preview.ApkBottomSheet
import dev.pranav.macaw.ui.file.preview.AudioPreviewDialog
import dev.pranav.macaw.util.BookmarksManager
import dev.pranav.macaw.util.Clipboard
import dev.pranav.macaw.util.ConflictInfo
import dev.pranav.macaw.util.ConflictResolution
import dev.pranav.macaw.util.orderedChildren
import dev.pranav.macaw.util.sortFiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.coroutines.resume
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.isReadable
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension

private sealed class DialogState {
    object None : DialogState()
    data class BottomSheet(val path: Path) : DialogState()
    data class Rename(val path: Path) : DialogState()
    data class Details(val path: Path) : DialogState()
    data class Apk(val path: Path) : DialogState()
    data class Extract(val path: Path) : DialogState()
    data class Conflict(
        val conflictInfo: ConflictInfo,
        val onAction: (action: ConflictResolution, applyToAll: Boolean) -> Unit
    ) : DialogState()
}

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
fun HomeScreen(
    modifier: Modifier,
    tabData: TabData,
    onDirectoryChange: (Path) -> Unit,
    isCurrent: Boolean
) {
    val files = remember { tabData.files }
    val errorState = remember { mutableStateOf(false) }
    val errorMessage = remember { mutableStateOf("") }
    val context = LocalContext.current
    var loading by remember { mutableStateOf(tabData.isLoading) }
    var isRefreshing by remember { mutableStateOf(false) }
    var filePreviewState by remember { mutableStateOf<FilePreviewState>(FilePreviewState.None) }
    val coroutineScope = rememberCoroutineScope()

    val sheetState = rememberModalBottomSheetState()
    var dialogState by remember { mutableStateOf<DialogState>(DialogState.None) }

    var selectionMode by remember { mutableStateOf(false) }
    val selectedPaths = remember { mutableStateListOf<Path>() }
    var showMultiSelectAction by remember { mutableStateOf(false) }

    suspend fun loadFiles(firstLoad: Boolean = false) {
        withContext(Dispatchers.Main) {
            if (!firstLoad) loading = true
            tabData.isLoading = true
        }
        try {
            val children = withContext(Dispatchers.IO) {
                tabData.currentPath.orderedChildren(tabData.sortOrder)
            }
            withContext(Dispatchers.Main) {
                files.clear()
                files.addAll(children)
                tabData.loadedPath = tabData.currentPath.absolutePathString()
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

    suspend fun refreshFiles() {
        isRefreshing = true
        try {
            val children = withContext(Dispatchers.IO) {
                tabData.currentPath.orderedChildren(tabData.sortOrder)
            }
            withContext(Dispatchers.Main) {
                files.clear()
                files.addAll(children)
                tabData.loadedPath = tabData.currentPath.absolutePathString()
            }
        } catch (e: Exception) {
            errorState.value = true
            errorMessage.value = "Error refreshing files: ${e.message}"
        } finally {
            isRefreshing = false
        }
    }


    val actions by ActionManager.actions.collectAsState()
    val reloadedActionIds = remember { mutableSetOf<Long>() }

    LaunchedEffect(actions.map { it.state.value }) {
        actions.forEach { action ->
            if (action.state.value is ActionState.Completed && action.id !in reloadedActionIds) {
                reloadedActionIds.add(action.id)
                coroutineScope.launch {
                    loadFiles()
                }
            }
        }
    }
    if (isCurrent) {
        ActionProgressDialog()
    }

    fun handleFileAction(context: Context, rename: Path, action: FileAction): Path? {
        var resultPath: Path? = null
        when (action) {
            FileAction.SHARE -> {
                val uri =
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.provider",
                        rename.toFile()
                    )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    type = context.contentResolver.getType(uri)
                    putExtra(Intent.EXTRA_STREAM, uri)
                }
                context.startActivity(Intent.createChooser(intent, "Share File"))
            }

            FileAction.OPEN_WITH -> {
                val uri =
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.provider",
                        rename.toFile()
                    )
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    setDataAndType(uri, context.contentResolver.getType(uri))
                }
                context.startActivity(Intent.createChooser(intent, "Open File"))
            }

            FileAction.DELETE -> {
                ActionManager.addAction(
                    DeleteAction(
                        id = System.currentTimeMillis(),
                        files = listOf(rename)
                    )
                )
            }

            FileAction.OPEN_TEXT_EDITOR -> {
                val intent = Intent(context, TextEditorActivity::class.java).apply {
                    putExtra("file", rename.absolutePathString())
                }
                context.startActivity(intent)
            }

            FileAction.COMPRESS -> {
                val destination = rename.parent.resolve("${rename.nameWithoutExtension}.zip")
                    ?: Paths.get(rename.toAbsolutePath().toString() + ".zip")
                ActionManager.addAction(
                    CompressAction(
                        id = System.currentTimeMillis(),
                        files = listOf(rename),
                        destination = destination
                    )
                )
                resultPath = destination
            }

            FileAction.EXTRACT -> {
                dialogState = DialogState.Extract(rename)
            }

            FileAction.CLONE -> {
                ActionManager.addAction(
                    CloneAction(
                        id = System.currentTimeMillis(),
                        path = rename,
                        onConflict = { conflictInfo ->
                            suspendCancellableCoroutine { continuation ->
                                dialogState = DialogState.Conflict(
                                    conflictInfo = conflictInfo,
                                    onAction = { resolution, applyToAll ->
                                        val finalResolution = if (applyToAll) {
                                            when (resolution) {
                                                ConflictResolution.SKIP -> ConflictResolution.SKIP_ALL
                                                ConflictResolution.OVERWRITE -> ConflictResolution.OVERWRITE_ALL
                                                else -> resolution
                                            }
                                        } else {
                                            resolution
                                        }
                                        continuation.resume(finalResolution)
                                    }
                                )
                            }
                        }
                    )
                )
            }

            FileAction.CUT -> {
                Clipboard.cut(rename)
            }

            FileAction.COPY -> {
                Clipboard.copy(rename)
            }

            FileAction.PASTE -> {
                Clipboard.paste(
                    destination = tabData.currentPath,
                    onConflict = { conflictInfo ->
                        suspendCancellableCoroutine { continuation ->
                            dialogState = DialogState.Conflict(
                                conflictInfo = conflictInfo,
                                onAction = { resolution, applyToAll ->
                                    val finalResolution = if (applyToAll) {
                                        when (resolution) {
                                            ConflictResolution.SKIP -> ConflictResolution.SKIP_ALL
                                            ConflictResolution.OVERWRITE -> ConflictResolution.OVERWRITE_ALL
                                            else -> resolution
                                        }
                                    } else {
                                        resolution
                                    }
                                    continuation.resume(finalResolution)
                                }
                            )
                        }
                    }
                )
            }

            FileAction.CLEAR_CLIPBOARD -> {
                Clipboard.clear()
            }

            FileAction.BOOKMARK -> {
                BookmarksManager.addBookmark(context, rename.absolutePathString())
            }

            FileAction.UNBOOKMARK -> {
                BookmarksManager.removeBookmark(context, rename.absolutePathString())
            }

            else -> {
                Toast.makeText(context, "$action not implemented yet", Toast.LENGTH_SHORT)
                    .show()
            }
        }
        return resultPath
    }

    when (val currentDialog = dialogState) {
        is DialogState.Apk -> ApkBottomSheet(
            file = currentDialog.path,
            onDismiss = { dialogState = DialogState.None }
        )

        is DialogState.BottomSheet -> {
            ModalBottomSheet(
                onDismissRequest = { dialogState = DialogState.None },
                sheetState = sheetState,
            ) {
                FileActionBottomSheet(
                    path = currentDialog.path,
                    onAction = { action ->
                        coroutineScope.launch {
                            when (action) {
                                FileAction.RENAME -> dialogState =
                                    DialogState.Rename(currentDialog.path)

                                FileAction.DETAILS -> dialogState =
                                    DialogState.Details(currentDialog.path)

                                else -> {
                                    dialogState = DialogState.None
                                    handleFileAction(context, currentDialog.path, action)
                                }
                            }
                        }
                    },
                    isBookmarked = BookmarksManager.isBookmarked(
                        context,
                        currentDialog.path.absolutePathString()
                    )
                )
            }
        }

        is DialogState.Details -> DetailsSheet(
            file = currentDialog.path,
            onDismiss = { dialogState = DialogState.None }
        )

        is DialogState.Extract -> {
            var extractPath by remember { mutableStateOf(currentDialog.path.nameWithoutExtension) }
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
                        val destination = if (extractPath.isBlank()) {
                            currentDialog.path.parent
                        } else {
                            val destinationFile = Path(extractPath)
                            if (destinationFile.isAbsolute) {
                                destinationFile
                            } else {
                                tabData.currentPath.resolve(extractPath)
                            }
                        }
                        ActionManager.addAction(
                            ExtractAction(
                                id = System.currentTimeMillis(),
                                path = currentDialog.path,
                                destination = destination,
                                onConflict = { conflictInfo ->
                                    suspendCancellableCoroutine { continuation ->
                                        dialogState = DialogState.Conflict(
                                            conflictInfo = conflictInfo,
                                            onAction = { resolution, applyToAll ->
                                                val finalResolution = if (applyToAll) {
                                                    when (resolution) {
                                                        ConflictResolution.SKIP -> ConflictResolution.SKIP_ALL
                                                        ConflictResolution.OVERWRITE -> ConflictResolution.OVERWRITE_ALL
                                                        else -> resolution
                                                    }
                                                } else {
                                                    resolution
                                                }
                                                continuation.resume(finalResolution)
                                            }
                                        )
                                    }
                                }
                            )
                        )
                        dialogState = DialogState.None
                        Toast.makeText(context, "Extraction started", Toast.LENGTH_SHORT).show()
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

        is DialogState.Rename -> RenameDialog(
            path = currentDialog.path,
            onDismiss = { dialogState = DialogState.None },
            onRename = { newName ->
                dialogState = DialogState.None
                ActionManager.addAction(
                    RenameAction(
                        id = System.currentTimeMillis(),
                        path = currentDialog.path,
                        newName = newName
                    )
                )
            }
        )

        is DialogState.Conflict -> {
            ConflictDialog(
                conflictInfo = currentDialog.conflictInfo,
                onResolution = { resolution, applyToAll ->
                    currentDialog.onAction(resolution, applyToAll)
                    dialogState = DialogState.None
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
                                Clipboard.cut(selectedPaths.toList())
                                selectionMode = false
                                selectedPaths.clear()
                            }

                            FileAction.COPY -> {
                                Clipboard.copy(selectedPaths.toList())
                                selectionMode = false
                                selectedPaths.clear()
                            }

                            FileAction.DELETE -> {
                                ActionManager.addAction(
                                    DeleteAction(
                                        id = System.currentTimeMillis(),
                                        files = selectedPaths.toList()
                                    )
                                )
                                selectionMode = false
                                selectedPaths.clear()
                            }

                            FileAction.COMPRESS -> {
                                val destination = tabData.currentPath.resolve("archive.zip")
                                ActionManager.addAction(
                                    CompressAction(
                                        id = System.currentTimeMillis(),
                                        files = selectedPaths.toList(),
                                        destination = destination
                                    )
                                )
                                selectionMode = false
                                selectedPaths.clear()
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
            selectedPaths.clear()
        } else if (tabData.currentPath.parent.isReadable() == true && tabData.currentPath.absolutePathString() != tabData.initialRootDir.absolutePathString()) {
            onDirectoryChange(tabData.currentPath.parent)
        } else {
            errorState.value = true
            errorMessage.value = "Cannot read parent directory"
        }
    }

    LaunchedEffect(tabData.currentPath, tabData.sortOrder) {
        if (tabData.loadedPath != tabData.currentPath.absolutePathString()) {
            loadFiles(true)
        } else {
            val sortedFiles = sortFiles(files, tabData.sortOrder)
            files.clear()
            files.addAll(sortedFiles)
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            if (selectionMode) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedPaths.size} selected",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        selectionMode = false
                                        selectedPaths.clear()
                                    },
                                color = MaterialTheme.colorScheme.error,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    Icons.TwoTone.Clear,
                                    contentDescription = "Clear selection",
                                    modifier = Modifier.padding(8.dp),
                                    tint = MaterialTheme.colorScheme.onError
                                )
                            }
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showMultiSelectAction = true },
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    Icons.TwoTone.MoreVert,
                                    contentDescription = "More actions",
                                    modifier = Modifier.padding(8.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            }

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    coroutineScope.launch {
                        refreshFiles()
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) {
                if (loading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        LoadingIndicator(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(64.dp),
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface),
                        state = tabData.lazyListState
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        items(
                            items = files,
                            key = { it.name }
                        ) { fileInfo ->
                            val isSelected = remember(selectedPaths) {
                                selectedPaths.any { it.absolutePathString() == fileInfo.absolutePathString() }
                            }

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 1.dp),
                                color = when {
                                    isSelected -> MaterialTheme.colorScheme.secondaryContainer
                                    else -> MaterialTheme.colorScheme.surface
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                FileItem(
                                    path = fileInfo,
                                    isSelected = isSelected,
                                    selectionMode = selectionMode,
                                    onFileClick = { clickedFile ->
                                        if (selectionMode) {
                                            if (isSelected) {
                                                selectedPaths.remove(clickedFile)
                                            } else {
                                                selectedPaths.add(clickedFile)
                                            }
                                            if (selectedPaths.isEmpty()) {
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
                                            selectedPaths.add(it)
                                        }
                                    },
                                    onMoreClick = {
                                        dialogState = DialogState.BottomSheet(it)
                                    }
                                )
                            }
                        }
                        item {
                            Spacer(modifier = Modifier.height(100.dp))
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
                ExtendedFloatingActionButton(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    onClick = {
                        Clipboard.paste(
                            destination = tabData.currentPath,
                            onConflict = { conflictInfo ->
                                suspendCancellableCoroutine { continuation ->
                                    dialogState = DialogState.Conflict(
                                        conflictInfo = conflictInfo,
                                        onAction = { resolution, applyToAll ->
                                            val finalResolution = if (applyToAll) {
                                                when (resolution) {
                                                    ConflictResolution.SKIP -> ConflictResolution.SKIP_ALL
                                                    ConflictResolution.OVERWRITE -> ConflictResolution.OVERWRITE_ALL
                                                    else -> resolution
                                                }
                                            } else {
                                                resolution
                                            }
                                            continuation.resume(finalResolution)
                                        }
                                    )
                                }
                            }
                        )
                    },
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 12.dp
                    )
                ) {
                    Icon(
                        Icons.TwoTone.ContentPaste,
                        contentDescription = "Paste",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Paste")
                }
            }
        }
    }
}
