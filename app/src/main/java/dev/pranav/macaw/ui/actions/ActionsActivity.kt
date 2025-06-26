package dev.pranav.macaw.ui.actions

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.pranav.macaw.ui.theme.FileManagerTheme

class ActionsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FileManagerTheme {
                ActionsScreen()
            }
        }
    }
}

