package com.example.data.model

enum class DownloadStatus(val label: String) {
    QUEUED("QUEUED"),
    EXTRACTING("EXTRACTING"),
    DOWNLOADING("DOWNLOADING"),
    PAUSED("PAUSED"),
    COMPLETED("COMPLETED"),
    FAILED("FAILED")
}

enum class FormatPreset(
    val id: String,
    val title: String,
    val subtitle: String,
    val formatSelector: String,
    val ext: String,
    val isAudioOnly: Boolean,
    val badge: String
) {
    BEST_VIDEO(
        id = "best_mp4",
        title = "Best Video + Audio",
        subtitle = "Highest quality muxed MP4 stream",
        formatSelector = "bv*[ext=mp4]+ba[ext=m4a]/b[ext=mp4]/bv*+ba/b",
        ext = "mp4",
        isAudioOnly = false,
        badge = "MAX MP4"
    ),
    VIDEO_1080P(
        id = "1080p_mp4",
        title = "1080p Full HD",
        subtitle = "H.264 / AAC • Balanced size & clarity",
        formatSelector = "bv*[height<=1080][ext=mp4]+ba[ext=m4a]/b[height<=1080]",
        ext = "mp4",
        isAudioOnly = false,
        badge = "1080p"
    ),
    VIDEO_720P(
        id = "720p_mp4",
        title = "720p HD Fast",
        subtitle = "Fast batch downloading • Data saver",
        formatSelector = "bv*[height<=720][ext=mp4]+ba[ext=m4a]/b[height<=720]",
        ext = "mp4",
        isAudioOnly = false,
        badge = "720p"
    ),
    AUDIO_MP3(
        id = "audio_mp3",
        title = "Audio Only (MP3)",
        subtitle = "320kbps CBR • ID3 tags & cover art",
        formatSelector = "ba/b -x --audio-format mp3 --audio-quality 0",
        ext = "mp3",
        isAudioOnly = true,
        badge = "MP3 320K"
    ),
    AUDIO_M4A(
        id = "audio_m4a",
        title = "Audio Only (M4A)",
        subtitle = "Original AAC stream without re-encoding",
        formatSelector = "ba[ext=m4a]/ba -x --audio-format m4a",
        ext = "m4a",
        isAudioOnly = true,
        badge = "M4A AAC"
    ),
    ARCHIVE_MKV(
        id = "archive_mkv",
        title = "Archival MKV + Subs",
        subtitle = "All streams, chapters, subs & .info.json",
        formatSelector = "bv*+ba/b --merge-output-format mkv",
        ext = "mkv",
        isAudioOnly = false,
        badge = "MKV ARCHIVE"
    );

    companion object {
        fun fromId(id: String): FormatPreset =
            entries.find { it.id == id } ?: BEST_VIDEO
    }
}

data class StreamFormatOption(
    val formatId: String,
    val ext: String,
    val resolution: String,
    val fps: Int,
    val vcodec: String,
    val acodec: String,
    val filesizeBytes: Long,
    val note: String
)

data class ExtractedMediaInfo(
    val sourceUrl: String,
    val resolvedStreamUrl: String,
    val title: String,
    val uploader: String,
    val extractorName: String,
    val durationSeconds: Int,
    val thumbnailUrl: String,
    val estimatedSizeBytes: Long,
    val mimeType: String,
    val isDirectStream: Boolean,
    val availableFormats: List<StreamFormatOption>
)

data class BatchGroupSummary(
    val batchId: String,
    val batchLabel: String,
    val createdAt: Long,
    val totalCount: Int,
    val completedCount: Int,
    val activeCount: Int,
    val failedCount: Int,
    val totalDownloadedBytes: Long,
    val totalExpectedBytes: Long
) {
    val overallProgress: Float
        get() = if (totalExpectedBytes > 0L) {
            (totalDownloadedBytes.toFloat() / totalExpectedBytes.toFloat()).coerceIn(0f, 1f)
        } else if (totalCount > 0) {
            (completedCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f)
        } else 0f
}

data class SampleMediaItem(
    val title: String,
    val uploader: String,
    val url: String,
    val category: String,
    val durationSeconds: Int
)

object OpenSampleMediaCatalog {
    val sampleBatchItems = listOf(
        SampleMediaItem(
            title = "Big Buck Bunny - Open Movie 720p HD Trailer",
            uploader = "Blender Foundation",
            url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            category = "Open Movie (CC-BY)",
            durationSeconds = 596
        ),
        SampleMediaItem(
            title = "Sintel - Open Fantasy Short Film Clip",
            uploader = "Blender Institute",
            url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
            category = "Animation (CC-BY)",
            durationSeconds = 888
        ),
        SampleMediaItem(
            title = "Tears of Steel - Sci-Fi VFX Showcase",
            uploader = "Blender Studio Amsterdam",
            url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
            category = "Sci-Fi Short (CC-BY)",
            durationSeconds = 734
        ),
        SampleMediaItem(
            title = "Elephants Dream - Remastered Open Film",
            uploader = "Orange Open Movie Project",
            url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
            category = "3D Short (CC-BY)",
            durationSeconds = 653
        ),
        SampleMediaItem(
            title = "For Bigger Blazes - High Bitrate Stream Test",
            uploader = "Open Media Benchmark",
            url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            category = "HD Stream Benchmark",
            durationSeconds = 15
        ),
        SampleMediaItem(
            title = "For Bigger Escapes - Nature 1080p Stream",
            uploader = "Open Media Benchmark",
            url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
            category = "HD Stream Benchmark",
            durationSeconds = 15
        )
    )
}
