package dev.pranav.filemanager.ui.home

import android.os.Environment
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import dev.pranav.filemanager.model.TabData
import dev.pranav.filemanager.util.details
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FileExplorerHomeScreen(modifier: Modifier = Modifier) {
    val tabs = remember { mutableStateListOf(TabData(initialRootDir = Environment.getExternalStorageDirectory())) }
    val pagerState = rememberPagerState { tabs.size }
    val coroutineScope = rememberCoroutineScope()
    var activeTabPathDisplay by remember { mutableStateOf("") }
    var tabMenuExpandedFor by remember { mutableStateOf<UUID?>(null) }

    LaunchedEffect(pagerState.currentPage, tabs.toList()) {
        activeTabPathDisplay = if (tabs.isNotEmpty() && pagerState.currentPage < tabs.size) {
            tabs[pagerState.currentPage].currentPath.details()
        } else {
            ""
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                if (tabs.isNotEmpty() && pagerState.currentPage < tabs.size) {
                                    (tabs[pagerState.currentPage].currentPath.name.takeIf { it.isNotEmpty() } ?: tabs[pagerState.currentPage].initialRootDir.name).let {
                                        if (it == "0" && tabs[pagerState.currentPage].currentPath.absolutePath == Environment.getExternalStorageDirectory().absolutePath) "Internal Storage" else it
                                    }
                                } else {
                                    "File Explorer"
                                },
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Text(
                                activeTabPathDisplay,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            val newTab = TabData(initialRootDir = Environment.getExternalStorageDirectory())
                            tabs.add(newTab)
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(tabs.size - 1)
                            }
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "New Tab")
                        }
                        IconButton(onClick = {
                            /* Go to settings */
                        }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                )
                if (tabs.isNotEmpty()) {
                    TabRow(selectedTabIndex = pagerState.currentPage) {
                        tabs.forEachIndexed { index, tabData ->
                            Box(
                                modifier = Modifier.pointerInput(tabData.id) {
                                    detectTapGestures(
                                        onLongPress = {
                                            tabMenuExpandedFor = tabData.id
                                        }
                                    )
                                }
                            ) {
                                Tab(
                                    selected = pagerState.currentPage == index,
                                    modifier = Modifier.combinedClickable(
                                        onClick = {},
                                        onLongClick = {
                                            tabMenuExpandedFor = tabData.id
                                        }
                                    ),
                                    onClick = {
                                        coroutineScope.launch { pagerState.animateScrollToPage(index) }
                                    },
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            val displayName = if (tabData.currentPath.absolutePath == Environment.getExternalStorageDirectory().absolutePath && tabData.currentPath.name == "0") {
                                                "Internal Storage"
                                            } else {
                                                tabData.currentPath.name.takeIf { it.isNotEmpty() } ?: tabData.initialRootDir.name
                                            }
                                            Text(
                                                text = displayName.let {
                                                    if (it.length > 16) it.take(13) + "..." else it
                                                },
                                                style = MaterialTheme.typography.bodyMedium,
                                                overflow = TextOverflow.Ellipsis,
                                                maxLines = 1,
                                            )
                                        }
                                    }
                                )

                                DropdownMenu(
                                    expanded = tabMenuExpandedFor == tabData.id,
                                    onDismissRequest = { tabMenuExpandedFor = null }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Remove") },
                                        onClick = {
                                            tabs.removeAt(index)
                                            tabMenuExpandedFor = null
                                        },
                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = "Remove Tab") }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(if (tabData.isBookmarked) "Unbookmark" else "Bookmark") },
                                        onClick = {
                                            tabs[index] = tabData.copy(isBookmarked = !tabData.isBookmarked)
                                            tabMenuExpandedFor = null
                                            // TODO: Implement actual bookmark saving/loading logic
                                        },
                                        leadingIcon = { Icon(Icons.Default.Bookmark, contentDescription = "Bookmark Tab") }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerScaffoldPadding -> // This is the padding from the Scaffold itself
        if (tabs.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerScaffoldPadding), // Apply padding from this Scaffold
                contentAlignment = Alignment.Center
            ) {
                Button(onClick = {
                    tabs.add(TabData(initialRootDir = Environment.getExternalStorageDirectory()))
                }) {
                    Text("Add First Tab")
                }
            }
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .padding(innerScaffoldPadding) // Apply padding from this Scaffold
                    .fillMaxSize()
            ) { pageIndex ->
                if (pageIndex < tabs.size) {
                    val currentTabData = tabs[pageIndex]
                    HomeScreen(
                        modifier = Modifier.fillMaxSize(),
                        initialDirectory = currentTabData.currentPath,
                        onDirectoryChange = { newDirectory ->
                            tabs[pageIndex] = currentTabData.copy(currentPath = newDirectory)
                            if (pagerState.currentPage == pageIndex) {
                                activeTabPathDisplay = newDirectory.details()
                            }
                        }
                    )
                }
            }
        }
    }
}
