package moe.antimony.hoshi.navigation

import moe.antimony.hoshi.epub.BookInfo
import moe.antimony.hoshi.epub.Bookmark
import moe.antimony.hoshi.epub.EpubBook
import moe.antimony.hoshi.epub.EpubChapter
import moe.antimony.hoshi.features.bookshelf.MainTab
import moe.antimony.hoshi.features.reader.ReaderChapterPosition
import moe.antimony.hoshi.features.reader.ReaderStartPlayback
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderRouteAudiobookLaunchTest {
    private val bookmark = Bookmark(chapterIndex = 1, progress = 0.5, characterCount = 10)

    @Test
    fun readerOpensAtTheBookmarkUnlessAChapterWasChosen() {
        assertEquals(
            ReaderChapterPosition(index = 1, progress = 0.5),
            readerRouteInitialPosition(book(), bookmark, startCharacter = null),
        )
        assertEquals(
            ReaderChapterPosition(index = 0, progress = 0.0),
            readerRouteInitialPosition(book(), bookmark = null, startCharacter = null),
        )
    }

    @Test
    fun aChosenChapterOverridesTheBookmark() {
        assertEquals(
            ReaderChapterPosition(index = 1, progress = 0.0),
            readerRouteInitialPosition(book(), bookmark, startCharacter = 5),
        )
        assertEquals(
            ReaderChapterPosition(index = 0, progress = 0.0),
            readerRouteInitialPosition(book(), bookmark, startCharacter = 0),
        )
    }

    @Test
    fun autoplayStartsAtTheChosenChapterOrResumesTheBook() {
        assertNull(readerRouteStartPlayback(autoplay = false, startCharacter = 5))
        assertEquals(
            ReaderStartPlayback.FromInitialPosition,
            readerRouteStartPlayback(autoplay = true, startCharacter = 5),
        )
        assertEquals(ReaderStartPlayback.Resume, readerRouteStartPlayback(autoplay = true, startCharacter = null))
    }

    @Test
    fun audiobooksIsAMainShellTabNextToBooks() {
        assertTrue(appRouteUsesMainShell(AppRoute.AudiobooksRoute))
        assertEquals(1, MainTab.entries.indexOf(MainTab.Audiobooks))
    }

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
