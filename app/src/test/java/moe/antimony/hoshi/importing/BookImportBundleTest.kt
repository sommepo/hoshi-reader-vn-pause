package moe.antimony.hoshi.importing

import org.junit.Assert.assertEquals
import org.junit.Test

class BookImportBundleTest {
    @Test
    fun pairsCompanionsThatShareTheBookFileName() {
        val bundles = plan(
            books = listOf("521 戻れるものなら.epub", "513 万が一のために.epub"),
            subtitles = listOf("513 万が一のために.srt", "521 戻れるものなら.SRT"),
            audio = listOf("521 戻れるものなら.m4b", "513 万が一のために.mp3"),
        )

        assertEquals(
            listOf(
                Triple("521 戻れるものなら.epub", "521 戻れるものなら.SRT", "521 戻れるものなら.m4b"),
                Triple("513 万が一のために.epub", "513 万が一のために.srt", "513 万が一のために.mp3"),
            ),
            bundles,
        )
    }

    @Test
    fun pairsCompanionsWhoseNameExtendsTheBookName() {
        val bundles = plan(
            books = listOf("book.epub", "book 2.epub"),
            subtitles = listOf("book.ja.srt", "book 2.ja.srt"),
            audio = listOf("book 2 (audio).opus"),
        )

        assertEquals(
            listOf(
                Triple("book.epub", "book.ja.srt", null),
                Triple("book 2.epub", "book 2.ja.srt", "book 2 (audio).opus"),
            ),
            bundles,
        )
    }

    @Test
    fun pairsByLeadingIdWhenTitlesDiffer() {
        val bundles = plan(
            books = listOf("0042 dialogue.epub", "142 other.epub"),
            subtitles = listOf("42.srt", "142.srt"),
            audio = listOf("N3 skipped.m4b"),
        )

        assertEquals(
            listOf(
                Triple("0042 dialogue.epub", "42.srt", null),
                Triple("142 other.epub", "142.srt", null),
            ),
            bundles,
        )
    }

    @Test
    fun pairsTheOnlyCompanionBesideTheOnlyBookInAFolder() {
        val bundles = plan(
            books = listOf("Novel/Novel.epub", "Other/a.epub", "Other/b.epub"),
            subtitles = listOf("Novel/aligned.srt", "Other/unrelated.srt"),
            audio = listOf("Novel/full recording.m4b", "elsewhere.m4b"),
        )

        assertEquals(
            listOf(
                Triple("Novel/Novel.epub", "Novel/aligned.srt", "Novel/full recording.m4b"),
                Triple("Other/a.epub", null, null),
                Triple("Other/b.epub", null, null),
            ),
            bundles,
        )
    }

    @Test
    fun leavesOutACompanionThatFitsMoreThanOneBook() {
        val bundles = plan(
            books = listOf("7 first.epub", "7 second.epub"),
            subtitles = listOf("7.srt"),
            audio = emptyList(),
        )

        assertEquals(
            listOf(
                Triple("7 first.epub", null, null),
                Triple("7 second.epub", null, null),
            ),
            bundles,
        )
    }

    private fun plan(
        books: List<String>,
        subtitles: List<String>,
        audio: List<String>,
    ): List<Triple<String, String?, String?>> =
        planBookImportBundles(
            books = books.map(::file),
            subtitles = subtitles.map(::file),
            audio = audio.map(::file),
        ).map { Triple(it.book.displayName, it.subtitle?.displayName, it.audio?.displayName) }

    private fun file(name: String) = ImportDirectoryFile(key = name, displayName = name)
}
