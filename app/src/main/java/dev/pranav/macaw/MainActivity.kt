package dev.pranav.macaw

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import dev.pranav.macaw.ui.editor.resolveTheme
import dev.pranav.macaw.ui.home.FileExplorerHomeScreen
import dev.pranav.macaw.ui.theme.FileManagerTheme
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import org.eclipse.tm4e.core.registry.IThemeSource

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FileManagerTheme {
                FileManagerApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@PreviewScreenSizes
@Composable
fun FileManagerApp() {
    val context = LocalContext.current

    setupTextmateThemes(context, MaterialTheme.colorScheme)
    FileExplorerHomeScreen(Modifier.fillMaxSize())
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
