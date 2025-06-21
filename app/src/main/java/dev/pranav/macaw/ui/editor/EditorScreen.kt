package dev.pranav.macaw.ui.editor

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.rounded.WrapText
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EditOff
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import dev.pranav.macaw.App
import dev.pranav.macaw.util.getLastModifiedDate
import dev.pranav.macaw.util.sizeString
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.component.Magnifier
import io.github.rosemoe.sora.widget.subscribeAlways
import me.saket.cascade.CascadeDropdownMenu
import me.saket.cascade.rememberCascadeState
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.time.ExperimentalTime

@Composable
fun EditorScreen(file: File, editor: CodeEditor, onSave: () -> Unit) {
    Scaffold(
        topBar = {
            Column {
                EditorToolbar(
                    file = file,
                    editor = editor,
                    onSave = onSave
                )
                HorizontalDivider(thickness = 1.dp)
            }
        }
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
            TextEditorContent(editor)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalTime::class
)
@Composable
fun EditorToolbar(file: File, editor: CodeEditor, onSave: () -> Unit) {
    var fileSize by remember { mutableStateOf(file.sizeString()) }
    var showMenu by remember { mutableStateOf(false) }
    var showGoToLineDialog by remember { mutableStateOf(false) }
    val state = rememberCascadeState()
    val itemPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
    val prefs = App.prefs

    var useICULibToSelectWords by remember { mutableStateOf(prefs.useICULibToSelectWords) }
    var showLineNumber by remember { mutableStateOf(prefs.showLineNumber) }
    var pinLineNumber by remember { mutableStateOf(prefs.pinLineNumber) }
    var symbolPairAutoCompletion by remember { mutableStateOf(prefs.symbolPairAutoCompletion) }
    var deleteEmptyLineFast by remember { mutableStateOf(prefs.deleteEmptyLineFast) }
    var enableMagnifier by remember { mutableStateOf(prefs.enableMagnifier) }
    var wordWrap by remember { mutableStateOf(prefs.wordWrap) }
    var readOnly by remember { mutableStateOf(false) }
    var smoothMode by remember { mutableStateOf(false) }

    val undoManager = editor.text.undoManager

    var canUndo by remember { mutableStateOf(undoManager.canUndo()) }
    var canRedo by remember { mutableStateOf(undoManager.canRedo()) }
    var savedContentHash by remember { mutableStateOf(file.readBytes().contentHashCode()) }
    var editorContentHash by remember { mutableIntStateOf(savedContentHash) }

    editor.subscribeAlways<ContentChangeEvent> { event ->
        canUndo = undoManager.canUndo()
        canRedo = undoManager.canRedo()

        editorContentHash = editor.text.toString().toByteArray().contentHashCode()
    }

    fileSize = if (savedContentHash != editorContentHash) {
        file.sizeString() + " | " + LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("MMM dd, hh:mm a")) + " *"
    } else {
        file.sizeString() + " | " + file.getLastModifiedDate("MMM dd, hh:mm a")
    }

    TopAppBar(
        title = {
            Column {
                Text(
                    file.name,
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    fileSize,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        actions = {
            IconButton(onClick = {
                editor.undo()
                canUndo = undoManager.canUndo()
                canRedo = undoManager.canRedo()
                editorContentHash = editor.text.toString().toByteArray().contentHashCode()
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    tint = if (canUndo) MaterialTheme.colorScheme.onSurface else Color.Gray,
                    contentDescription = "Undo"
                )
            }
            IconButton(onClick = {
                editor.redo()
                canUndo = undoManager.canUndo()
                canRedo = undoManager.canRedo()

                editorContentHash = editor.text.toString().toByteArray().contentHashCode()
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Redo,
                    tint = if (canRedo) MaterialTheme.colorScheme.onSurface else Color.Gray,
                    contentDescription = "Redo"
                )
            }
            IconButton(
                onClick = {
                    onSave()
                    savedContentHash = file.readBytes().contentHashCode()
                },
                enabled = savedContentHash != editorContentHash
            ) {
                Icon(
                    imageVector = Icons.Filled.Save,
                    tint = if (savedContentHash != editorContentHash) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    contentDescription = "Save File"
                )
            }
            Box {
                IconButton(onClick = { showMenu = !showMenu }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Editor Options"
                    )
                }
                CascadeDropdownMenu(
                    state = state,
                    fixedWidth = 250.dp,
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    DropdownMenuItem(
                        text = { Text("Go to Line") },
                        onClick = {
                            showGoToLineDialog = true
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.Timeline, null) },
                        contentPadding = itemPadding
                    )
                    DropdownMenuItem(
                        text = { Text("Toggle Word Wrap") },
                        onClick = {
                            wordWrap = !wordWrap
                            prefs.wordWrap = wordWrap
                            editor.isWordwrap = wordWrap
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.AutoMirrored.Rounded.WrapText, null) },
                        contentPadding = itemPadding
                    )
                    DropdownMenuItem(
                        text = { Text("Read Only Mode") },
                        onClick = {
                            editor.isBasicDisplayMode = !editor.isBasicDisplayMode
                            readOnly = !readOnly
                        },
                        leadingIcon = { Icon(if (readOnly) Icons.Rounded.EditOff else Icons.Rounded.Edit, null) },
                        trailingIcon = {
                            Checkbox(
                                checked = readOnly,
                                onCheckedChange = {
                                    readOnly = it
                                    editor.isBasicDisplayMode = !it
                                }
                            )
                        },
                        contentPadding = itemPadding
                    )
                    DropdownMenuItem(
                        text = { Text("Smooth Mode") },
                        onClick = {
                            smoothMode = !smoothMode
                            editor.isBasicDisplayMode = smoothMode
                        },
                        leadingIcon = { Icon(if (readOnly) Icons.Rounded.EditOff else Icons.Rounded.Edit, null) },
                        trailingIcon = {
                            Checkbox(
                                checked = readOnly,
                                onCheckedChange = {
                                    readOnly = it
                                    editor.isBasicDisplayMode = !it
                                }
                            )
                        },
                        contentPadding = itemPadding
                    )
                    DropdownMenuItem(
                        text = { Text("More") },
                        contentPadding = itemPadding,
                        children = {
                            DropdownMenuItem(
                                text = { Text(text = "Use ICU Selection") },
                                onClick = {
                                    useICULibToSelectWords = !useICULibToSelectWords
                                    prefs.useICULibToSelectWords = useICULibToSelectWords
                                    editor.props.useICULibToSelectWords = useICULibToSelectWords
                                },
                                leadingIcon = { Icon(Icons.Rounded.TextFields, null) },
                                trailingIcon = {
                                    Checkbox(
                                        checked = useICULibToSelectWords,
                                        onCheckedChange = {
                                            useICULibToSelectWords = it
                                            prefs.useICULibToSelectWords = it
                                            editor.props.useICULibToSelectWords = it
                                        }
                                    )
                                },
                                contentPadding = itemPadding
                            )
                            DropdownMenuItem(
                                text = { Text(text = "Show Line Numbers") },
                                onClick = {
                                    showLineNumber = !showLineNumber
                                    prefs.showLineNumber = pinLineNumber
                                    editor.isDisplayLnPanel = showLineNumber
                                },
                                leadingIcon = { Icon(Icons.Rounded.PushPin, null) },
                                trailingIcon = {
                                    Checkbox(
                                        checked = pinLineNumber,
                                        onCheckedChange = {
                                            pinLineNumber = it
                                            prefs.pinLineNumber = it
                                            editor.setPinLineNumber(it)
                                        }
                                    )
                                },
                                contentPadding = itemPadding
                            )
                            DropdownMenuItem(
                                text = { Text(text = "Pin Line Number") },
                                onClick = {
                                    pinLineNumber = !pinLineNumber
                                    prefs.pinLineNumber = pinLineNumber
                                    editor.setPinLineNumber(pinLineNumber)
                                },
                                leadingIcon = { Icon(Icons.Rounded.PushPin, null) },
                                trailingIcon = {
                                    Checkbox(
                                        checked = pinLineNumber,
                                        onCheckedChange = {
                                            pinLineNumber = it
                                            prefs.pinLineNumber = it
                                            editor.setPinLineNumber(it)
                                        }
                                    )
                                },
                                contentPadding = itemPadding
                            )
                            DropdownMenuItem(
                                text = { Text(text = "Match Symbol Pair") },
                                onClick = {
                                    symbolPairAutoCompletion = !symbolPairAutoCompletion
                                    prefs.symbolPairAutoCompletion = symbolPairAutoCompletion
                                    editor.props.symbolPairAutoCompletion = symbolPairAutoCompletion
                                },
                                leadingIcon = { Icon(Icons.Rounded.Code, null) },
                                trailingIcon = {
                                    Checkbox(
                                        checked = symbolPairAutoCompletion,
                                        onCheckedChange = {
                                            symbolPairAutoCompletion = it
                                            prefs.symbolPairAutoCompletion = it
                                            editor.props.symbolPairAutoCompletion = it
                                        }
                                    )
                                },
                                contentPadding = itemPadding
                            )
                            DropdownMenuItem(
                                text = { Text(text = "Delete Empty Space") },
                                onClick = {
                                    deleteEmptyLineFast = !deleteEmptyLineFast
                                    prefs.deleteEmptyLineFast = deleteEmptyLineFast
                                    editor.props.deleteEmptyLineFast = deleteEmptyLineFast
                                    editor.props.deleteMultiSpaces = if (deleteEmptyLineFast) -1 else 1
                                },
                                leadingIcon = { Icon(Icons.Default.DeleteSweep, null) },
                                trailingIcon = {
                                    Checkbox(
                                        checked = deleteEmptyLineFast,
                                        onCheckedChange = {
                                            deleteEmptyLineFast = it
                                            prefs.deleteEmptyLineFast = it
                                            editor.props.deleteEmptyLineFast = it
                                            editor.props.deleteMultiSpaces = if (it) -1 else 1
                                        }
                                    )
                                },
                                contentPadding = itemPadding
                            )
                            DropdownMenuItem(
                                text = { Text(text = "Selection Magnifier") },
                                onClick = {
                                    enableMagnifier = !enableMagnifier
                                    prefs.enableMagnifier = enableMagnifier
                                    editor.getComponent(Magnifier::class.java).isEnabled =
                                        enableMagnifier
                                },
                                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                                trailingIcon = {
                                    Checkbox(
                                        checked = enableMagnifier,
                                        onCheckedChange = {
                                            enableMagnifier = it
                                            prefs.enableMagnifier = it
                                            editor.getComponent(Magnifier::class.java).isEnabled =
                                                it
                                        }
                                    )
                                },
                                contentPadding = itemPadding
                            )
                        }
                    )
                }
            }
        }
    )

    if (showGoToLineDialog) {
        GoToLineDialog(
            lineCount = editor.lineCount,
            onDismiss = { showGoToLineDialog = false },
            onConfirm = { lineNumber ->
                editor.jumpToLine(lineNumber - 1)
                showGoToLineDialog = false
            }
        )
    }
}

