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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.twotone.Bookmark
import androidx.compose.material.icons.twotone.PhoneAndroid
import androidx.compose.material.icons.twotone.SdStorage
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.pranav.macaw.model.TabData
import dev.pranav.macaw.model.name
import dev.pranav.macaw.ui.actions.ActionsActivity
import dev.pranav.macaw.ui.file.SortMenu
import dev.pranav.macaw.ui.settings.SettingsActivity
import dev.pranav.macaw.util.SortOrder
import dev.pranav.macaw.util.details
import dev.pranav.macaw.util.getStorageVolumes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.file.Path

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
    ExperimentalMaterial3ExpressiveApi::class, ExperimentalAnimationApi::class
)
@Composable
fun FileExplorerHomeScreen(
    modifier: Modifier = Modifier,
    onNavigateToBookmarks: () -> Unit,
    newPathToOpen: Path?,
    onNewPathHandled: () -> Unit
) {
    val tabs = remember {
        mutableStateListOf(
            TabData(
                initialRootDir = Environment.getExternalStorageDirectory().toPath()
            )
        )
    }
    val pagerState = rememberPagerState { tabs.size }
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val context = LocalContext.current

    LaunchedEffect(newPathToOpen) {
        if (newPathToOpen != null) {
            if (pagerState.currentPage < tabs.size) {
                tabs[pagerState.currentPage] =
                    tabs[pagerState.currentPage].copy(currentPath = newPathToOpen)
            } else {
                tabs.add(TabData(initialRootDir = newPathToOpen))
            }
            onNewPathHandled()
        }
    }

    val storageVolumesInfo = remember(context) { getStorageVolumes(context) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                storageVolumesInfo.forEach { volumeInfo ->
                    val file = volumeInfo.file.toPath()
                    val isActive =
                        pagerState.currentPage < tabs.size && tabs[pagerState.currentPage].currentPath.startsWith(
                            file
                        )
                    val icon = if (volumeInfo.isRemovable) {
                        Icons.TwoTone.SdStorage
                    } else {
                        Icons.TwoTone.PhoneAndroid
                    }
                    NavigationDrawerItem(
                        icon = { Icon(icon, contentDescription = volumeInfo.name) },
                        label = { Text(volumeInfo.name) },
                        selected = isActive,
                        onClick = {
                            if (pagerState.currentPage < tabs.size) {
                                tabs[pagerState.currentPage] =
                                    tabs[pagerState.currentPage].copy(
                                        currentPath = file,
                                        initialRootDir = file
                                    )
                            } else {
                                tabs.add(TabData(initialRootDir = file))
                            }
                            coroutineScope.launch {
                                drawerState.close()
                            }
                        }
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                NavigationDrawerItem(
                    icon = { Icon(Icons.TwoTone.Bookmark, contentDescription = "Bookmarks") },
                    label = { Text("Bookmarks") },
                    selected = false,
                    onClick = {
                        coroutineScope.launch {
                            drawerState.close()
                        }
                        onNavigateToBookmarks()
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Ongoing Actions") },
                    selected = false,
                    onClick = {
                        context.startActivity(Intent(context, ActionsActivity::class.java))
                        coroutineScope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) }
                )
                HorizontalDivider()
                Text("Storage", modifier = Modifier.padding(16.dp))
            }
        }
    ) {
        Scaffold(
            modifier = modifier,
            topBar = {
                FileExplorerAppBar(
                    tabs = tabs,
                    activeTabIndex = pagerState.currentPage,
                    onAddTab = {
                        tabs.add(
                            TabData(
                                initialRootDir = Environment.getExternalStorageDirectory().toPath()
                            )
                        )
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(tabs.size - 1)
                        }
                    },
                    onTabSelected = { index ->
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    onNavigationIconClick = {
                        coroutineScope.launch {
                            drawerState.open()
                        }
                    },
                    onSortOrderChange = { tabIndex, newSortOrder ->
                        if (tabIndex >= 0 && tabIndex < tabs.size) {
                            val currentTab = tabs[tabIndex]
                            if (currentTab.sortOrder != newSortOrder) {
                                tabs[tabIndex] = currentTab.copy(
                                    sortOrder = newSortOrder
                                )
                                coroutineScope.launch {
                                    tabs[tabIndex].lazyListState.scrollToItem(0)
                                }
                            }
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
                    tabs.add(
                        TabData(
                            initialRootDir = Environment.getExternalStorageDirectory().toPath()
                        )
                    )
                },
                onDirectoryChange = { pageIndex, newDirectory ->
                    if (pageIndex < tabs.size) {
                        tabs[pageIndex] = tabs[pageIndex].copy(currentPath = newDirectory)
                    }
                }
            )
        }
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
    onTabSelected: (Int) -> Unit,
    onNavigationIconClick: () -> Unit,
    onSortOrderChange: (Int, SortOrder) -> Unit
) {
    val context = LocalContext.current
    var pathDetails by remember { mutableStateOf("") }
    val activeTab = tabs.getOrNull(activeTabIndex)
    var showSortMenu by remember { mutableStateOf(false) }

    LaunchedEffect(activeTab) {
        if (activeTab != null) {
            pathDetails = withContext(Dispatchers.IO) {
                activeTab.currentPath.toFile().details()
            }
        }
    }

    Column {
        TopAppBar(
            title = {
                Column {
                    AnimatedContent(
                        targetState = tabs.getOrNull(activeTabIndex)?.name() ?: "File Explorer",
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
            navigationIcon = {
                IconButton(onClick = onNavigationIconClick) {
                    Icon(Icons.Default.Menu, contentDescription = "Open drawer")
                }
            },
            actions = {
                Box {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(
                            Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Sort files"
                        )
                    }
                    SortMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        currentSortOrder = activeTab?.sortOrder ?: SortOrder.NAME_ASCENDING,
                        onSortOrderSelected = { sortOrder ->
                            if (activeTab != null) {
                                onSortOrderChange(activeTabIndex, sortOrder)
                            }
                            showSortMenu = false
                        }
                    )
                }

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
                onClick = {
                    if (activeTabIndex != index) {
                        onTabSelected(index)
                    }
                },
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
                    onClick = {
                        if (activeTabIndex != index) {
                            onTabSelected(index)
                        }
                    },
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
    onDirectoryChange: (Int, Path) -> Unit,
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
                    tabData = tabs[pageIndex],
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
