package moe.antimony.hoshi.features.sasayaki

import java.io.File
import kotlinx.coroutines.runBlocking
import moe.antimony.hoshi.epub.EpubBook
import moe.antimony.hoshi.epub.EpubChapter
import moe.antimony.hoshi.epub.SasayakiMatchData
import moe.antimony.hoshi.epub.SasayakiPlaybackData
import moe.antimony.hoshi.epub.SasayakiSidecarRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SasayakiCompanionImporterTest {
    private val bookRoot = File("book")
    private val book = EpubBook(
        title = "Book",
        chapters = listOf(
            EpubChapter(
                id = "chapter-1",
                href = "chapter-1.xhtml",
                mediaType = "application/xhtml+xml",
                html = "<html><body><p>それはある日の日常の光景だった。</p></body></html>",
            ),
        ),
    )
    private val subtitle = "1\n00:00:01,000 --> 00:00:03,000\nそれはある日の日常の光景だった。\n".toByteArray()

    @Test
    fun matchesTheSubtitleAndLinksTheAudiobook() = runBlocking {
        val sidecars = FakeSidecars()

        sidecars.attachSasayakiCompanions(
            bookRoot = bookRoot,
            book = book,
            readSubtitle = { subtitle },
            audioUri = "content://folder/book.m4b",
            copyAudio = { null },
        )

        assertEquals(1, sidecars.match?.matches?.size)
        assertEquals(1.0, sidecars.match?.matches?.single()?.startTime)
        assertEquals("content://folder/book.m4b", sidecars.playback?.audioUri)
        assertNull(sidecars.playback?.audioFileName)
    }

    @Test
    fun recordsTheCopiedAudiobookInsteadOfTheFolderUri() = runBlocking {
        val sidecars = FakeSidecars()

        sidecars.attachSasayakiCompanions(
            bookRoot = bookRoot,
            book = book,
            readSubtitle = null,
            audioUri = "content://folder/book.m4b",
            copyAudio = { "sasayaki_audio.m4b" },
        )

        assertNull(sidecars.match)
        assertNull(sidecars.playback?.audioUri)
        assertEquals("sasayaki_audio.m4b", sidecars.playback?.audioFileName)
    }

    @Test
    fun keepsAnExistingMatchAndAudioSourceWhenTheFolderIsImportedAgain() = runBlocking {
        val existingMatch = SasayakiMatchData(matches = emptyList(), unmatched = 3)
        val existingPlayback = SasayakiPlaybackData(lastPosition = 42.0, audioFileName = "sasayaki_audio.mp3")
        val sidecars = FakeSidecars(match = existingMatch, playback = existingPlayback)

        sidecars.attachSasayakiCompanions(
            bookRoot = bookRoot,
            book = book,
            readSubtitle = { error("An existing match must not be replaced.") },
            audioUri = "content://folder/book.m4b",
            copyAudio = { error("An existing audiobook must not be replaced.") },
        )

        assertEquals(existingMatch, sidecars.match)
        assertEquals(existingPlayback, sidecars.playback)
    }

    @Test
    fun keepsTheSavedPositionWhenAttachingAudioToABookThatHadNone() = runBlocking {
        val sidecars = FakeSidecars(playback = SasayakiPlaybackData(lastPosition = 12.5, rate = 1.25f))

        sidecars.attachSasayakiCompanions(
            bookRoot = bookRoot,
            book = book,
            readSubtitle = null,
            audioUri = "content://folder/book.m4b",
            copyAudio = { null },
        )

        assertEquals(
            SasayakiPlaybackData(lastPosition = 12.5, rate = 1.25f, audioUri = "content://folder/book.m4b"),
            sidecars.playback,
        )
    }

    private class FakeSidecars(
        var match: SasayakiMatchData? = null,
        var playback: SasayakiPlaybackData? = null,
    ) : SasayakiSidecarRepository {
        override suspend fun loadSasayakiMatch(bookRoot: File): SasayakiMatchData? = match

        override suspend fun saveSasayakiMatch(bookRoot: File, match: SasayakiMatchData) {
            this.match = match
        }

        override suspend fun loadSasayakiPlayback(bookRoot: File): SasayakiPlaybackData? = playback

        override suspend fun saveSasayakiPlayback(bookRoot: File, playback: SasayakiPlaybackData) {
            this.playback = playback
        }
    }
}
