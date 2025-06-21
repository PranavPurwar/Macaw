package dev.pranav.filemanager.ui.editor

import android.content.Context
import android.util.Log
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.gson.Gson
import java.io.InputStream

private val gson = Gson()

fun resolveTheme(context: Context, colorScheme: ColorScheme, fileName: String): InputStream {
    val theme = context.assets.open("textmate/$fileName")
    return applyAttributes(theme, colorScheme)
}

fun applyAttributes(stream: InputStream, colorScheme: ColorScheme): InputStream {
    val contents = stream.bufferedReader().readText()

    val json = gson.fromJson(contents, Map::class.java)
    // Should probably clean this up
    ((json["settings"]!! as List<Map<String, Any>>)[0]["settings"]!! as MutableMap<String, String>).let { settings ->
        settings["background"] =
            colorScheme.surfaceContainerLowest.hexString()
        settings["foreground"] =
            colorScheme.onSurface.hexString()
        settings["blockLineColor"] =
            colorScheme.primary.hexString()
        settings["lineHighlight"] =
            colorScheme.surfaceDim.hexString()
        settings["selection"] =
            colorScheme.primaryContainer.hexString()
        settings["caret"] =
            colorScheme.primary.hexString()
    }
    Log.d("MaterialEditorTheme", "Applying attributes to theme")
    Log.d("MaterialEditorTheme", json.toString())

    return gson.toJson(json).byteInputStream()
}

private fun Color.hexString(): String {
    val hex = String.format("#%06X", 0xFFFFFF and toArgb())
    Log.d("MaterialEditorTheme", "Color hex: $hex from Color: $this")
    return hex
}
