package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.DownloadDatabase
import com.example.data.local.DownloadTaskEntity
import com.example.data.model.BatchGroupSummary
import com.example.data.model.DownloadStatus
import com.example.data.model.ExtractedMediaInfo
import com.example.data.model.FormatPreset
import com.example.data.model.OpenSampleMediaCatalog
import com.example.data.repository.DownloadRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AppScreenTab {
    DOWNLOADER,
    BATCH_QUEUE,
    LIBRARY,
    CLI_STUDIO
}

data class DownloaderUiState(
    val currentTab: AppScreenTab = AppScreenTab.DOWNLOADER,
    val urlInputText: String = "",
    val parsedUrls: List<String> = emptyList(),
    val batchLabelInput: String = "",
    val selectedPreset: FormatPreset = FormatPreset.BEST_VIDEO,
    val concurrentWorkers: Int = 3,
    val concurrentFragments: Int = 4,
    val embedSubtitles: Boolean = true,
    val sponsorBlock: Boolean = false,
    val writeInfoJson: Boolean = true,
    val outputTemplate: String = "%(uploader)s - %(title)s [%(id)s].%(ext)s",
    val isInspecting: Boolean = false,
    val inspectedMediaInfo: ExtractedMediaInfo? = null,
    val selectedBatchFilter: String? = null,
    val statusMessage: String? = null,
    val isDarkTheme: Boolean = true
)

