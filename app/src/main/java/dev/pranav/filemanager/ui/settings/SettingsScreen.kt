package dev.pranav.filemanager.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import me.zhanghai.compose.preference.Preference
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.listPreference
import me.zhanghai.compose.preference.sliderPreference
import me.zhanghai.compose.preference.switchPreference
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    val title = when (selectedCategory) {
        "editor" -> "Editor"
        "about" -> "About"
        else -> "Settings"
    }

    Scaffold(
        modifier = modifier,
        topBar = { LargeFlexibleTopAppBar(title = { Text(title) }) }
    ) { padding ->
        ProvidePreferenceLocals {
            if (selectedCategory == null) {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                    item {
                        Preference(
                            title = { Text("Editor")},
                            icon = {
                                Icon(Icons.Default.Edit, contentDescription = "Editor")
                            },
                            onClick = { selectedCategory = "editor" }
                        )
                    }
                    item {
                        Preference(
                            title = { Text("About")},
                            icon = {
                                Icon(Icons.Outlined.Info, contentDescription = "About")
                            },
                            onClick = { selectedCategory = "about" }
                        )
                    }
                }
            } else {
                BackHandler {
                    selectedCategory = null
                }
                when (selectedCategory) {
                    "editor" -> EditorSettings(Modifier.padding(padding))
                    "about" -> AboutSettings(Modifier.padding(padding))
                }
            }
        }
    }
}

@Composable
private fun EditorSettings(modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        listPreference(
            key = "editorFont",
            defaultValue = "Jetbrains Mono",
            values = listOf("Jetbrains Mono", "Normal"),
            title = { Text(text = "Editor Font") },
            summary = { Text(text = it) },
        )
        switchPreference(
            key = "autoIndentation",
            defaultValue = true,
            title = { Text("Auto Indentation") },
            summary = { Text("Automatically indents code as per previous indentation level") }
        )
        switchPreference(
            key = "disableSoftKbdIfHardKbdAvailable",
            defaultValue = true,
            title = { Text("Disable software keyboard") },
            summary = { Text("Disables soft keyboard when connected to an external keyboard") }
        )
        switchPreference(
            key = "disableCursorAnimation",
            defaultValue = false,
            title = { Text("Disable cursor animation") },
            summary = { Text("Disables editor cursor flicker animation") }
        )
        switchPreference(
            key = "disableLigatures",
            defaultValue = false,
            title = { Text("Disable ligatures") },
            summary = { Text("Disables all font ligatures") }
        )
        switchPreference(
            key = "highlightBracketPair",
            defaultValue = true,
            title = { Text("Highlight bracket pair") },
            summary = { Text("Highlights matching pairs of brackets currently on cursor") }
        )
        switchPreference(
            key = "hardwareAccelerated",
            defaultValue = true,
            title = { Text("Hardware accelerated") },
            summary = { Text("Use hardware acceleration to improve editor performance") }
        )
        sliderPreference(
            key = "lineSpacing",
            defaultValue = 0f,
            title = { Text("Line spacing") },
            valueRange = 0f..10f,
            valueSteps = 10,
            valueText = { Text(it.roundToInt().toString()) }
        )
    }
}

@Composable
private fun AboutSettings(modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            Preference(
                title = { Text("File Manager") },
                summary = { Text("Version 1.0.0") },
                icon = { Icon(Icons.Outlined.Info, contentDescription = "About") }
            )
        }
    }
}
