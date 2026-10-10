package moe.antimony.hoshi.importing

import java.text.Normalizer
import java.util.Locale

/** An EPUB found in an imported folder, with the subtitle and audiobook that belong to it. */
internal data class BookImportBundle<T>(
    val book: ImportDirectoryFile<T>,
    val subtitle: ImportDirectoryFile<T>? = null,
    val audio: ImportDirectoryFile<T>? = null,
)

/**
 * Pairs each EPUB with the subtitle and audiobook stored next to it. A companion belongs to a book
 * when, in the same folder, it has the same file name, a file name that extends the book's (or the
 * reverse, like `book.ja.srt`), the same leading ID number, or is the only candidate beside the
 * only book. A companion is never given to two books; an ambiguous one is left out.
 */
internal fun <T> planBookImportBundles(
    books: List<ImportDirectoryFile<T>>,
    subtitles: List<ImportDirectoryFile<T>>,
    audio: List<ImportDirectoryFile<T>>,
): List<BookImportBundle<T>> {
    val subtitleByBook = pairCompanions(books, subtitles)
    val audioByBook = pairCompanions(books, audio)
    return books.map { book ->
        BookImportBundle(
            book = book,
            subtitle = subtitleByBook[book],
            audio = audioByBook[book],
        )
    }
}

private fun <T> pairCompanions(
    books: List<ImportDirectoryFile<T>>,
    companions: List<ImportDirectoryFile<T>>,
): Map<ImportDirectoryFile<T>, ImportDirectoryFile<T>> {
    val paired = mutableMapOf<ImportDirectoryFile<T>, ImportDirectoryFile<T>>()
    val companionsByDirectory = companions.groupBy { it.directory() }
    books.groupBy { it.directory() }.forEach { (directory, directoryBooks) ->
        val remainingBooks = directoryBooks.toMutableList()
        val remainingCompanions = companionsByDirectory[directory].orEmpty().toMutableList()
        CompanionRules.forEach { rule ->
            // Repeat a rule while it settles pairs: taking "book 2.srt" leaves "book.srt" unambiguous.
            do {
                // Collect first, then commit, so one pass cannot hand a companion to two books.
                val candidates = remainingBooks.mapNotNull { book ->
                    remainingCompanions
                        .mapNotNull { companion -> rule(book.stem(), companion.stem())?.let { companion to it } }
                        .singleOrNull()
                        ?.let { (companion, closeness) -> Candidate(book, companion, closeness) }
                }
                // Two books wanting one companion: the closer name wins, and a tie leaves it out.
                val settled = candidates.groupBy { it.companion }.values.mapNotNull { contenders ->
                    val closest = contenders.maxOf { it.closeness }
                    contenders.singleOrNull { it.closeness == closest }
                }
                settled.forEach { candidate ->
                    paired[candidate.book] = candidate.companion
                    remainingBooks -= candidate.book
                    remainingCompanions -= candidate.companion
                }
            } while (settled.isNotEmpty())
        }
        if (remainingBooks.size == 1 && directoryBooks.size == 1 && remainingCompanions.size == 1) {
            paired[remainingBooks.single()] = remainingCompanions.single()
        }
    }
    return paired
}

private data class Candidate<T>(
    val book: ImportDirectoryFile<T>,
    val companion: ImportDirectoryFile<T>,
    val closeness: Int,
)

/** Each rule returns how close the two names are, or null when they do not go together. */
private val CompanionRules: List<(String, String) -> Int?> = listOf(
    { book, companion -> if (book == companion) 0 else null },
    { book, companion ->
        if (companion.extends(book) || book.extends(companion)) minOf(book.length, companion.length) else null
    },
    { book, companion -> if (book.leadingId().let { it != null && it == companion.leadingId() }) 0 else null },
)

private val StemSeparators = charArrayOf('.', '-', '_', ' ', '(', '[', '（', '【')
private val LeadingId = Regex("^\\D{0,3}(\\d+)")

private fun String.extends(other: String): Boolean =
    length > other.length && startsWith(other) && this[other.length] in StemSeparators

private fun String.leadingId(): String? =
    LeadingId.find(this)?.groupValues?.get(1)?.trimStart('0')

private fun ImportDirectoryFile<*>.directory(): String =
    displayName.substringBeforeLast('/', missingDelimiterValue = "")

private fun ImportDirectoryFile<*>.stem(): String =
    Normalizer.normalize(
        displayName.substringAfterLast('/').substringBeforeLast('.'),
        Normalizer.Form.NFKC,
    ).lowercase(Locale.ROOT).trim()
