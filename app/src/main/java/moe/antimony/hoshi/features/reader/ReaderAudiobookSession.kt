package moe.antimony.hoshi.features.reader

import moe.antimony.hoshi.epub.EpubBook
import moe.antimony.hoshi.epub.SasayakiMatch
import moe.antimony.hoshi.epub.SasayakiMatchData

/** Playback the reader starts by itself once the book's audio is ready. */
enum class ReaderStartPlayback {
    /** Play from the first line at or after the position the reader opened on. */
    FromInitialPosition,

    /** Continue from the saved audio position. */
    Resume,
}

internal fun readerAudiobookStartCue(
    book: EpubBook,
    matchData: SasayakiMatchData?,
    chapterIndex: Int,
    progress: Double,
): SasayakiMatch? {
    val chapter = book.chapters.getOrNull(chapterIndex) ?: return null
    val chapterCount = book.bookInfo.chapterInfo[chapter.href]?.chapterCount ?: 0
    val offset = (progress.coerceIn(0.0, 1.0) * chapterCount).toInt()
    return matchData
        ?.matches
        ?.asSequence()
        ?.filter { it.chapterIndex == chapterIndex && it.start + it.length > offset }
        ?.minByOrNull { it.start }
}

/**
 * The line before or after the one being spoken. Line buttons always move by line, independent of
 * the Sasayaki skip-button preference that can turn the regular skip controls into time jumps.
 */
internal fun readerAudiobookAdjacentCue(
    matchData: SasayakiMatchData?,
    activeCue: SasayakiMatch?,
    playbackTime: Double,
    forward: Boolean,
): SasayakiMatch? {
    val cues = matchData?.matches.orEmpty()
    if (cues.isEmpty()) return null
    val activeIndex = activeCue?.let { active -> cues.indexOfFirst { it.id == active.id } } ?: -1
    return if (forward) {
        if (activeIndex >= 0) {
            cues.getOrNull(activeIndex + 1)
        } else {
            cues.firstOrNull { it.startTime > playbackTime }
        }
    } else {
        if (activeIndex >= 0) {
            cues[(activeIndex - 1).coerceAtLeast(0)]
        } else {
            cues.lastOrNull { it.startTime < playbackTime } ?: cues.first()
        }
    }
}

/**
 * A reader opened from the Audiobooks tab shows Audiobook mode without changing the view mode
 * saved for ordinary reading. The override ends once the user picks another mode themselves.
 */
internal data class ReaderAudiobookSession(
    val active: Boolean,
) {
    fun displayedSettings(persisted: ReaderSettings): ReaderSettings =
        if (active && persisted.viewMode != ReaderViewMode.Audiobook) {
            persisted.copy(viewMode = ReaderViewMode.Audiobook)
        } else {
            persisted
        }

    fun settingsChange(
        changed: ReaderSettings,
        persisted: ReaderSettings,
    ): ReaderAudiobookSessionChange =
        if (active && changed.viewMode == ReaderViewMode.Audiobook) {
            ReaderAudiobookSessionChange(
                session = this,
                persisted = changed.copy(viewMode = persisted.viewMode),
            )
        } else {
            ReaderAudiobookSessionChange(
                session = copy(active = false),
                persisted = changed,
            )
        }
}

internal data class ReaderAudiobookSessionChange(
    val session: ReaderAudiobookSession,
    val persisted: ReaderSettings,
)
