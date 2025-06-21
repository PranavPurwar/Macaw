package dev.pranav.filemanager.ui.editor

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import dev.pranav.filemanager.App
import dev.pranav.filemanager.util.mapFontNameToFontResource
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.component.Magnifier
import java.io.File

data class CodeEditorState(
    var editor: CodeEditor? = null,
    val initialContent: Content = Content()
) {
    var content by mutableStateOf(initialContent)
}

fun setCodeEditorFactory(
    context: Context,
    state: CodeEditorState
): CodeEditor {
    val editor = CodeEditor(context)
    editor.apply {
        setText(state.content)
    }
    state.editor = editor
    return editor
}

@Composable
fun CodeEditor(
    modifier: Modifier = Modifier,
    state: CodeEditorState
) {
    val context = LocalContext.current
    val editor = remember {
        setCodeEditorFactory(
            context = context,
            state = state
        )
    }
    AndroidView(
        factory = { editor },
        modifier = modifier,
        onRelease = {
            it.release()
        }
    )
}

fun CodeEditor.applyEditorSettings(file: File) {
    val prefs = App.prefs
    props.apply {
        useICULibToSelectWords = prefs.useICULibToSelectWords
        symbolPairAutoCompletion = prefs.symbolPairAutoCompletion
        deleteEmptyLineFast = prefs.deleteEmptyLineFast
        deleteMultiSpaces = if (prefs.deleteEmptyLineFast) -1 else 1
        autoIndent = prefs.autoIndentation
    }

    isWordwrap = prefs.wordWrap
    setPinLineNumber(prefs.pinLineNumber)
    getComponent(Magnifier::class.java).isEnabled = prefs.enableMagnifier

    setLineSpacing(prefs.lineSpacing, 1f)
    isLigatureEnabled = !prefs.disableLigatures
    isCursorAnimationEnabled = !prefs.disableCursorAnimation
    isHighlightBracketPair = prefs.highlightBracketPair

    isHardwareAcceleratedDrawAllowed = prefs.hardwareAccelerated

    isDisableSoftKbdIfHardKbdAvailable = prefs.disableSoftKbdIfHardKbdAvailable

    colorScheme = TextMateColorScheme.create(
        ThemeRegistry.getInstance().currentThemeModel
    )

    val fontResource = mapFontNameToFontResource(prefs.editorFont)
    if (fontResource != null) {
        typefaceText = ResourcesCompat.getFont(App.app, fontResource)
    }

    setEditorLanguage(
        when (file.extension.lowercase()) {
            "java" -> TextMateLanguage.create("source.java", true)
            "kt" -> TextMateLanguage.create("source.kotlin", true)
            "json" -> TextMateLanguage.create("source.json", true)
            "smali" -> TextMateLanguage.create("source.smali", true)
            else -> EmptyLanguage()
        }
    )
}
