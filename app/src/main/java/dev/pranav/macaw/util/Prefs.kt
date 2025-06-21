package dev.pranav.macaw.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class Prefs(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("editor_prefs", Context.MODE_PRIVATE)

    var useICULibToSelectWords: Boolean
        get() = prefs.getBoolean("useICULibToSelectWords", true)
        set(value) = prefs.edit { putBoolean("useICULibToSelectWords", value) }

    var showLineNumber: Boolean
        get() = prefs.getBoolean("showLineNumber", true)
        set(value) = prefs.edit { putBoolean("showLineNumber", value) }

    var pinLineNumber: Boolean
        get() = prefs.getBoolean("pinLineNumber", false)
        set(value) = prefs.edit { putBoolean("pinLineNumber", value) }

    var symbolPairAutoCompletion: Boolean
        get() = prefs.getBoolean("symbolPairAutoCompletion", true)
        set(value) = prefs.edit { putBoolean("symbolPairAutoCompletion", value) }

    var deleteEmptyLineFast: Boolean
        get() = prefs.getBoolean("deleteEmptyLineFast", true)
        set(value) = prefs.edit { putBoolean("deleteEmptyLineFast", value) }

    var enableMagnifier: Boolean
        get() = prefs.getBoolean("enableMagnifier", true)
        set(value) = prefs.edit { putBoolean("enableMagnifier", value) }

    var wordWrap: Boolean
        get() = prefs.getBoolean("wordWrap", false)
        set(value) = prefs.edit { putBoolean("wordWrap", value) }

    var editorFont: String
        get() = prefs.getString("editorFont", "Jetbrains Mono") ?: "Jetbrains Mono"
        set(value) = prefs.edit { putString("editorFont", value) }

    var autoIndentation: Boolean
        get() = prefs.getBoolean("autoIndentation", true)
        set(value) = prefs.edit { putBoolean("autoIndentation", value) }

    var disableSoftKbdIfHardKbdAvailable: Boolean
        get() = prefs.getBoolean("disableSoftKbdIfHardKbdAvailable", true)
        set(value) = prefs.edit { putBoolean("disableSoftKbdIfHardKbdAvailable", value) }

    var disableCursorAnimation: Boolean
        get() = prefs.getBoolean("disableCursorAnimation", false)
        set(value) = prefs.edit { putBoolean("disableCursorAnimation", value) }

    var disableLigatures: Boolean
        get() = prefs.getBoolean("disableLigatures", false)
        set(value) = prefs.edit { putBoolean("disableLigatures", value) }

    var lineSpacing: Float
        get() = prefs.getFloat("lineSpacing", 1.2f)
        set(value) = prefs.edit { putFloat("lineSpacing", value) }

    var highlightBracketPair: Boolean
        get() = prefs.getBoolean("highlightBracketPair", true)
        set(value) = prefs.edit { putBoolean("highlightBracketPair", value) }

    var hardwareAccelerated: Boolean
        get() = prefs.getBoolean("hardwareAccelerated", true)
        set(value) = prefs.edit { putBoolean("hardwareAccelerated", value) }
}
