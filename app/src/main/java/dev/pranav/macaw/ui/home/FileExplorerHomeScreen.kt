package dev.pranav.macaw.ui.home

import android.content.Intent
import android.os.Environment
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.pranav.macaw.model.TabData
import dev.pranav.macaw.ui.settings.SettingsActivity
import dev.pranav.macaw.util.details
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
    ExperimentalMaterial3ExpressiveApi::class, ExperimentalAnimationApi::class
)
@Composable
fun FileExplorerHomeScreen(modifier: Modifier = Modifier) {
    val tabs = remember { mutableStateListOf(TabData(initialRootDir = Environment.getExternalStorageDirectory())) }
    val pagerState = rememberPagerState { tabs.size }
    val coroutineScope = rememberCoroutineScope()
    var activeTabIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(pagerState.currentPage, tabs.size) {
        if (tabs.isNotEmpty() && pagerState.currentPage < tabs.size) {
            activeTabIndex = pagerState.currentPage
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            FileExplorerAppBar(
                tabs = tabs,
                activeTabIndex = activeTabIndex,
                onAddTab = {
                    tabs.add(TabData(initialRootDir = Environment.getExternalStorageDirectory()))
                    coroutineScope.launch {
                        pagerState.scrollToPage(tabs.size - 1)
                    }
                },
                onTabSelected = { index ->
                    coroutineScope.launch {
                        pagerState.scrollToPage(index)
                    }
                }
            )
        }
    ) { innerScaffoldPadding ->
        FileExplorerContent(
            modifier = Modifier
                .padding(innerScaffoldPadding)
                .fillMaxSize(),
            tabs = tabs,
            pagerState = pagerState,
            onAddFirstTab = {
                tabs.add(TabData(initialRootDir = Environment.getExternalStorageDirectory()))
            },
            onDirectoryChange = { pageIndex, newDirectory ->
                if (pageIndex < tabs.size) {
                    tabs[pageIndex] = tabs[pageIndex].copy(currentPath = newDirectory)
                }
            }
        )
    }
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalAnimationApi::class
)
@Composable
private fun FileExplorerAppBar(
    tabs: List<TabData>,
    activeTabIndex: Int,
    onAddTab: () -> Unit,
    onTabSelected: (Int) -> Unit
) {
    val context = LocalContext.current
    var pathDetails by remember { mutableStateOf("") }
    val activeTab = tabs.getOrNull(activeTabIndex)

    LaunchedEffect(activeTab) {
        if (activeTab != null) {
            pathDetails = withContext(Dispatchers.IO) {
                activeTab.currentPath.details()
            }
        }
    }

    Column {
        TopAppBar(
            title = {
                Column {
                    AnimatedContent(
                        targetState = if (tabs.isEmpty()) "File Explorer" else tabs[activeTabIndex].name(),
                        label = "AppBarTitle",
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut() using SizeTransform(clip = false)
                        }
                    ) { title ->
                        Text(
                            title,
                            style = MaterialTheme.typography.titleLargeEmphasized,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    AnimatedContent(
                        targetState = pathDetails,
                        label = "AppBarSubtitle",
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut() using SizeTransform(clip = false)
                        }
                    ) { subtitle ->
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelMediumEmphasized,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            },
            actions = {
                IconButton(onClick = onAddTab) {
                    Icon(Icons.Default.Add, contentDescription = "New Tab")
                }
                IconButton(onClick = {
                    val intent = Intent(context, SettingsActivity::class.java)
                    context.startActivity(intent)
                }) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            }
        )

        TabRow(
            tabs = tabs,
            activeTabIndex = activeTabIndex,
            onTabSelected = onTabSelected
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
private fun TabRow(
    tabs: List<TabData>,
    activeTabIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    PrimaryScrollableTabRow(
        selectedTabIndex = activeTabIndex,
        modifier = Modifier.fillMaxWidth(),
        edgePadding = 4.dp,
        minTabWidth = 72.dp
    ) {
        tabs.forEachIndexed { index, tab ->
            Tab(
                selected = activeTabIndex == index,
                onClick = { onTabSelected(index) },
                text = {
                    AnimatedContent(
                        targetState = tab.name(),
                        label = "TabTitle",
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut() using SizeTransform(clip = false)
                        }
                    ) { title ->
                        Text(
                            text = title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                modifier = Modifier.combinedClickable(
                    onClick = { onTabSelected(index) },
                    onLongClick = { /* Tab options could be added here */ }
                )
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileExplorerContent(
    modifier: Modifier,
    tabs: List<TabData>,
    pagerState: PagerState,
    onAddFirstTab: () -> Unit,
    onDirectoryChange: (Int, File) -> Unit
) {
    if (tabs.isEmpty()) {
        EmptyTabsView(
            modifier = modifier,
            onAddFirstTab = onAddFirstTab
        )
    } else {
        HorizontalPager(
            state = pagerState,
            modifier = modifier,
            userScrollEnabled = true,
            key = { index -> tabs[index].id }
        ) { pageIndex ->
            if (pageIndex < tabs.size) {
                HomeScreen(
                    modifier = Modifier.fillMaxSize(),
                    initialDirectory = tabs[pageIndex].currentPath,
                    onDirectoryChange = { newDirectory ->
                        onDirectoryChange(pageIndex, newDirectory)
                    },
                    isCurrent = pagerState.currentPage == pageIndex
                )
            }
        }
    }
}

@Composable
private fun EmptyTabsView(
    modifier: Modifier,
    onAddFirstTab: () -> Unit
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Button(onClick = onAddFirstTab) {
            Text("Add First Tab")
        }
    }
}

fun TabData.name(): String {
    return if (currentPath.absolutePath == Environment.getExternalStorageDirectory().absolutePath) {
        "Internal Storage"
    } else {
        currentPath.name.takeIf { it.isNotEmpty() } ?: this.initialRootDir.name
    }
}
