package moe.antimony.hoshi.features.audiobooks

import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import moe.antimony.hoshi.di.CacheDir
import moe.antimony.hoshi.di.IoDispatcher
import moe.antimony.hoshi.epub.BookEntry
import moe.antimony.hoshi.epub.BookRepository
import moe.antimony.hoshi.epub.BookSortOption
import moe.antimony.hoshi.epub.EpubBook
import moe.antimony.hoshi.epub.EpubBookParser
import moe.antimony.hoshi.epub.filteredReaderText
import moe.antimony.hoshi.features.reader.chapterRows

/** A book that has Sasayaki audio attached, with the chapters a listener can jump to. */
data class AudiobookLibraryBook(
    val id: String,
    val title: String,
    val chapters: List<AudiobookLibraryChapter>,
)

@Serializable
data class AudiobookLibraryChapter(
    val label: String,
    /** Whole-book character offset the chapter starts at. */
    val startCharacter: Int,
    val indentLevel: Int = 0,
    /** Chapter body text used only for search; empty when the chapter shares its file with others. */
    val searchText: String = "",
)

interface AudiobookLibrarySource {
    suspend fun loadLibrary(): List<AudiobookLibraryBook>
}

@Singleton
class AudiobookLibraryRepository internal constructor(
    private val loadEntries: suspend () -> List<BookEntry>,
    private val hasAudio: suspend (BookEntry) -> Boolean,
    private val epubSignature: suspend (BookEntry) -> String?,
    private val parseChapters: suspend (BookEntry) -> List<AudiobookLibraryChapter>,
    private val cacheDir: File,
    private val ioDispatcher: CoroutineDispatcher,
) : AudiobookLibrarySource {
    @Inject
    constructor(
        bookRepository: BookRepository,
        epubBookParser: EpubBookParser,
        @CacheDir cacheDir: File,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ) : this(
        loadEntries = { bookRepository.loadBookEntries(BookSortOption.Title) },
        hasAudio = { entry ->
            val playback = bookRepository.loadSasayakiPlayback(entry.root)
            !playback?.audioUri.isNullOrBlank() || !playback?.audioFileName.isNullOrBlank()
        },
        epubSignature = { entry ->
            bookRepository.epubFile(entry)?.let { file -> "${file.length()}:${file.lastModified()}" }
        },
        parseChapters = { entry ->
            epubBookParser
                .parse(entry.root, cachedBookInfo = bookRepository.loadReaderBookInfo(entry.root))
                .audiobookLibraryChapters()
        },
        cacheDir = File(cacheDir, "audiobook-library"),
        ioDispatcher = ioDispatcher,
    )

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun loadLibrary(): List<AudiobookLibraryBook> = withContext(ioDispatcher) {
        loadEntries()
            .filter { entry -> runCatching { hasAudio(entry) }.getOrDefault(false) }
            .map { entry ->
                AudiobookLibraryBook(
                    id = entry.metadata.id,
                    title = entry.displayTitle,
                    chapters = runCatching { chaptersFor(entry) }.getOrDefault(emptyList()),
                )
            }
    }

    private suspend fun chaptersFor(entry: BookEntry): List<AudiobookLibraryChapter> {
        val signature = epubSignature(entry)
        val cacheFile = File(cacheDir, "${entry.root.name}.json")
        if (signature != null) {
            readCache(cacheFile)
                ?.takeIf { it.version == CacheVersion && it.signature == signature }
                ?.let { return it.chapters }
        }
        val chapters = parseChapters(entry)
        if (signature != null) {
            writeCache(cacheFile, CachedChapters(CacheVersion, signature, chapters))
        }
        return chapters
    }

    private fun readCache(file: File): CachedChapters? =
        runCatching {
            if (!file.isFile) return null
            json.decodeFromString(CachedChapters.serializer(), file.readText())
        }.getOrNull()

    private fun writeCache(file: File, cached: CachedChapters) {
        runCatching {
            file.parentFile?.mkdirs()
            file.writeText(json.encodeToString(CachedChapters.serializer(), cached))
        }
    }

    @Serializable
    private data class CachedChapters(
        val version: Int,
        val signature: String,
        val chapters: List<AudiobookLibraryChapter>,
    )

    private companion object {
        const val CacheVersion = 1
    }
}

private const val AudiobookSearchTextMaxLength = 6000

internal fun EpubBook.audiobookLibraryChapters(): List<AudiobookLibraryChapter> {
    val rows = chapterRows(currentCharacter = 0).filter { it.characterCount != null }
    val rowsPerSpine = rows.groupingBy { it.spineIndex }.eachCount()
    return rows.map { row ->
        val ownsWholeFile = row.fragment == null && rowsPerSpine[row.spineIndex] == 1
        AudiobookLibraryChapter(
            label = row.label.trim(),
            startCharacter = requireNotNull(row.characterCount),
            indentLevel = row.indentLevel,
            searchText = if (ownsWholeFile) {
                chapters.getOrNull(row.spineIndex)
                    ?.html
                    ?.filteredReaderText()
                    ?.take(AudiobookSearchTextMaxLength)
                    .orEmpty()
            } else {
                ""
            },
        )
    }
}
