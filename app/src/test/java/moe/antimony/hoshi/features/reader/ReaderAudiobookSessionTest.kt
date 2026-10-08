package moe.antimony.hoshi.features.reader

import moe.antimony.hoshi.epub.BookInfo
import moe.antimony.hoshi.epub.EpubBook
import moe.antimony.hoshi.epub.EpubChapter
import moe.antimony.hoshi.epub.SasayakiMatch
import moe.antimony.hoshi.epub.SasayakiMatchData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderAudiobookSessionTest {
    private val novelSettings = ReaderSettings(viewMode = ReaderViewMode.Paginated, verticalWriting = true)

    @Test
    fun audiobookSessionShowsAudiobookModeOverTheSavedMode() {
        val session = ReaderAudiobookSession(active = true)

        assertEquals(ReaderViewMode.Audiobook, session.displayedSettings(novelSettings).viewMode)
        assertEquals(novelSettings, ReaderAudiobookSession(active = false).displayedSettings(novelSettings))
    }

    @Test
    fun changingOtherSettingsInAnAudiobookSessionKeepsTheSavedMode() {
        val session = ReaderAudiobookSession(active = true)
        val changed = session.displayedSettings(novelSettings).copy(fontSize = 30)

        val change = session.settingsChange(changed = changed, persisted = novelSettings)

        assertTrue(change.session.active)
        assertEquals(novelSettings.copy(fontSize = 30), change.persisted)
    }

    @Test
    fun pickingAnotherModeEndsTheSessionAndSavesThatMode() {
        val session = ReaderAudiobookSession(active = true)
        val changed = session.displayedSettings(novelSettings).copy(viewMode = ReaderViewMode.Continuous)

        val change = session.settingsChange(changed = changed, persisted = novelSettings)

        assertFalse(change.session.active)
        assertEquals(ReaderViewMode.Continuous, change.persisted.viewMode)
        assertEquals(ReaderViewMode.Continuous, change.session.displayedSettings(change.persisted).viewMode)
    }

    @Test
    fun choosingAudiobookModeOutsideASessionIsSavedLikeAnyOtherMode() {
        val session = ReaderAudiobookSession(active = false)

        val change = session.settingsChange(
            changed = novelSettings.copy(viewMode = ReaderViewMode.Audiobook),
            persisted = novelSettings,
        )

        assertEquals(ReaderViewMode.Audiobook, change.persisted.viewMode)
    }

    @Test
    fun audiobookModeLaysOutHorizontallyWithoutChangingTheSavedDirection() {
        val audiobook = novelSettings.copy(viewMode = ReaderViewMode.Audiobook)

        assertFalse(audiobook.layoutSettings.verticalWriting)
        assertTrue(audiobook.verticalWriting)
        assertTrue(audiobook.continuousMode)
        assertEquals(0.0, audiobook.layoutSettings.continuousViewportHorizontalPaddingRatio, 0.0)
        assertEquals(novelSettings, novelSettings.layoutSettings)
    }

    @Test
    fun audiobookModeRoundTripsThroughStorageValues() {
        assertEquals(ReaderViewMode.Audiobook, ReaderViewMode.fromStorage(ReaderViewMode.Audiobook.rawValue))
        assertTrue(ReaderViewMode.Audiobook.usesContinuousScroll)
        assertTrue(ReaderViewMode.Continuous.usesContinuousScroll)
        assertFalse(ReaderViewMode.VisualNovel.usesContinuousScroll)
        assertFalse(ReaderViewMode.Paginated.usesContinuousScroll)
    }

    @Test
    fun startCueIsTheFirstLineAtOrAfterTheOpeningPosition() {
        val match = SasayakiMatchData(
            matches = listOf(
                cue("late", chapterIndex = 1, start = 6, length = 4),
                cue("early", chapterIndex = 1, start = 0, length = 6),
                cue("other", chapterIndex = 0, start = 0, length = 5),
            ),
            unmatched = 0,
        )

        assertEquals("early", readerAudiobookStartCue(book(), match, chapterIndex = 1, progress = 0.0)?.id)
        assertEquals("early", readerAudiobookStartCue(book(), match, chapterIndex = 1, progress = 0.5)?.id)
        assertEquals("late", readerAudiobookStartCue(book(), match, chapterIndex = 1, progress = 0.6)?.id)
        assertEquals("other", readerAudiobookStartCue(book(), match, chapterIndex = 0, progress = 0.0)?.id)
    }

    @Test
    fun startCueIsMissingWhenTheChapterHasNoAudioLines() {
        val match = SasayakiMatchData(
            matches = listOf(cue("other", chapterIndex = 0, start = 0, length = 5)),
            unmatched = 0,
        )

        assertNull(readerAudiobookStartCue(book(), match, chapterIndex = 1, progress = 0.0))
        assertNull(readerAudiobookStartCue(book(), null, chapterIndex = 0, progress = 0.0))
        assertNull(readerAudiobookStartCue(book(), match, chapterIndex = 9, progress = 0.0))
    }

    @Test
    fun lineButtonsMoveToTheNeighbouringLineOfTheSpokenOne() {
        val first = cue("first", chapterIndex = 0, start = 0, length = 2, startTime = 1.0)
        val second = cue("second", chapterIndex = 0, start = 2, length = 2, startTime = 4.0)
        val third = cue("third", chapterIndex = 1, start = 0, length = 2, startTime = 9.0)
        val match = SasayakiMatchData(matches = listOf(first, second, third), unmatched = 0)

        assertEquals(third, readerAudiobookAdjacentCue(match, second, playbackTime = 5.0, forward = true))
        assertEquals(first, readerAudiobookAdjacentCue(match, second, playbackTime = 5.0, forward = false))
        assertEquals(first, readerAudiobookAdjacentCue(match, first, playbackTime = 1.5, forward = false))
        assertNull(readerAudiobookAdjacentCue(match, third, playbackTime = 9.5, forward = true))
    }

    @Test
    fun lineButtonsUseThePlaybackTimeBetweenLines() {
        val first = cue("first", chapterIndex = 0, start = 0, length = 2, startTime = 1.0)
        val second = cue("second", chapterIndex = 0, start = 2, length = 2, startTime = 4.0)
        val match = SasayakiMatchData(matches = listOf(first, second), unmatched = 0)

        assertEquals(second, readerAudiobookAdjacentCue(match, null, playbackTime = 3.0, forward = true))
        assertEquals(first, readerAudiobookAdjacentCue(match, null, playbackTime = 3.0, forward = false))
        assertEquals(first, readerAudiobookAdjacentCue(match, null, playbackTime = 0.0, forward = false))
        assertNull(readerAudiobookAdjacentCue(match, null, playbackTime = 8.0, forward = true))
        assertNull(readerAudiobookAdjacentCue(null, null, playbackTime = 0.0, forward = true))
    }

    @Test
    fun transportBarShowsOnlyForAudiobookModeWithPlayableAudio() {
        val metrics = readerBottomChromeMetrics(bottomSafeAreaDp = 40)
        fun transport(
            viewMode: ReaderViewMode = ReaderViewMode.Audiobook,
            enabled: Boolean = true,
            hasAudio: Boolean = true,
        ) = readerAudiobookTransport(viewMode, enabled, hasAudio, metrics)

        assertTrue(transport().visible)
        assertFalse(transport(viewMode = ReaderViewMode.VisualNovel).visible)
        assertFalse(transport(enabled = false).visible)
        assertFalse(transport(hasAudio = false).visible)
        assertEquals(0, transport(hasAudio = false).reservedBottomDp)
        assertEquals(0, transport(hasAudio = false).contentOverlapDp)
    }

    @Test
    fun transportButtonsAreLargeTouchTargetsAndOverlapIsIndependentOfTheSafeArea() {
        val small = readerAudiobookTransport(
            ReaderViewMode.Audiobook, true, true, readerBottomChromeMetrics(bottomSafeAreaDp = 20),
        )
        val large = readerAudiobookTransport(
            ReaderViewMode.Audiobook, true, true, readerBottomChromeMetrics(bottomSafeAreaDp = 60),
        )

        assertTrue(small.tertiaryButtonSizeDp >= 48)
        assertTrue(small.secondaryButtonSizeDp > small.tertiaryButtonSizeDp)
        assertTrue(small.primaryButtonSizeDp > small.secondaryButtonSizeDp)
        assertTrue(small.barHeightDp >= small.primaryButtonSizeDp)
        assertEquals(small.contentOverlapDp, large.contentOverlapDp)
        assertEquals(small.contentOverlapDp, readerAudiobookDefaultContentOverlapDp())
        assertEquals(large.reservedBottomDp - large.bottomSafeAreaDp, large.contentOverlapDp)
    }

    @Test
    fun smallSafeAreaControlsStepAsideForTheTransportBar() {
        val settings = moe.antimony.hoshi.features.sasayaki.SasayakiSettings(
            enabled = true,
            showReaderBottomPlaybackControls = true,
        )
        val metrics = readerBottomChromeMetrics()

        assertFalse(
            readerSasayakiBottomPlaybackControls(settings, hasAudio = true, metrics, ReaderViewMode.Audiobook).visible,
        )
        assertTrue(
            readerSasayakiBottomPlaybackControls(settings, hasAudio = true, metrics, ReaderViewMode.Continuous).visible,
        )
    }

    private fun cue(id: String, chapterIndex: Int, start: Int, length: Int, startTime: Double = 0.0) =
        SasayakiMatch(
            id = id,
            startTime = startTime,
            endTime = startTime + 1.0,
            text = id,
            chapterIndex = chapterIndex,
            start = start,
            length = length,
        )

    private fun book(): EpubBook {
        val chapters = listOf("a.xhtml", "b.xhtml").map { href ->
            EpubChapter(id = href, href = href, mediaType = "application/xhtml+xml", html = "")
        }
        return EpubBook(
            title = "Book",
            chapters = chapters,
            bookInfo = BookInfo(
                characterCount = 15,
                chapterInfo = mapOf(
                    "a.xhtml" to BookInfo.ChapterInfo(spineIndex = 0, currentTotal = 0, chapterCount = 5),
                    "b.xhtml" to BookInfo.ChapterInfo(spineIndex = 1, currentTotal = 5, chapterCount = 10),
                ),
                images = emptyList(),
            ),
        )
    }
}
