package dev.pranav.filemanager.ui.editor

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.automirrored.rounded.FormatIndentIncrease
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Numbers
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SpaceBar
import androidx.compose.material.icons.rounded.TextRotationNone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import dev.pranav.filemanager.util.sizeString
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.component.Magnifier
import me.saket.cascade.CascadeDropdownMenu
import me.saket.cascade.rememberCascadeState
import java.io.File

@Composable
fun EditorScreen(file: File, editor: CodeEditor, onSave: () -> Unit) {
    Scaffold(
        topBar = {
            EditorToolbar(
                file = file,
                editor = editor,
                onSave = onSave
            )
        }
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
            TextEditorContent(editor)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorToolbar(file: File, editor: CodeEditor, onSave: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    var showGoToLineDialog by remember { mutableStateOf(false) }
    val state = rememberCascadeState()
    val itemPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)

    var useICULibToSelectWords by remember { mutableStateOf(false) }
    var pinLineNumber by remember { mutableStateOf(false) }
    var symbolPairAutoCompletion by remember { mutableStateOf(true) }
    var deleteEmptyLineFast by remember { mutableStateOf(true) }
    var deleteMultiSpaces by remember { mutableStateOf(false) }
    var autoIndent by remember { mutableStateOf(true) }
    var enableMagnifier by remember { mutableStateOf(false) }
    var readOnly by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Column {
                Text(
                    file.name,
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    file.sizeString(),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        actions = {
            IconButton(onClick = onSave) {
                Icon(
                    imageVector = Icons.Filled.Save,
                    contentDescription = "Save File"
                )
            }
            IconButton(onClick = { editor.undo() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Undo"
                )
            }
            IconButton(onClick = { editor.redo() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Redo,
                    contentDescription = "Redo"
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
                    fixedWidth = 260.dp,
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
                        contentPadding = itemPadding
                    )
                    DropdownMenuItem(
                        text = { Text("Toggle Word Wrap") },
                        onClick = {
                            editor.isWordwrap = !editor.isWordwrap
                            showMenu = false
                        },
                        contentPadding = itemPadding
                    )
                    DropdownMenuItem(
                        text = { Text("Read Only Mode") },
                        onClick = {
                            editor.editable = !editor.editable
                            showMenu = false
                        },
                        trailingIcon = {
                            Checkbox(
                                checked = readOnly,
                                onCheckedChange = {
                                    readOnly = it
                                    editor.editable = !it
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
                                    editor.props.useICULibToSelectWords = useICULibToSelectWords
                                },
                                leadingIcon = { Icon(Icons.Rounded.TextRotationNone, null) },
                                trailingIcon = {
                                    Checkbox(
                                        checked = useICULibToSelectWords,
                                        onCheckedChange = {
                                            useICULibToSelectWords = it
                                            editor.props.useICULibToSelectWords = it
                                        }
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(text = "Pin Line Number") },
                                onClick = {
                                    pinLineNumber = !pinLineNumber
                                    editor.setPinLineNumber(pinLineNumber)
                                },
                                leadingIcon = { Icon(Icons.Rounded.Numbers, null) },
                                trailingIcon = {
                                    Checkbox(
                                        checked = pinLineNumber,
                                        onCheckedChange = {
                                            pinLineNumber = it
                                            editor.setPinLineNumber(it)
                                        }
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(text = "Match Symbol Pair") },
                                onClick = {
                                    symbolPairAutoCompletion = !symbolPairAutoCompletion
                                    editor.props.symbolPairAutoCompletion = symbolPairAutoCompletion
                                },
                                leadingIcon = { Icon(Icons.Rounded.Code, null) },
                                trailingIcon = {
                                    Checkbox(
                                        checked = symbolPairAutoCompletion,
                                        onCheckedChange = {
                                            symbolPairAutoCompletion = it
                                            editor.props.symbolPairAutoCompletion = it
                                        }
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(text = "Delete Empty Lines") },
                                onClick = {
                                    deleteEmptyLineFast = !deleteEmptyLineFast
                                    editor.props.deleteEmptyLineFast = deleteEmptyLineFast
                                },
                                leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Backspace, null) },
                                trailingIcon = {
                                    Checkbox(
                                        checked = deleteEmptyLineFast,
                                        onCheckedChange = {
                                            deleteEmptyLineFast = it
                                            editor.props.deleteEmptyLineFast = it
                                        }
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(text = "Auto Indentation") },
                                onClick = {
                                    autoIndent = !autoIndent
                                    editor.props.autoIndent = autoIndent
                                },
                                leadingIcon = { Icon(Icons.AutoMirrored.Rounded.FormatIndentIncrease, null) },
                                trailingIcon = {
                                    Checkbox(
                                        checked = autoIndent,
                                        onCheckedChange = {
                                            autoIndent = it
                                            editor.props.autoIndent = it
                                        }
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(text = "Delete Tabs") },
                                onClick = {
                                    deleteMultiSpaces = !deleteMultiSpaces
                                    editor.props.deleteMultiSpaces = if (deleteMultiSpaces) -1 else 1
                                },
                                leadingIcon = { Icon(Icons.Rounded.SpaceBar, null) },
                                trailingIcon = {
                                    Checkbox(
                                        checked = deleteMultiSpaces,
                                        onCheckedChange = {
                                            deleteMultiSpaces = it
                                            editor.props.deleteMultiSpaces = if (it) -1 else 1
                                        }
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(text = "Selection Magnifier") },
                                onClick = {
                                    enableMagnifier = !enableMagnifier
                                    editor.getComponent(Magnifier::class.java).isEnabled =
                                        enableMagnifier
                                },
                                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                                trailingIcon = {
                                    Checkbox(
                                        checked = enableMagnifier,
                                        onCheckedChange = {
                                            enableMagnifier = it
                                            editor.getComponent(Magnifier::class.java).isEnabled =
                                                it
                                        }
                                    )
                                }
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

@Composable
private fun TextEditorContent(editor: CodeEditor) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { editor }
    )
}
