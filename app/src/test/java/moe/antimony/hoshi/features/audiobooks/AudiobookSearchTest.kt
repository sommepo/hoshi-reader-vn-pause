package moe.antimony.hoshi.features.audiobooks

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudiobookSearchTest {
    private val n3 = AudiobookLibraryBook(
        id = "n3",
        title = "JLPT N3 文法",
        chapters = listOf(
            chapter("042 〜てしまう", start = 0, text = "宿題を忘れてしまった"),
            chapter("142 〜わけにはいかない", start = 100, text = "休むわけにはいかない"),
            chapter("007 〜ようにする", start = 200, text = "毎日歩くようにしている"),
        ),
    )
    private val n2 = AudiobookLibraryBook(
        id = "n2",
        title = "JLPT N2 文法",
        chapters = listOf(
            chapter("042 〜にすぎない", start = 0, text = "噂にすぎない"),
        ),
    )
    private val books = listOf(n3, n2)

    @Test
    fun blankQueryHasNoResults() {
        assertEquals(emptyList<AudiobookSearchResult>(), searchAudiobookLibrary(books, "   "))
    }

    @Test
    fun idNumberFindsItsChapterWithoutLeadingZeros() {
        val results = searchAudiobookLibrary(books, "42")

        assertEquals(
            listOf("n3" to "042 〜てしまう", "n2" to "042 〜にすぎない"),
            results.map { it.bookId to it.chapter.label },
        )
    }

    @Test
    fun idNumberDoesNotMatchInsideALongerNumber() {
        assertFalse(audiobookSearchTermMatches(audiobookSearchNormalize("142 〜わけにはいかない"), "42"))
        assertTrue(audiobookSearchTermMatches(audiobookSearchNormalize("042 〜てしまう"), "4"))
        assertTrue(audiobookSearchTermMatches(audiobookSearchNormalize("007 〜ようにする"), "7"))
    }

    @Test
    fun levelAndIdNarrowToOneBook() {
        for (query in listOf("N3 42", "n3-042", "Ｎ３・０４２", "42 n3")) {
            val results = searchAudiobookLibrary(books, query)

            assertEquals(query, listOf("n3" to "042 〜てしまう"), results.map { it.bookId to it.chapter.label })
        }
    }

    @Test
    fun grammarPointInTheTitleMatches() {
        val results = searchAudiobookLibrary(books, "わけには")

        assertEquals(listOf("142 〜わけにはいかない"), results.map { it.chapter.label })
        assertFalse(results.single().matchedInText)
    }

    @Test
    fun bookTitleAloneListsEveryChapterOfThatBook() {
        val results = searchAudiobookLibrary(books, "N2")

        assertEquals(listOf("n2"), results.map { it.bookId })
    }

    @Test
    fun spokenTextMatchesRankBelowTitleMatches() {
        val library = listOf(
            n3.copy(
                chapters = listOf(
                    chapter("001 〜ている", start = 0, text = "宿題を忘れてしまった"),
                    chapter("042 〜てしまう", start = 100, text = "財布を落とした"),
                ),
            ),
        )

        val results = searchAudiobookLibrary(library, "てしまう")

        assertEquals(listOf("042 〜てしまう"), results.map { it.chapter.label })

        val textResults = searchAudiobookLibrary(library, "忘れて")
        assertEquals(listOf("001 〜ている"), textResults.map { it.chapter.label })
        assertTrue(textResults.single().matchedInText)

        val mixed = searchAudiobookLibrary(library, "て")
        assertEquals(listOf("001 〜ている", "042 〜てしまう"), mixed.map { it.chapter.label })
        assertTrue(mixed.none { it.matchedInText })
    }

    @Test
    fun everyTermMustMatch() {
        assertEquals(emptyList<AudiobookSearchResult>(), searchAudiobookLibrary(books, "N3 999"))
    }

    private fun chapter(label: String, start: Int, text: String) =
        AudiobookLibraryChapter(label = label, startCharacter = start, searchText = text)
}
