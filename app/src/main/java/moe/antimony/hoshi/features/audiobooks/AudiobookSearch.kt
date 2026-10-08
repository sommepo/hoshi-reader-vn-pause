package moe.antimony.hoshi.features.audiobooks

import java.text.Normalizer
import java.util.Locale

data class AudiobookSearchResult(
    val bookId: String,
    val bookTitle: String,
    val chapter: AudiobookLibraryChapter,
    /** True when the query was only found in the chapter's spoken text, not in a title. */
    val matchedInText: Boolean,
)

private val SearchSeparators = Regex("[\\s\\-_.:/#・,，、()（）\\[\\]【】「」]+")
private val DigitRuns = Regex("\\d+")

internal fun audiobookSearchNormalize(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFKC).lowercase(Locale.ROOT)

/**
 * Splits a query into terms that must all match. Separators are dropped so "N3-042", "n3 42"
 * and "N3・042" ask for the same thing.
 */
internal fun audiobookSearchTerms(query: String): List<String> =
    audiobookSearchNormalize(query)
        .split(SearchSeparators)
        .filter { it.isNotEmpty() }

/** [text] must already be normalized with [audiobookSearchNormalize]. */
internal fun audiobookSearchTermMatches(text: String, term: String): Boolean {
    if (term.all(Char::isDigit)) {
        // Numbers compare by value from the left, so "42" finds ID 042 but not 142.
        val wanted = term.trimStart('0')
        return DigitRuns.findAll(text).any { run ->
            val digits = run.value.trimStart('0')
            if (wanted.isEmpty()) digits.isEmpty() else digits.startsWith(wanted)
        }
    }
    return text.contains(term)
}

internal fun searchAudiobookLibrary(
    books: List<AudiobookLibraryBook>,
    query: String,
): List<AudiobookSearchResult> {
    val terms = audiobookSearchTerms(query)
    if (terms.isEmpty()) return emptyList()
    val ranked = mutableListOf<Pair<Int, AudiobookSearchResult>>()
    for (book in books) {
        val title = audiobookSearchNormalize(book.title)
        for (chapter in book.chapters) {
            val label = audiobookSearchNormalize(chapter.label)
            val text by lazy(LazyThreadSafetyMode.NONE) { audiobookSearchNormalize(chapter.searchText) }
            var needsText = false
            var labelHits = 0
            val matches = terms.all { term ->
                when {
                    audiobookSearchTermMatches(label, term) -> {
                        labelHits += 1
                        true
                    }
                    audiobookSearchTermMatches(title, term) -> true
                    chapter.searchText.isNotEmpty() && audiobookSearchTermMatches(text, term) -> {
                        needsText = true
                        true
                    }
                    else -> false
                }
            }
            if (!matches) continue
            val rank = when {
                needsText -> 3
                labelHits == terms.size -> 0
                labelHits > 0 -> 1
                else -> 2
            }
            ranked += rank to AudiobookSearchResult(
                bookId = book.id,
                bookTitle = book.title,
                chapter = chapter,
                matchedInText = needsText,
            )
        }
    }
    return ranked.sortedBy { it.first }.map { it.second }
}
