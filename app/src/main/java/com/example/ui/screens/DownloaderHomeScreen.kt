package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.AutoAwesomeMotion
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.DownloadTaskEntity
import com.example.data.model.FormatPreset
import com.example.data.model.OpenSampleMediaCatalog
import com.example.ui.components.DownloadTaskCard
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.util.FormatUtils
import com.example.ui.viewmodel.AppScreenTab
import com.example.ui.viewmodel.DownloaderUiState
import com.example.ui.viewmodel.DownloaderViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DownloaderHomeScreen(
    uiState: DownloaderUiState,
    activeQueueTasks: List<DownloadTaskEntity>,
    viewModel: DownloaderViewModel,
    onLaunchImportTxt: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val urlCount = uiState.parsedUrls.size
    val isBatchMode = urlCount > 1

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 760.dp)
                .testTag("downloader_home_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. URL & Batch Composer Card
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        1.2.dp,
                        if (isBatchMode) CrimsonRed.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline
                    ),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("url_composer_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isBatchMode) Icons.Default.AutoAwesomeMotion else Icons.Default.AddLink,
                                    contentDescription = null,
                                    tint = CrimsonRed
                                )
                                Text(
                                    text = if (isBatchMode) "Batch URL Queue Composer" else "Media URL & Batch Input",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Surface(
                                color = when {
                                    isBatchMode -> CrimsonRed.copy(alpha = 0.2f)
                                    urlCount == 1 -> EmeraldSuccess.copy(alpha = 0.18f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(
                                    1.dp,
                                    when {
                                        isBatchMode -> CrimsonRed
                                        urlCount == 1 -> EmeraldSuccess
                                        else -> MaterialTheme.colorScheme.outline
                                    }
                                )
                            ) {
                                Text(
                                    text = when {
                                        isBatchMode -> "BATCH • $urlCount URLS"
                                        urlCount == 1 -> "1 URL READY"
                                        else -> "PASTE LINKS"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = when {
                                        isBatchMode -> CrimsonRed
                                        urlCount == 1 -> EmeraldSuccess
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = uiState.urlInputText,
                            onValueChange = { viewModel.updateUrlInput(it) },
                            label = {
                                Text("Media URL or Multiple Batch Links (1 per line)")
                            },
                            placeholder = {
                                Text("https://www.youtube.com/watch?v=...\nhttps://archive.org/download/...\n# Supports yt-dlp batch syntax")
                            },
                            minLines = 3,
                            maxLines = 7,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CrimsonRed,
                                cursorColor = CrimsonRed
                            ),
                            trailingIcon = {
                                if (uiState.urlInputText.isNotEmpty()) {
                                    IconButton(
                                        onClick = { viewModel.clearUrlInput() },
                                        modifier = Modifier.testTag("clear_url_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear URL input"
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("url_input_field")
                        )

                        if (isBatchMode) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = uiState.batchLabelInput,
                                onValueChange = { viewModel.updateBatchLabel(it) },
                                label = { Text("Batch Group Name (Optional)") },
                                placeholder = { Text("e.g., Lecture Series HD / Music Playlist") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("batch_label_field")
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick tools row: Paste, Import .txt, Load Sample Batch
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    val clipText = clipboardManager.getText()?.text.orEmpty()
                                    if (clipText.isNotBlank()) {
                                        viewModel.appendUrlToInput(clipText)
                                    } else {
                                        viewModel.postStatusMessage("Clipboard is empty")
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("paste_url_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = "Paste from clipboard",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Paste", style = MaterialTheme.typography.labelLarge)
                            }

                            FilledTonalButton(
                                onClick = onLaunchImportTxt,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("import_txt_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileOpen,
                                    contentDescription = "Import .txt batch file",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Import .txt", style = MaterialTheme.typography.labelLarge)
                            }

                            FilledTonalButton(
                                onClick = { viewModel.loadOpenSampleBatchIntoInput() },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("load_sample_batch_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlaylistAdd,
                                    contentDescription = "Load sample batch",
                                    tint = CrimsonRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Load Sample Batch (4)", style = MaterialTheme.typography.labelLarge)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Primary CTAs: Inspect URL + Download Now / Queue Batch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.inspectPrimaryUrl() },
                                enabled = urlCount > 0 && !uiState.isInspecting,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier
                                    .weight(0.4f)
                                    .height(50.dp)
                                    .testTag("inspect_url_button")
                            ) {
                                if (uiState.isInspecting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = CrimsonRed
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Inspect formats",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Inspect")
                                }
                            }

                            Button(
                                onClick = {
                                    viewModel.enqueueCurrentUrls(
                                        autoStart = true,
                                        switchToQueueTab = urlCount > 1
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CrimsonRed,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(0.6f)
                                    .height(50.dp)
                                    .testTag("start_download_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Download Media",
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when {
                                        urlCount > 1 -> "Download Batch ($urlCount)"
                                        urlCount == 1 -> "Download Now"
                                        else -> "Download Media"
                                    },
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }

            // 2. Format & Quality Preset Selector
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.HighQuality,
                                contentDescription = null,
                                tint = CrimsonRed
                            )
                            Text(
                                text = "Format & Stream Preset",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Text(
                            text = "-f ${uiState.selectedPreset.ext.uppercase()}",
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.labelMedium,
                            color = CrimsonRed
                        )
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp),
                        modifier = Modifier.testTag("format_preset_row")
                    ) {
                        items(FormatPreset.entries, key = { it.id }) { preset ->
                            val selected = uiState.selectedPreset == preset
                            Card(
                                modifier = Modifier
                                    .width(195.dp)
                                    .clickable { viewModel.selectFormatPreset(preset) }
                                    .testTag("preset_card_${preset.id}"),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selected) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    }
                                ),
                                border = BorderStroke(
                                    width = if (selected) 1.8.dp else 1.dp,
                                    color = if (selected) CrimsonRed else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                ),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (preset.isAudioOnly) Icons.Default.AudioFile else Icons.Default.VideoFile,
                                            contentDescription = null,
                                            tint = if (selected) CrimsonRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Surface(
                                            color = if (selected) CrimsonRed else MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = preset.badge,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = preset.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = preset.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Batch Engine Flags & Concurrency Controls
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = CrimsonRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Parallel Batch Workers: ${uiState.concurrentWorkers}x",
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(1, 2, 3, 4).forEach { count ->
                                    val active = uiState.concurrentWorkers == count
                                    Surface(
                                        color = if (active) CrimsonRed else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .clickable { viewModel.updateConcurrentWorkers(count) }
                                            .testTag("concurrency_chip_$count")
                                    ) {
                                        Text(
                                            text = "${count}x",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FilterChip(
                                selected = uiState.embedSubtitles,
                                onClick = { viewModel.toggleEmbedSubtitles(!uiState.embedSubtitles) },
                                label = { Text("--embed-subs") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Subtitles,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CrimsonRed.copy(alpha = 0.2f),
                                    selectedLabelColor = CrimsonRed
                                ),
                                modifier = Modifier.testTag("chip_embed_subs")
                            )

                            FilterChip(
                                selected = uiState.sponsorBlock,
                                onClick = { viewModel.toggleSponsorBlock(!uiState.sponsorBlock) },
                                label = { Text("--sponsorblock-mark") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CrimsonRed.copy(alpha = 0.2f),
                                    selectedLabelColor = CrimsonRed
                                ),
                                modifier = Modifier.testTag("chip_sponsorblock")
                            )

                            FilterChip(
                                selected = uiState.writeInfoJson,
                                onClick = { viewModel.toggleWriteInfoJson(!uiState.writeInfoJson) },
                                label = { Text("--write-info-json") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CrimsonRed.copy(alpha = 0.2f),
                                    selectedLabelColor = CrimsonRed
                                ),
                                modifier = Modifier.testTag("chip_write_info_json")
                            )
                        }
                    }
                }
            }

            // 4. Active Queue Live Preview (if tasks are running/queued)
            if (activeQueueTasks.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Active Queue (${activeQueueTasks.size})",
                            style = MaterialTheme.typography.titleMedium
                        )
                        TextButton(
                            onClick = { viewModel.selectTab(AppScreenTab.BATCH_QUEUE) },
                            modifier = Modifier.testTag("view_all_queue_button")
                        ) {
                            Text("Open Batch Controller →", color = CrimsonRed)
                        }
                    }
                }

                items(activeQueueTasks.take(3), key = { it.id }) { task ->
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

            // 5. Open-Licensed Creative Commons Stream Catalog for 1-Tap Batch Testing
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Open Creative Commons Media Streams",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Test multi-stream batch downloading or append to your batch queue",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Button(
                                onClick = { viewModel.queueSampleBatchImmediately() },
                                colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("instant_batch_run_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlaylistPlay,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Run 4-Stream Batch", style = MaterialTheme.typography.labelLarge)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OpenSampleMediaCatalog.sampleBatchItems.forEachIndexed { idx, sample ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = sample.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${sample.uploader} • ${sample.category} • ${FormatUtils.formatDuration(sample.durationSeconds)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    TextButton(
                                        onClick = {
                                            viewModel.appendUrlToInput(sample.url)
                                            viewModel.postStatusMessage("Added '${sample.title}' to batch input")
                                        },
                                        modifier = Modifier.testTag("sample_add_button_$idx")
                                    ) {
                                        Text("+ Batch", color = CrimsonRed, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
