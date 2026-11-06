package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.local.DownloadTaskEntity
import com.example.data.model.BatchGroupSummary
import com.example.data.model.DownloadStatus
import com.example.ui.components.BatchGroupCard
import com.example.ui.components.DownloadTaskCard
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ObsidianBlack
import com.example.ui.util.FormatUtils
import com.example.ui.viewmodel.AppScreenTab
import com.example.ui.viewmodel.DownloaderUiState
import com.example.ui.viewmodel.DownloaderViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BatchQueueScreen(
    uiState: DownloaderUiState,
    allTasks: List<DownloadTaskEntity>,
    batchGroups: List<BatchGroupSummary>,
    viewModel: DownloaderViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.selectTab(AppScreenTab.DOWNLOADER)
    }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val filteredTasks = remember(allTasks, uiState.selectedBatchFilter) {
        if (uiState.selectedBatchFilter == null) {
            allTasks
        } else {
            allTasks.filter { it.batchId == uiState.selectedBatchFilter }
        }
    }

    val activeCount = allTasks.count {
        it.statusEnum == DownloadStatus.DOWNLOADING || it.statusEnum == DownloadStatus.EXTRACTING
    }
    val queuedOrPausedCount = allTasks.count {
        it.statusEnum == DownloadStatus.QUEUED || it.statusEnum == DownloadStatus.PAUSED
    }
    val failedCount = allTasks.count { it.statusEnum == DownloadStatus.FAILED }
    val completedCount = allTasks.count { it.statusEnum == DownloadStatus.COMPLETED }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 760.dp)
                .testTag("batch_queue_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Batch Queue Controller Header Card
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.55f)),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("batch_controller_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Batch Queue Controller",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "$activeCount running • $queuedOrPausedCount waiting • $completedCount done",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Surface(
                                color = CrimsonRed.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${uiState.concurrentWorkers}x Parallel",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = CrimsonRed,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { viewModel.startAllTasks() },
                                enabled = queuedOrPausedCount > 0 || failedCount > 0,
                                colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("batch_start_all_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Start All",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Start All")
                            }

                            FilledTonalButton(
                                onClick = { viewModel.pauseAllTasks() },
                                enabled = activeCount > 0,
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("batch_pause_all_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Pause,
                                    contentDescription = "Pause All",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pause All")
                            }

                            if (failedCount > 0) {
                                OutlinedButton(
                                    onClick = { viewModel.retryFailedTasks() },
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                    modifier = Modifier.testTag("batch_retry_failed_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Retry Failed",
                                        tint = CyberAmber,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Retry ($failedCount)")
                                }
                            }

                            if (completedCount > 0) {
                                OutlinedButton(
                                    onClick = { viewModel.clearCompletedTasks() },
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                    modifier = Modifier.testTag("batch_clear_completed_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CleaningServices,
                                        contentDescription = "Clear Completed",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Clear Done")
                                }
                            }
                        }
                    }
                }
            }

            // 2. Batch Groups Section
            if (batchGroups.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Batch Groups (${batchGroups.size})",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (uiState.selectedBatchFilter != null) {
                            TextButton(
                                onClick = { viewModel.selectBatchFilter(null) },
                                modifier = Modifier.testTag("clear_batch_filter_button")
                            ) {
                                Text("Show All Batches", color = CrimsonRed)
                            }
                        }
                    }
                }

                items(batchGroups, key = { "group_${it.batchId}" }) { batch ->
                    BatchGroupCard(
                        batch = batch,
                        isSelected = uiState.selectedBatchFilter == batch.batchId,
                        onSelectFilter = {
                            val next = if (uiState.selectedBatchFilter == batch.batchId) null else batch.batchId
                            viewModel.selectBatchFilter(next)
                        },
                        onStartBatch = { viewModel.startBatch(batch.batchId) },
                        onPauseBatch = { viewModel.pauseBatch(batch.batchId) },
                        onDeleteBatch = { viewModel.deleteBatch(batch.batchId) }
                    )
                }
            }

            // 3. Individual Tasks List or Empty State
            if (filteredTasks.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .testTag("empty_batch_queue_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderSpecial,
                                contentDescription = null,
                                tint = CrimsonRed,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Batch Queue is Empty",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Paste multiple video or audio URLs in the Downloader tab or start the 4-stream sample batch to watch concurrent downloads in action.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(
                                    onClick = { viewModel.selectTab(AppScreenTab.DOWNLOADER) },
                                    modifier = Modifier.testTag("empty_go_downloader_button")
                                ) {
                                    Text("Add URLs")
                                }
                                Button(
                                    onClick = { viewModel.queueSampleBatchImmediately() },
                                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed),
                                    modifier = Modifier.testTag("empty_start_sample_batch_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlaylistPlay,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Start 4-Stream Batch")
                                }
                            }
                        }
                    }
                }
            } else {
                item {
                    Text(
                        text = "Download Tasks (${filteredTasks.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                items(filteredTasks, key = { it.id }) { task ->
                    DownloadTaskCard(
                        task = task,
                        onStartOrResume = { viewModel.startOrResumeTask(task.id) },
                        onPause = { viewModel.pauseTask(task.id) },
                        onDelete = { viewModel.deleteTask(task.id) },
                        onOpenFile = { viewModel.openTaskFile(context, task) },
                        onShareFile = { viewModel.shareTaskFile(context, task) },
                        onExportToDownloads = { viewModel.exportToPublicDownloads(task) },
                        onCopyCommand = { cmd ->
                            clipboardManager.setText(AnnotatedString(cmd))
                            viewModel.postStatusMessage("Copied yt-dlp command")
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MediaLibraryScreen(
    completedTasks: List<DownloadTaskEntity>,
    viewModel: DownloaderViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.selectTab(AppScreenTab.DOWNLOADER)
    }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var filterAudioOnly by remember { mutableStateOf<Boolean?>(null) }

    val displayedTasks = remember(completedTasks, filterAudioOnly) {
        when (filterAudioOnly) {
            true -> completedTasks.filter { it.presetEnum.isAudioOnly }
            false -> completedTasks.filter { !it.presetEnum.isAudioOnly }
            null -> completedTasks
        }
    }

    val totalSavedBytes = remember(completedTasks) {
        completedTasks.sumOf { it.totalBytes }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 760.dp)
                .testTag("media_library_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Downloaded Media Library",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "${completedTasks.size} files saved • ${FormatUtils.formatBytes(totalSavedBytes)} verified",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EmeraldSuccess
                                )
                            }

                            if (completedTasks.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = { viewModel.clearCompletedTasks() },
                                    modifier = Modifier.testTag("library_clear_button")
                                ) {
                                    Text("Clear History")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = filterAudioOnly == null,
                                onClick = { filterAudioOnly = null },
                                label = { Text("All (${completedTasks.size})") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CrimsonRed.copy(alpha = 0.2f),
                                    selectedLabelColor = CrimsonRed
                                ),
                                modifier = Modifier.testTag("library_filter_all")
                            )
                            FilterChip(
                                selected = filterAudioOnly == false,
                                onClick = { filterAudioOnly = false },
                                label = { Text("Video") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.VideoLibrary,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CrimsonRed.copy(alpha = 0.2f),
                                    selectedLabelColor = CrimsonRed
                                ),
                                modifier = Modifier.testTag("library_filter_video")
                            )
                            FilterChip(
                                selected = filterAudioOnly == true,
                                onClick = { filterAudioOnly = true },
                                label = { Text("Audio") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.LibraryMusic,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CrimsonRed.copy(alpha = 0.2f),
                                    selectedLabelColor = CrimsonRed
                                ),
                                modifier = Modifier.testTag("library_filter_audio")
                            )
                        }
                    }
                }
            }

            if (displayedTasks.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .testTag("empty_library_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlaylistAddCheck,
                                contentDescription = null,
                                tint = CrimsonRed,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Completed Downloads Yet",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Completed downloads appear here with SHA-256 checksums, .info.json metadata, and 1-tap export to your device's Downloads folder.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(displayedTasks, key = { it.id }) { task ->
                    DownloadTaskCard(
                        task = task,
                        onStartOrResume = { viewModel.startOrResumeTask(task.id) },
                        onPause = { viewModel.pauseTask(task.id) },
                        onDelete = { viewModel.deleteTask(task.id) },
                        onOpenFile = { viewModel.openTaskFile(context, task) },
                        onShareFile = { viewModel.shareTaskFile(context, task) },
                        onExportToDownloads = { viewModel.exportToPublicDownloads(task) },
                        onCopyCommand = { cmd ->
                            clipboardManager.setText(AnnotatedString(cmd))
                            viewModel.postStatusMessage("Copied yt-dlp command")
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun YtDlpCliStudioScreen(
    uiState: DownloaderUiState,
    viewModel: DownloaderViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.selectTab(AppScreenTab.DOWNLOADER)
    }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val generatedScript = viewModel.generateCurrentCliScript()

    val templatePresets = listOf(
        "%(uploader)s - %(title)s [%(id)s].%(ext)s" to "Standard Uploader + Title",
        "%(playlist_index)s - %(title)s.%(ext)s" to "Numbered Playlist / Batch",
        "%(upload_date)s_%(title)s_%(height)sp.%(ext)s" to "Archival Date + Resolution"
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 760.dp)
                .testTag("cli_studio_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.55f)),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = null,
                                tint = CrimsonRed
                            )
                            Column {
                                Text(
                                    text = "yt-dlp CLI Command & Batch Script Studio",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Configure output templates, fragment threads, and export reproducible yt-dlp scripts",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = uiState.outputTemplate,
                            onValueChange = { viewModel.updateOutputTemplate(it) },
                            label = { Text("Output Filename Template (-o)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("cli_output_template_field")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            templatePresets.forEachIndexed { idx, (tpl, label) ->
                                FilterChip(
                                    selected = uiState.outputTemplate == tpl,
                                    onClick = { viewModel.updateOutputTemplate(tpl) },
                                    label = { Text(label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CrimsonRed.copy(alpha = 0.2f),
                                        selectedLabelColor = CrimsonRed
                                    ),
                                    modifier = Modifier.testTag("template_preset_chip_$idx")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Concurrent Fragments (-N): ${uiState.concurrentFragments}",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(1, 4, 8, 16).forEach { frag ->
                                    val selected = uiState.concurrentFragments == frag
                                    FilterChip(
                                        selected = selected,
                                        onClick = { viewModel.updateConcurrentFragments(frag) },
                                        label = { Text("${frag}f") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = CrimsonRed.copy(alpha = 0.2f),
                                            selectedLabelColor = CrimsonRed
                                        ),
                                        modifier = Modifier.testTag("fragment_chip_$frag")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Live Terminal Code Preview Card
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = ObsidianBlack
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cli_terminal_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Code,
                                    contentDescription = null,
                                    tint = CrimsonRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "batch_download.sh",
                                    fontFamily = JetBrainsMonoFamily,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = CrimsonRed
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilledTonalButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(generatedScript))
                                        viewModel.postStatusMessage("Copied yt-dlp batch script to clipboard")
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("copy_cli_script_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy Script",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Copy")
                                }

                                Button(
                                    onClick = {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, "yt-dlp Batch Script")
                                            putExtra(Intent.EXTRA_TEXT, generatedScript)
                                        }
                                        context.startActivity(
                                            Intent.createChooser(shareIntent, "Export yt-dlp Script").apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("export_cli_script_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Export Script",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Export")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            color = Color(0xFF11131A),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF282C3E)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = generatedScript,
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFE2E8F0),
                                modifier = Modifier
                                    .padding(14.dp)
                                    .testTag("generated_cli_script_text")
                            )
                        }
                    }
                }
            }
        }
    }
}
