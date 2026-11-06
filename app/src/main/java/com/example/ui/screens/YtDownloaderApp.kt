package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DownloadStatus
import com.example.ui.components.CustomBrandHeader
import com.example.ui.components.InspectedMediaDialog
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppScreenTab
import com.example.ui.viewmodel.DownloaderViewModel

private data class NavDestination(
    val tab: AppScreenTab,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
    val badgeCount: Int = 0
)

@Composable
fun YtDownloaderApp(
    viewModel: DownloaderViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val allTasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val activeQueueTasks by viewModel.activeQueueTasks.collectAsStateWithLifecycle()
    val completedTasks by viewModel.completedTasks.collectAsStateWithLifecycle()
    val batchGroups by viewModel.batchGroups.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val importTxtLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importBatchTextFile(context, uri)
        }
    }

    LaunchedEffect(uiState.statusMessage) {
        val msg = uiState.statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    val totalSpeedBytesPerSec = remember(allTasks) {
        allTasks.filter { it.statusEnum == DownloadStatus.DOWNLOADING }
            .sumOf { it.speedBytesPerSec }
    }
    val activeDownloadingCount = remember(allTasks) {
        allTasks.count {
            it.statusEnum == DownloadStatus.DOWNLOADING || it.statusEnum == DownloadStatus.EXTRACTING
        }
    }

    val destinations = listOf(
        NavDestination(
            tab = AppScreenTab.DOWNLOADER,
            label = "Downloader",
            selectedIcon = Icons.Filled.Download,
            unselectedIcon = Icons.Outlined.Download,
            testTag = "nav_tab_downloader"
        ),
        NavDestination(
            tab = AppScreenTab.BATCH_QUEUE,
            label = "Batch Queue",
            selectedIcon = Icons.Filled.PlaylistPlay,
            unselectedIcon = Icons.Outlined.PlaylistPlay,
            testTag = "nav_tab_batch_queue",
            badgeCount = activeQueueTasks.size
        ),
        NavDestination(
            tab = AppScreenTab.LIBRARY,
            label = "Library",
            selectedIcon = Icons.Filled.VideoLibrary,
            unselectedIcon = Icons.Outlined.VideoLibrary,
            testTag = "nav_tab_library",
            badgeCount = completedTasks.size
        ),
        NavDestination(
            tab = AppScreenTab.CLI_STUDIO,
            label = "yt-dlp CLI",
            selectedIcon = Icons.Filled.Terminal,
            unselectedIcon = Icons.Outlined.Terminal,
            testTag = "nav_tab_cli_studio"
        )
    )

    MyApplicationTheme(darkTheme = uiState.isDarkTheme) {
        uiState.inspectedMediaInfo?.let { info ->
            InspectedMediaDialog(
                info = info,
                onDismiss = { viewModel.dismissInspectedInfo() },
                onDownloadNow = {
                    viewModel.enqueueCurrentUrls(autoStart = true, switchToQueueTab = false)
                }
            )
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isExpandedScreen = maxWidth >= 700.dp

            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    CustomBrandHeader(
                        activeCount = activeDownloadingCount,
                        totalSpeedBytesPerSec = totalSpeedBytesPerSec,
                        isDarkTheme = uiState.isDarkTheme,
                        onToggleTheme = { viewModel.toggleTheme() }
                    )
                },
                bottomBar = {
                    if (!isExpandedScreen) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 6.dp
                        ) {
                            destinations.forEach { dest ->
                                val selected = uiState.currentTab == dest.tab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { viewModel.selectTab(dest.tab) },
                                    icon = {
                                        BadgedBox(
                                            badge = {
                                                if (dest.badgeCount > 0) {
                                                    Badge(
                                                        containerColor = if (dest.tab == AppScreenTab.LIBRARY) {
                                                            EmeraldSuccess
                                                        } else {
                                                            CrimsonRed
                                                        },
                                                        contentColor = if (dest.tab == AppScreenTab.LIBRARY) {
                                                            Color.Black
                                                        } else {
                                                            Color.White
                                                        }
                                                    ) {
                                                        Text(dest.badgeCount.toString())
                                                    }
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                contentDescription = dest.label
                                            )
                                        }
                                    },
                                    label = { Text(dest.label) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = CrimsonRed,
                                        selectedTextColor = CrimsonRed,
                                        indicatorColor = CrimsonRed.copy(alpha = 0.16f)
                                    ),
                                    modifier = Modifier.testTag(dest.testTag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isExpandedScreen) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            destinations.forEach { dest ->
                                val selected = uiState.currentTab == dest.tab
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { viewModel.selectTab(dest.tab) },
                                    icon = {
                                        BadgedBox(
                                            badge = {
                                                if (dest.badgeCount > 0) {
                                                    Badge(containerColor = CrimsonRed) {
                                                        Text(dest.badgeCount.toString())
                                                    }
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                contentDescription = dest.label
                                            )
                                        }
                                    },
                                    label = { Text(dest.label) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = CrimsonRed,
                                        selectedTextColor = CrimsonRed,
                                        indicatorColor = CrimsonRed.copy(alpha = 0.16f)
                                    ),
                                    modifier = Modifier.testTag(dest.testTag)
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        when (uiState.currentTab) {
                            AppScreenTab.DOWNLOADER -> DownloaderHomeScreen(
                                uiState = uiState,
                                activeQueueTasks = activeQueueTasks,
                                viewModel = viewModel,
                                onLaunchImportTxt = {
                                    importTxtLauncher.launch(arrayOf("text/plain", "text/*"))
                                }
                            )

                            AppScreenTab.BATCH_QUEUE -> BatchQueueScreen(
                                uiState = uiState,
                                allTasks = allTasks,
                                batchGroups = batchGroups,
                                viewModel = viewModel
                            )

                            AppScreenTab.LIBRARY -> MediaLibraryScreen(
                                completedTasks = completedTasks,
                                viewModel = viewModel
                            )

                            AppScreenTab.CLI_STUDIO -> YtDlpCliStudioScreen(
                                uiState = uiState,
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
    }
}
