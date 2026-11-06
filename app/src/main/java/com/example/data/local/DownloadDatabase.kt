package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import com.example.data.model.DownloadStatus
import com.example.data.model.FormatPreset
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "download_tasks")
data class DownloadTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val batchId: String,
    val batchLabel: String,
    val sourceUrl: String,
    val resolvedStreamUrl: String,
    val title: String,
    val uploader: String,
    val extractorName: String,
    val thumbnailUrl: String,
    val durationSeconds: Int,
    val formatPresetId: String,
    val formatCode: String,
    val containerExt: String,
    val status: String,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val speedBytesPerSec: Long = 0L,
    val etaSeconds: Long = -1L,
    val localFilePath: String = "",
    val infoJsonPath: String = "",
    val sha256Hash: String = "",
    val errorMessage: String = "",
    val ytDlpCommand: String = "",
    val embedSubtitles: Boolean = true,
    val sponsorBlock: Boolean = false,
    val writeInfoJson: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long = 0L
) {
    val statusEnum: DownloadStatus
        get() = runCatching { DownloadStatus.valueOf(status) }.getOrDefault(DownloadStatus.QUEUED)

    val presetEnum: FormatPreset
        get() = FormatPreset.fromId(formatPresetId)

    val progressFraction: Float
        get() = if (totalBytes > 0L) {
            (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
        } else if (statusEnum == DownloadStatus.COMPLETED) {
            1f
        } else {
            0f
        }
}

@Dao
interface DownloadTaskDao {
    @Query("SELECT * FROM download_tasks ORDER BY createdAt DESC, id DESC")
    fun observeAllTasks(): Flow<List<DownloadTaskEntity>>

    @Query("SELECT * FROM download_tasks WHERE status != 'COMPLETED' ORDER BY createdAt DESC, id ASC")
    fun observeActiveQueueTasks(): Flow<List<DownloadTaskEntity>>

    @Query("SELECT * FROM download_tasks WHERE status = 'COMPLETED' ORDER BY completedAt DESC, id DESC")
    fun observeCompletedTasks(): Flow<List<DownloadTaskEntity>>

    @Query("SELECT * FROM download_tasks ORDER BY createdAt DESC, id DESC")
    suspend fun getAllTasksSnapshot(): List<DownloadTaskEntity>

    @Query("SELECT * FROM download_tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: Long): DownloadTaskEntity?

    @Query("SELECT * FROM download_tasks WHERE batchId = :batchId ORDER BY id ASC")
    suspend fun getTasksByBatchId(batchId: String): List<DownloadTaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: DownloadTaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<DownloadTaskEntity>): List<Long>

    @Update
    suspend fun updateTask(task: DownloadTaskEntity)

    @Query(
        """
        UPDATE download_tasks 
        SET status = :status, 
            downloadedBytes = :downloadedBytes, 
            totalBytes = :totalBytes, 
            speedBytesPerSec = :speedBytesPerSec, 
            etaSeconds = :etaSeconds
        WHERE id = :id
        """
    )
    suspend fun updateProgress(
        id: Long,
        status: String,
        downloadedBytes: Long,
        totalBytes: Long,
        speedBytesPerSec: Long,
        etaSeconds: Long
    )

    @Query("UPDATE download_tasks SET status = :status, speedBytesPerSec = 0, etaSeconds = -1, errorMessage = :errorMessage WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, errorMessage: String = "")

    @Query("DELETE FROM download_tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query("DELETE FROM download_tasks WHERE batchId = :batchId")
    suspend fun deleteBatchById(batchId: String)

    @Query("DELETE FROM download_tasks WHERE status = 'COMPLETED'")
    suspend fun clearCompletedTasks()
}

@Database(
    entities = [DownloadTaskEntity::class],
    version = 1,
    exportSchema = false
)
abstract class DownloadDatabase : RoomDatabase() {
    abstract fun downloadTaskDao(): DownloadTaskDao

    companion object {
        @Volatile
        private var INSTANCE: DownloadDatabase? = null

        fun getInstance(context: Context): DownloadDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    DownloadDatabase::class.java,
                    "yt_downloader_tasks.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
