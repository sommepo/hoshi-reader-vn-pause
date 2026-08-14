package moe.antimony.hoshi.features.sasayaki

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SasayakiScreenAwakeTest {
    @Test
    fun keepsScreenAwakeOnlyWhilePlaybackAndAutoScrollAreActiveLikeIos() {
        assertTrue(SasayakiScreenAwake.shouldKeepScreenOn(isPlaying = true, autoScroll = true))
        assertFalse(SasayakiScreenAwake.shouldKeepScreenOn(isPlaying = false, autoScroll = true))
        assertFalse(SasayakiScreenAwake.shouldKeepScreenOn(isPlaying = true, autoScroll = false))
        assertFalse(SasayakiScreenAwake.shouldKeepScreenOn(isPlaying = false, autoScroll = false))
        assertTrue(
            SasayakiScreenAwake.shouldKeepScreenOn(
                isPlaying = false,
                autoScroll = true,
                waitingForContinue = true,
            ),
        )
    }
}
