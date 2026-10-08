package moe.antimony.hoshi.features.audiobooks

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import moe.antimony.hoshi.epub.BookEntry
import moe.antimony.hoshi.epub.BookInfo
import moe.antimony.hoshi.epub.BookMetadata
import moe.antimony.hoshi.epub.EpubBook
import moe.antimony.hoshi.epub.EpubChapter
import moe.antimony.hoshi.epub.EpubTocItem
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AudiobookLibraryRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val chapters = listOf(AudiobookLibraryChapter(label = "042 〜てしまう", startCharacter = 0))

    @Test
    fun onlyBooksWithAudioAreListed() = runBlocking {
        val withAudio = entry("with-audio", "N3 文法")
        val silent = entry("silent", "小説")
        val repository = repository(
            entries = listOf(withAudio, silent),
            hasAudio = { it == withAudio },
        )

        val library = repository.loadLibrary()

        assertEquals(listOf(AudiobookLibraryBook(id = "with-audio", title = "N3 文法", chapters = chapters)), library)
    }

    @Test
    fun chaptersAreParsedOnceWhileTheEpubIsUnchanged() = runBlocking {
        var parses = 0
        var signature = "10:1"
        val repository = repository(
            entries = listOf(entry("book", "N3 文法")),
            signature = { signature },
            parse = {
                parses += 1
                chapters
            },
        )

        assertEquals(chapters, repository.loadLibrary().single().chapters)
        assertEquals(chapters, repository.loadLibrary().single().chapters)
        assertEquals(1, parses)

        signature = "12:2"
        repository.loadLibrary()
        assertEquals(2, parses)
    }

    @Test
    fun aBookThatCannotBeParsedIsStillListedWithoutChapters() = runBlocking {
        val repository = repository(
            entries = listOf(entry("book", "N3 文法")),
            parse = { error("broken epub") },
        )

        assertEquals(
            listOf(AudiobookLibraryBook(id = "book", title = "N3 文法", chapters = emptyList())),
            repository.loadLibrary(),
        )
    }

    @Test
    fun libraryChaptersFollowTheTableOfContentsAndIndexWholeFileText() {
        val first = EpubChapter(id = "a", href = "a.xhtml", mediaType = "application/xhtml+xml", html = "<body><p>忘れてしまった。</p></body>")
        val second = EpubChapter(id = "b", href = "b.xhtml", mediaType = "application/xhtml+xml", html = "<body><p>一二三四</p></body>")
        val book = EpubBook(
            title = "N3",
            chapters = listOf(first, second),
            toc = listOf(
                EpubTocItem(label = " 042 〜てしまう ", href = "a.xhtml"),
                EpubTocItem(
                    label = "043",
                    href = "b.xhtml",
                    children = listOf(EpubTocItem(label = "043 続き", href = "b.xhtml#more")),
                ),
            ),
            bookInfo = BookInfo(
                characterCount = 11,
                chapterInfo = mapOf(
                    "a.xhtml" to BookInfo.ChapterInfo(spineIndex = 0, currentTotal = 0, chapterCount = 7),
                    "b.xhtml" to BookInfo.ChapterInfo(
                        spineIndex = 1,
                        currentTotal = 7,
                        chapterCount = 4,
                        fragmentOffsets = mapOf("more" to 2),
                    ),
                ),
                images = emptyList(),
            ),
        )

        val chapters = book.audiobookLibraryChapters()

        assertEquals(listOf("042 〜てしまう", "043", "043 続き"), chapters.map { it.label })
        assertEquals(listOf(0, 7, 9), chapters.map { it.startCharacter })
        assertEquals(listOf(0, 0, 1), chapters.map { it.indentLevel })
        assertEquals(listOf("忘れてしまった", "", ""), chapters.map { it.searchText })
    }

    private fun repository(
        entries: List<BookEntry>,
        hasAudio: (BookEntry) -> Boolean = { true },
        signature: () -> String? = { "10:1" },
        parse: () -> List<AudiobookLibraryChapter> = { chapters },
    ) = AudiobookLibraryRepository(
        loadEntries = { entries },
        hasAudio = { hasAudio(it) },
        epubSignature = { signature() },
        parseChapters = { parse() },
        cacheDir = File(temporaryFolder.root, "audiobook-library"),
        ioDispatcher = Dispatchers.Unconfined,
    )

    private fun entry(id: String, title: String) =
        BookEntry(
            root = File(temporaryFolder.root, id),
            metadata = BookMetadata(id = id, title = title, cover = null, folder = id, lastAccess = 0.0),
        )
}
