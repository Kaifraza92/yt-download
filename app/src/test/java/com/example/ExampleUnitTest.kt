package com.example

import com.example.data.model.FormatPreset
import com.example.data.network.YtDlpExtractorEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    private val engine = YtDlpExtractorEngine()

    @Test
    fun batchUrlExtraction_ignoresCommentsAndDeduplicates() {
        val rawBatchInput = """
            # Batch download list
            https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4
            youtube.com/watch?v=aqz-KE-bpKQ
            https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4
            ; semicolon comment
            https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4
        """.trimIndent()

        val extracted = engine.extractUrlsFromText(rawBatchInput)
        assertEquals(3, extracted.size)
        assertEquals("https://youtube.com/watch?v=aqz-KE-bpKQ", extracted[1])
    }

    @Test
    fun buildYtDlpCommand_includesAudioExtractionFlagsForMp3() {
        val cmd = engine.buildYtDlpCommand(
            url = "https://www.youtube.com/watch?v=aqz-KE-bpKQ",
            preset = FormatPreset.AUDIO_MP3,
            concurrentFragments = 4,
            embedSubtitles = true,
            sponsorBlock = true,
            writeInfoJson = true
        )
        assertTrue(cmd.startsWith("yt-dlp"))
        assertTrue(cmd.contains("--audio-format mp3"))
        assertTrue(cmd.contains("--sponsorblock-mark all"))
        assertTrue(cmd.contains("--write-info-json"))
    }
}
