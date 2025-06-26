package dev.pranav.macaw

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import dev.pranav.macaw.model.FileAction
import dev.pranav.macaw.service.ActionService
import dev.pranav.macaw.ui.bookmarks.BookmarksActivity
import dev.pranav.macaw.ui.editor.resolveTheme
import dev.pranav.macaw.ui.file.determineFileAction
import dev.pranav.macaw.ui.file.executeFileAction
import dev.pranav.macaw.ui.home.FileExplorerHomeScreen
import dev.pranav.macaw.ui.theme.FileManagerTheme
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import org.eclipse.tm4e.core.registry.IThemeSource
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FileManagerTheme {
                FileManagerApp()
            }
        }
        startService(Intent(this, ActionService::class.java))
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@PreviewScreenSizes
@Composable
fun FileManagerApp() {
    val context = LocalContext.current
    var newPath by remember { mutableStateOf<String?>(null) }
    val colorScheme = MaterialTheme.colorScheme

    val bookmarksLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringExtra("path")?.let { path ->
                val file = File(path)
                if (file.isDirectory) {
                    newPath = file.absolutePath
                } else {
                    val action = determineFileAction(file)
                    if (action == FileAction.HANDLE_AUDIO || action == FileAction.OPEN_APK_DETAILS) {
                        newPath = file.parent
                        Toast.makeText(context, "Opening file location", Toast.LENGTH_SHORT).show()
                    } else {
                        executeFileAction(context, file, action)
                    }
                }
            }
        }
    }

    setupTextmateThemes(context, colorScheme)

    FileExplorerHomeScreen(
        modifier = Modifier.fillMaxSize(),
        onNavigateToBookmarks = {
            bookmarksLauncher.launch(Intent(context, BookmarksActivity::class.java))
        },
        newPathToOpen = newPath,
        onNewPathHandled = { newPath = null }
    )
}

fun setupTextmateThemes(context: Context, colorScheme: ColorScheme) {
    val themes = arrayOf("darcula.json", "QuietLight.tmTheme.json")
    val themeRegistry = ThemeRegistry.getInstance()

    themes.forEach { name ->
        themeRegistry.loadTheme(
            ThemeModel(
                IThemeSource.fromInputStream(
                    resolveTheme(context, colorScheme, name), name, null
                ), name.substringBefore('.')
            ).apply {
                isDark = name.substringBefore('.') == "darcula"
            }
        )
    }

    context.applyThemeBasedOnConfiguration()
}

fun Context.applyThemeBasedOnConfiguration() {
    val themeName =
        when (AppCompatDelegate.getDefaultNightMode()) {
            AppCompatDelegate.MODE_NIGHT_YES -> "darcula"
            AppCompatDelegate.MODE_NIGHT_NO -> "light"
            else -> {
                when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                    Configuration.UI_MODE_NIGHT_YES -> "darcula"
                    else -> "light"
                }
            }
        }
    ThemeRegistry.getInstance().setTheme(themeName)
}