@Composable
fun GoToLineDialog(lineCount: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var text by remember { mutableStateOf("") }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Go to Line") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter { char -> char.isDigit() } },
                label = { Text("Line number") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val lineNumber = text.toIntOrNull()
                    if (lineNumber != null && lineNumber in 1..lineCount) {
                        onConfirm(lineNumber)
                    } else {
                        Toast.makeText(
                            context,
                            "Invalid line number. Must be between 1 and $lineCount.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            ) {
                Text("Go")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Statistics(content: Content, onDismiss: () -> Unit) {
    val bytes = content.toString().toByteArray().size
    val charCount = content.length

    AlertDialog(
        onDismissRequest = { onDismiss() },
        modifier = Modifier.fillMaxWidth(0.8f),
        title = { Text("Statistics") },
        text = {
            Column {
                Text("Byte Count: $bytes")
                Text("Character Count: $charCount")
                Text("Word Count: ${content.split(" ").size}")
                Text("Line Count: ${content.lineCount}")
            }
        },
        confirmButton = {
            TextButton(onDismiss) {
                Text("Dismiss")
            }
        },
        dismissButton = {},
        properties = DialogProperties(usePlatformDefaultWidth = false)
    )
}

@Composable
private fun TextEditorContent(editor: CodeEditor) {
    var charset by remember { mutableStateOf("UTF-8") }
    var position by remember { mutableStateOf("1:1") }
    var statsDialogShown by remember { mutableStateOf(false) }

    editor.subscribeAlways<SelectionChangeEvent> { event ->
        position = event.left.let {
            "${it.line + 1}:${it.column}"
        }
    }
    Column {
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = position,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontFamily = FontFamily.Monospace
            )

            Text(
                modifier = Modifier
                    .weight(1f)
                    .clickable(true) {
                        statsDialogShown = true
                    },
                text = charset,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.End
            )
        }
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { editor },
            onRelease = {
                editor.release()
            }
        )
    }

    if (statsDialogShown) {
        Statistics(editor.text) {
            statsDialogShown = false
        }
    }
}
