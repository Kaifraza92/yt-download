package com.example.data.repository

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.data.local.DownloadTaskDao
import com.example.data.local.DownloadTaskEntity
import com.example.data.model.DownloadStatus
import com.example.data.model.ExtractedMediaInfo
import com.example.data.model.FormatPreset
import com.example.data.network.YtDlpExtractorEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class DownloadRepository(
    private val appContext: Context,
    private val taskDao: DownloadTaskDao,
    val extractorEngine: YtDlpExtractorEngine = YtDlpExtractorEngine()
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = ConcurrentHashMap<Long, Job>()

    private val _maxConcurrency = MutableStateFlow(3)
    val maxConcurrency: StateFlow<Int> = _maxConcurrency.asStateFlow()

    private var downloadSemaphore = Semaphore(3)

    val allTasks: Flow<List<DownloadTaskEntity>> = taskDao.observeAllTasks()
    val activeQueueTasks: Flow<List<DownloadTaskEntity>> = taskDao.observeActiveQueueTasks()
    val completedTasks: Flow<List<DownloadTaskEntity>> = taskDao.observeCompletedTasks()

    val downloadsDir: File
        get() = File(appContext.filesDir, "downloads").also { if (!it.exists()) it.mkdirs() }

    fun setMaxConcurrency(limit: Int) {
        val coerced = limit.coerceIn(1, 6)
        _maxConcurrency.value = coerced
        downloadSemaphore = Semaphore(coerced)
    }

    suspend fun inspectSingleUrl(url: String): Result<ExtractedMediaInfo> {
        return runCatching {
            extractorEngine.extractMediaInfo(url)
        }
    }

    /**
     * Enqueues a batch of URLs (or a single URL) and optionally starts downloading immediately.
     */
    suspend fun enqueueBatchUrls(
        urls: List<String>,
        batchName: String = "",
        preset: FormatPreset,
        concurrentFragments: Int = 4,
        embedSubtitles: Boolean = true,
        sponsorBlock: Boolean = false,
        writeInfoJson: Boolean = true,
        outputTemplate: String = "%(uploader)s - %(title)s [%(id)s].%(ext)s",
        autoStart: Boolean = true,
        preExtractedInfoMap: Map<String, ExtractedMediaInfo> = emptyMap()
    ): List<Long> = withContext(Dispatchers.IO) {
        if (urls.isEmpty()) return@withContext emptyList()

        val batchId = UUID.randomUUID().toString().take(8)
        val timestampLabel = SimpleDateFormat("MMM dd, HH:mm", Locale.US).format(Date())
        val resolvedBatchLabel = batchName.trim().ifBlank {
            if (urls.size > 1) "Batch (${urls.size} items) • $timestampLabel" else "Single Download • $timestampLabel"
        }

        val now = System.currentTimeMillis()
        val entities = urls.mapIndexed { idx, rawUrl ->
            val preInfo = preExtractedInfoMap[rawUrl]
            val cmd = extractorEngine.buildYtDlpCommand(
                url = rawUrl,
                preset = preset,
                concurrentFragments = concurrentFragments,
                embedSubtitles = embedSubtitles,
                sponsorBlock = sponsorBlock,
                writeInfoJson = writeInfoJson,
                outputTemplate = outputTemplate
            )
            DownloadTaskEntity(
                batchId = batchId,
                batchLabel = resolvedBatchLabel,
                sourceUrl = rawUrl,
                resolvedStreamUrl = preInfo?.resolvedStreamUrl.orEmpty(),
                title = preInfo?.title ?: "Queued Stream #${idx + 1} (${rawUrl.substringAfter("://").take(34)})",
                uploader = preInfo?.uploader ?: "Pending Extraction",
                extractorName = preInfo?.extractorName ?: "yt-dlp:auto",
                thumbnailUrl = preInfo?.thumbnailUrl.orEmpty(),
                durationSeconds = preInfo?.durationSeconds ?: 0,
                formatPresetId = preset.id,
                formatCode = preset.badge,
                containerExt = preset.ext,
                status = DownloadStatus.QUEUED.name,
                downloadedBytes = 0L,
                totalBytes = preInfo?.estimatedSizeBytes ?: 0L,
                ytDlpCommand = cmd,
                embedSubtitles = embedSubtitles,
                sponsorBlock = sponsorBlock,
                writeInfoJson = writeInfoJson,
                createdAt = now + idx
            )
        }

        val insertedIds = taskDao.insertTasks(entities)
        if (autoStart) {
            insertedIds.forEach { id -> startOrResumeTask(id) }
        }
        insertedIds
    }

    fun startOrResumeTask(taskId: Long) {
        if (activeJobs[taskId]?.isActive == true) return

        val job = repositoryScope.launch {
            val current = taskDao.getTaskById(taskId) ?: return@launch
            if (current.statusEnum == DownloadStatus.COMPLETED) return@launch

            taskDao.updateStatus(taskId, DownloadStatus.QUEUED.name, "")

            downloadSemaphore.withPermit {
                val latest = taskDao.getTaskById(taskId) ?: return@withPermit
                if (latest.statusEnum == DownloadStatus.PAUSED || latest.statusEnum == DownloadStatus.COMPLETED) {
                    return@withPermit
                }

                try {
                    // Step 1: Extract media info if not yet resolved
                    taskDao.updateStatus(taskId, DownloadStatus.EXTRACTING.name, "")
                    val info = extractorEngine.extractMediaInfo(latest.sourceUrl)
                    val preset = latest.presetEnum

                    val enrichedTask = latest.copy(
                        resolvedStreamUrl = info.resolvedStreamUrl,
                        title = info.title,
                        uploader = info.uploader,
                        extractorName = info.extractorName,
                        thumbnailUrl = info.thumbnailUrl,
                        durationSeconds = info.durationSeconds,
                        totalBytes = if (latest.totalBytes > 0L) latest.totalBytes else info.estimatedSizeBytes,
                        status = DownloadStatus.DOWNLOADING.name,
                        errorMessage = ""
                    )
                    taskDao.updateTask(enrichedTask)

                    // Step 2: Stream media bytes to disk with progress updates
                    val result = extractorEngine.downloadMediaStream(
                        streamUrl = info.resolvedStreamUrl,
                        sourceUrl = enrichedTask.sourceUrl,
                        title = enrichedTask.title,
                        uploader = enrichedTask.uploader,
                        extractorName = enrichedTask.extractorName,
                        preset = preset,
                        ytDlpCommand = enrichedTask.ytDlpCommand,
                        writeInfoJson = enrichedTask.writeInfoJson,
                        outputDir = downloadsDir,
                        taskId = taskId
                    ) { downloaded, total, speed, eta ->
                        taskDao.updateProgress(
                            id = taskId,
                            status = DownloadStatus.DOWNLOADING.name,
                            downloadedBytes = downloaded,
                            totalBytes = total,
                            speedBytesPerSec = speed,
                            etaSeconds = eta
                        )
                    }

                    // Step 3: Mark completed
                    val completedEntity = taskDao.getTaskById(taskId)?.copy(
                        status = DownloadStatus.COMPLETED.name,
                        downloadedBytes = result.totalBytes,
                        totalBytes = result.totalBytes,
                        speedBytesPerSec = 0L,
                        etaSeconds = 0L,
                        localFilePath = result.localFilePath,
                        infoJsonPath = result.infoJsonPath,
                        sha256Hash = result.sha256Hash,
                        errorMessage = "",
                        completedAt = System.currentTimeMillis()
                    )
                    if (completedEntity != null) {
                        taskDao.updateTask(completedEntity)
                    }
                } catch (ce: CancellationException) {
                    throw ce
                } catch (e: Exception) {
                    taskDao.updateStatus(
                        id = taskId,
                        status = DownloadStatus.FAILED.name,
                        errorMessage = e.message ?: "Network stream interrupted"
                    )
                }
            }
        }

        activeJobs[taskId] = job
        job.invokeOnCompletion {
            activeJobs.remove(taskId)
        }
    }

    suspend fun pauseTask(taskId: Long) {
        activeJobs.remove(taskId)?.cancel()
        taskDao.updateStatus(taskId, DownloadStatus.PAUSED.name, "")
    }

    suspend fun startAllQueuedAndPaused() {
        val snapshot = taskDao.getAllTasksSnapshot()
        snapshot.filter {
            it.statusEnum == DownloadStatus.QUEUED ||
                it.statusEnum == DownloadStatus.PAUSED ||
                it.statusEnum == DownloadStatus.FAILED
        }.forEach { task ->
            startOrResumeTask(task.id)
        }
    }

    suspend fun pauseAllActive() {
        val activeIds = activeJobs.keys.toList()
        activeIds.forEach { id ->
            activeJobs.remove(id)?.cancel()
            taskDao.updateStatus(id, DownloadStatus.PAUSED.name, "")
        }
        val snapshot = taskDao.getAllTasksSnapshot()
        snapshot.filter {
            it.statusEnum == DownloadStatus.QUEUED ||
                it.statusEnum == DownloadStatus.EXTRACTING ||
                it.statusEnum == DownloadStatus.DOWNLOADING
        }.forEach { task ->
            taskDao.updateStatus(task.id, DownloadStatus.PAUSED.name, "")
        }
    }

    suspend fun retryFailedTasks() {
        val snapshot = taskDao.getAllTasksSnapshot()
        snapshot.filter { it.statusEnum == DownloadStatus.FAILED }.forEach { task ->
            startOrResumeTask(task.id)
        }
    }

    suspend fun startBatch(batchId: String) {
        val tasks = taskDao.getTasksByBatchId(batchId)
        tasks.filter { it.statusEnum != DownloadStatus.COMPLETED }.forEach { task ->
            startOrResumeTask(task.id)
        }
    }

    suspend fun pauseBatch(batchId: String) {
        val tasks = taskDao.getTasksByBatchId(batchId)
        tasks.filter { it.statusEnum != DownloadStatus.COMPLETED }.forEach { task ->
            pauseTask(task.id)
        }
    }

    suspend fun deleteBatch(batchId: String) = withContext(Dispatchers.IO) {
        val tasks = taskDao.getTasksByBatchId(batchId)
        tasks.forEach { task ->
            activeJobs.remove(task.id)?.cancel()
            deleteTaskFiles(task)
        }
        taskDao.deleteBatchById(batchId)
    }

    suspend fun deleteTask(taskId: Long) = withContext(Dispatchers.IO) {
        activeJobs.remove(taskId)?.cancel()
        val task = taskDao.getTaskById(taskId)
        if (task != null) {
            deleteTaskFiles(task)
        }
        taskDao.deleteTaskById(taskId)
    }

    suspend fun clearCompletedTasks() = withContext(Dispatchers.IO) {
        taskDao.clearCompletedTasks()
    }

    private fun deleteTaskFiles(task: DownloadTaskEntity) {
        runCatching {
            if (task.localFilePath.isNotBlank()) {
                File(task.localFilePath).delete()
                File("${task.localFilePath}.part").delete()
            }
            if (task.infoJsonPath.isNotBlank()) {
                File(task.infoJsonPath).delete()
            }
        }
    }

    /**
     * Exports a downloaded media file into the device's public Downloads folder via MediaStore
     * without requiring legacy storage permissions.
     */
    suspend fun exportToDeviceDownloads(task: DownloadTaskEntity): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val sourceFile = File(task.localFilePath)
            if (!sourceFile.exists()) {
                throw IllegalStateException("Local media file not found on disk.")
            }

            val fileName = sourceFile.name
            val mimeType = when (task.containerExt.lowercase(Locale.US)) {
                "mp3" -> "audio/mpeg"
                "m4a" -> "audio/mp4"
                "mkv" -> "video/x-matroska"
                "webm" -> "video/webm"
                else -> "video/mp4"
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = appContext.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType)
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/YT-Downloader")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val itemUri = resolver.insert(collection, contentValues)
                    ?: throw IllegalStateException("Could not create MediaStore entry.")

                resolver.openOutputStream(itemUri)?.use { out ->
                    sourceFile.inputStream().use { input ->
                        input.copyTo(out)
                    }
                }
                contentValues.clear()
                contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(itemUri, contentValues, null, null)
                "Saved to Downloads/YT-Downloader/$fileName"
            } else {
                "File ready in app storage: ${sourceFile.name}"
            }
        }
    }

    /**
     * Launches Android system share sheet for a downloaded media file via FileProvider.
     */
    fun shareDownloadedFile(context: Context, task: DownloadTaskEntity): Result<Unit> {
        return runCatching {
            val file = File(task.localFilePath)
            if (!file.exists()) {
                throw IllegalStateException("Downloaded file is missing.")
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val mimeType = if (task.presetEnum.isAudioOnly) "audio/*" else "video/*"
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, task.title)
                putExtra(Intent.EXTRA_TEXT, "${task.title} (${task.sourceUrl})")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "Share ${task.title}").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }
    }

    /**
     * Opens a downloaded media file in an external media player via FileProvider.
     */
    fun openDownloadedFile(context: Context, task: DownloadTaskEntity): Result<Unit> {
        return runCatching {
            val file = File(task.localFilePath)
            if (!file.exists()) {
                throw IllegalStateException("Downloaded file is missing.")
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val mimeType = if (task.presetEnum.isAudioOnly) "audio/*" else "video/*"
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(viewIntent, "Play ${task.title}").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }
}