class DownloaderViewModel(
    private val repository: DownloadRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DownloaderUiState())
    val uiState: StateFlow<DownloaderUiState> = _uiState.asStateFlow()

    val allTasks: StateFlow<List<DownloadTaskEntity>> = repository.allTasks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val activeQueueTasks: StateFlow<List<DownloadTaskEntity>> = repository.activeQueueTasks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val completedTasks: StateFlow<List<DownloadTaskEntity>> = repository.completedTasks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val batchGroups: StateFlow<List<BatchGroupSummary>> = allTasks
        .map { tasks ->
            tasks.groupBy { it.batchId }
                .map { (batchId, groupTasks) ->
                    val first = groupTasks.first()
                    BatchGroupSummary(
                        batchId = batchId,
                        batchLabel = first.batchLabel,
                        createdAt = groupTasks.minOf { it.createdAt },
                        totalCount = groupTasks.size,
                        completedCount = groupTasks.count { it.statusEnum == DownloadStatus.COMPLETED },
                        activeCount = groupTasks.count {
                            it.statusEnum == DownloadStatus.DOWNLOADING ||
                                it.statusEnum == DownloadStatus.EXTRACTING ||
                                it.statusEnum == DownloadStatus.QUEUED
                        },
                        failedCount = groupTasks.count { it.statusEnum == DownloadStatus.FAILED },
                        totalDownloadedBytes = groupTasks.sumOf { it.downloadedBytes },
                        totalExpectedBytes = groupTasks.sumOf {
                            if (it.totalBytes > 0L) it.totalBytes else it.downloadedBytes
                        }
                    )
                }
                .sortedByDescending { it.createdAt }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun selectTab(tab: AppScreenTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun toggleTheme() {
        _uiState.update { it.copy(isDarkTheme = !it.isDarkTheme) }
    }

    fun updateUrlInput(rawText: String) {
        val parsed = repository.extractorEngine.extractUrlsFromText(rawText)
        _uiState.update {
            it.copy(
                urlInputText = rawText,
                parsedUrls = parsed
            )
        }
    }

    fun clearUrlInput() {
        _uiState.update {
            it.copy(
                urlInputText = "",
                parsedUrls = emptyList(),
                inspectedMediaInfo = null
            )
        }
    }

    fun appendUrlToInput(url: String) {
        val current = _uiState.value.urlInputText.trim()
        val combined = if (current.isEmpty()) url else "$current\n$url"
        updateUrlInput(combined)
    }

    fun updateBatchLabel(label: String) {
        _uiState.update { it.copy(batchLabelInput = label) }
    }

    fun selectFormatPreset(preset: FormatPreset) {
        _uiState.update { it.copy(selectedPreset = preset) }
    }

    fun updateConcurrentWorkers(workers: Int) {
        val coerced = workers.coerceIn(1, 6)
        repository.setMaxConcurrency(coerced)
        _uiState.update { it.copy(concurrentWorkers = coerced) }
    }

    fun updateConcurrentFragments(fragments: Int) {
        _uiState.update { it.copy(concurrentFragments = fragments.coerceIn(1, 16)) }
    }

    fun toggleEmbedSubtitles(enabled: Boolean) {
        _uiState.update { it.copy(embedSubtitles = enabled) }
    }

    fun toggleSponsorBlock(enabled: Boolean) {
        _uiState.update { it.copy(sponsorBlock = enabled) }
    }

    fun toggleWriteInfoJson(enabled: Boolean) {
        _uiState.update { it.copy(writeInfoJson = enabled) }
    }

    fun updateOutputTemplate(template: String) {
        _uiState.update { it.copy(outputTemplate = template) }
    }

    fun selectBatchFilter(batchId: String?) {
        _uiState.update { it.copy(selectedBatchFilter = batchId) }
    }

    fun dismissInspectedInfo() {
        _uiState.update { it.copy(inspectedMediaInfo = null) }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    fun postStatusMessage(msg: String) {
        _uiState.update { it.copy(statusMessage = msg) }
    }

    fun inspectPrimaryUrl() {
        val targetUrl = _uiState.value.parsedUrls.firstOrNull()
        if (targetUrl.isNullOrBlank()) {
            postStatusMessage("Paste at least one valid URL to inspect.")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isInspecting = true, inspectedMediaInfo = null) }
            val result = repository.inspectSingleUrl(targetUrl)
            result.onSuccess { info ->
                _uiState.update {
                    it.copy(
                        isInspecting = false,
                        inspectedMediaInfo = info,
                        statusMessage = "Extracted [${info.extractorName}]: ${info.title}"
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isInspecting = false,
                        statusMessage = "Could not inspect URL: ${err.message}"
                    )
                }
            }
        }
    }

    fun enqueueCurrentUrls(autoStart: Boolean = true, switchToQueueTab: Boolean = false) {
        val state = _uiState.value
        val urls = state.parsedUrls
        if (urls.isEmpty()) {
            postStatusMessage("Enter one or more media URLs (one per line for batch).")
            return
        }
        val preExtractedMap = state.inspectedMediaInfo?.let { mapOf(it.sourceUrl to it) } ?: emptyMap()
        viewModelScope.launch {
            val ids = repository.enqueueBatchUrls(
                urls = urls,
                batchName = state.batchLabelInput,
                preset = state.selectedPreset,
                concurrentFragments = state.concurrentFragments,
                embedSubtitles = state.embedSubtitles,
                sponsorBlock = state.sponsorBlock,
                writeInfoJson = state.writeInfoJson,
                outputTemplate = state.outputTemplate,
                autoStart = autoStart,
                preExtractedInfoMap = preExtractedMap
            )
            _uiState.update {
                it.copy(
                    urlInputText = "",
                    parsedUrls = emptyList(),
                    batchLabelInput = "",
                    inspectedMediaInfo = null,
                    currentTab = if (switchToQueueTab || ids.size > 1) AppScreenTab.BATCH_QUEUE else it.currentTab,
                    statusMessage = if (ids.size > 1) {
                        "Batch queued: ${ids.size} media streams (${state.selectedPreset.badge})"
                    } else {
                        "Download started (${state.selectedPreset.badge})"
                    }
                )
            }
        }
    }

    fun loadOpenSampleBatchIntoInput() {
        val sampleLines = buildString {
            appendLine("# yt-dlp Open Creative Commons Media Batch")
            OpenSampleMediaCatalog.sampleBatchItems.take(4).forEach { item ->
                appendLine("# ${item.title} (${item.uploader})")
                appendLine(item.url)
            }
        }.trim()
        updateUrlInput(sampleLines)
        _uiState.update {
            it.copy(
                batchLabelInput = "Open Movie HD Batch (4 Videos)",
                statusMessage = "Loaded 4 open-licensed HD media URLs into batch input"
            )
        }
    }

    fun queueSampleBatchImmediately() {
        val state = _uiState.value
        val items = OpenSampleMediaCatalog.sampleBatchItems.take(4)
        val urls = items.map { it.url }
        viewModelScope.launch {
            repository.enqueueBatchUrls(
                urls = urls,
                batchName = "Open Movie HD Batch (${items.size} Streams)",
                preset = state.selectedPreset,
                concurrentFragments = state.concurrentFragments,
                embedSubtitles = state.embedSubtitles,
                sponsorBlock = state.sponsorBlock,
                writeInfoJson = state.writeInfoJson,
                outputTemplate = state.outputTemplate,
                autoStart = true
            )
            _uiState.update {
                it.copy(
                    currentTab = AppScreenTab.BATCH_QUEUE,
                    statusMessage = "Started batch download of ${items.size} open media streams"
                )
            }
        }
    }

    fun importBatchTextFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
                }
            }
            result.onSuccess { content ->
                if (content.isNotBlank()) {
                    val current = _uiState.value.urlInputText.trim()
                    val merged = if (current.isEmpty()) content else "$current\n$content"
                    updateUrlInput(merged)
                    val count = _uiState.value.parsedUrls.size
                    postStatusMessage("Imported batch file ($count total URLs detected)")
                } else {
                    postStatusMessage("Selected text file was empty.")
                }
            }.onFailure { err ->
                postStatusMessage("Failed to read file: ${err.message}")
            }
        }
    }

    fun startOrResumeTask(taskId: Long) {
        repository.startOrResumeTask(taskId)
    }

    fun pauseTask(taskId: Long) {
        viewModelScope.launch {
            repository.pauseTask(taskId)
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            repository.deleteTask(taskId)
            postStatusMessage("Removed task from queue")
        }
    }

    fun startAllTasks() {
        viewModelScope.launch {
            repository.startAllQueuedAndPaused()
            postStatusMessage("Resumed all queued & paused downloads")
        }
    }

    fun pauseAllTasks() {
        viewModelScope.launch {
            repository.pauseAllActive()
            postStatusMessage("Paused all active downloads")
        }
    }

    fun retryFailedTasks() {
        viewModelScope.launch {
            repository.retryFailedTasks()
            postStatusMessage("Retrying failed downloads")
        }
    }

    fun clearCompletedTasks() {
        viewModelScope.launch {
            repository.clearCompletedTasks()
            postStatusMessage("Cleared completed tasks from history")
        }
    }

    fun startBatch(batchId: String) {
        viewModelScope.launch {
            repository.startBatch(batchId)
            postStatusMessage("Started batch jobs")
        }
    }

    fun pauseBatch(batchId: String) {
        viewModelScope.launch {
            repository.pauseBatch(batchId)
            postStatusMessage("Paused batch jobs")
        }
    }

    fun deleteBatch(batchId: String) {
        viewModelScope.launch {
            repository.deleteBatch(batchId)
            if (_uiState.value.selectedBatchFilter == batchId) {
                _uiState.update { it.copy(selectedBatchFilter = null) }
            }
            postStatusMessage("Deleted batch group")
        }
    }

    fun exportToPublicDownloads(task: DownloadTaskEntity) {
        viewModelScope.launch {
            repository.exportToDeviceDownloads(task)
                .onSuccess { msg -> postStatusMessage(msg) }
                .onFailure { err -> postStatusMessage("Export failed: ${err.message}") }
        }
    }

    fun shareTaskFile(context: Context, task: DownloadTaskEntity) {
        repository.shareDownloadedFile(context, task)
            .onFailure { err -> postStatusMessage("Unable to share: ${err.message}") }
    }

    fun openTaskFile(context: Context, task: DownloadTaskEntity) {
        repository.openDownloadedFile(context, task)
            .onFailure { err -> postStatusMessage("Unable to open file: ${err.message}") }
    }

    fun generateCurrentCliScript(): String {
        val state = _uiState.value
        val urls = if (state.parsedUrls.isNotEmpty()) {
            state.parsedUrls
        } else {
            allTasks.value.take(5).map { it.sourceUrl }
        }
        return repository.extractorEngine.buildBatchScript(
            urls = urls,
            preset = state.selectedPreset,
            concurrentFragments = state.concurrentFragments,
            embedSubtitles = state.embedSubtitles,
            sponsorBlock = state.sponsorBlock,
            writeInfoJson = state.writeInfoJson,
            outputTemplate = state.outputTemplate
        )
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = DownloadDatabase.getInstance(context)
                    val repo = DownloadRepository(
                        appContext = context.applicationContext,
                        taskDao = db.downloadTaskDao()
                    )
                    return DownloaderViewModel(repo) as T
                }
            }
        }
    }
}
