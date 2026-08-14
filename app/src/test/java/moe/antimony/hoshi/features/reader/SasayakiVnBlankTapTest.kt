package moe.antimony.hoshi.features.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SasayakiVnBlankTapTest {
    @Test
    fun blankTapTogglesPlaybackWhileAudioIsOnTheCurrentScreen() {
        assertEquals(
            SasayakiVnBlankTapResult.TogglePlayback,
            result(waitingForContinue = false),
        )
    }

    @Test
    fun blankTapAdvancesTheScreenAfterThatScreensAudioEnds() {
        assertEquals(
            SasayakiVnBlankTapResult.AdvanceScreen,
            result(waitingForContinue = true),
        )
    }

    @Test
    fun blankTapDismissesLookupWithoutChangingPlayback() {
        assertEquals(
            SasayakiVnBlankTapResult.DismissLookup,
            result(waitingForContinue = false, lookupVisible = true),
        )
        assertEquals(
            SasayakiVnBlankTapResult.DismissLookup,
            result(waitingForContinue = true, lookupVisible = true),
        )
    }

    @Test
    fun blankTapIgnoresPushToContinueOutsideVisualNovelPlayback() {
        assertEquals(
            SasayakiVnBlankTapResult.Ignored,
            result(viewMode = ReaderViewMode.Paginated, waitingForContinue = true),
        )
        assertEquals(
            SasayakiVnBlankTapResult.Ignored,
            result(pauseAtScreenEnd = false, waitingForContinue = true),
        )
        assertFalse(
            sasayakiBindsAudioToVisibleVnScreen(
                pauseAtScreenEnd = true,
                viewMode = ReaderViewMode.VisualNovel,
                hasPlayer = false,
            ),
        )
        assertTrue(
            sasayakiBindsAudioToVisibleVnScreen(
                pauseAtScreenEnd = true,
                viewMode = ReaderViewMode.VisualNovel,
                hasPlayer = true,
            ),
        )
    }

    private fun result(
        pauseAtScreenEnd: Boolean = true,
        viewMode: ReaderViewMode = ReaderViewMode.VisualNovel,
        hasPlayer: Boolean = true,
        waitingForContinue: Boolean = false,
        lookupVisible: Boolean = false,
    ): SasayakiVnBlankTapResult = sasayakiVnBlankTapResult(
        pauseAtScreenEnd = pauseAtScreenEnd,
        viewMode = viewMode,
        hasPlayer = hasPlayer,
        waitingForContinue = waitingForContinue,
        lookupVisible = lookupVisible,
    )
}
