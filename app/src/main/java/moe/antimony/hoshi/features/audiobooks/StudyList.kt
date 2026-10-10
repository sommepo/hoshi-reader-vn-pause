package moe.antimony.hoshi.features.audiobooks

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import moe.antimony.hoshi.di.IoDispatcher

/**
 * The ID numbers a study tool says were studied today, used by the Audiobooks tab to put the
 * matching chapters first.
 */
interface StudyListSource {
    /** The link the list is read from; blank when none is set. */
    val url: Flow<String>

    suspend fun setUrl(url: String)

    /** Today's IDs in study order. Throws when the list cannot be read. */
    suspend fun loadToday(url: String): List<String>
}

@Singleton
class StudyListRepository internal constructor(
    private val dataStore: DataStore<Preferences>,
    private val today: () -> LocalDate,
    private val fetch: (String) -> String,
    private val ioDispatcher: CoroutineDispatcher,
) : StudyListSource {
    @Inject
    constructor(
        dataStore: StudyListDataStore,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ) : this(
        dataStore = dataStore.dataStore,
        today = LocalDate::now,
        fetch = ::fetchStudyList,
        ioDispatcher = ioDispatcher,
    )

    override val url: Flow<String> = dataStore.data.map { it[KEY_URL].orEmpty() }

    override suspend fun setUrl(url: String) {
        dataStore.edit { it[KEY_URL] = url.trim() }
    }

    override suspend fun loadToday(url: String): List<String> = withContext(ioDispatcher) {
        parseStudyListIds(fetch(studyListRequestUrl(url, today())))
    }

    private companion object {
        val KEY_URL = stringPreferencesKey("studyListUrl")
    }
}

/** Wrapper so Hilt can tell this store apart from the app's other preference stores. */
class StudyListDataStore(val dataStore: DataStore<Preferences>)

private val Context.studyListDataStore by preferencesDataStore(name = "study-list-settings")

fun Context.studyListDataStore(): StudyListDataStore = StudyListDataStore(studyListDataStore)

internal const val StudyListDatePlaceholder = "{date}"

/** The device's own calendar day, written YYYY-MM-DD, replaces [StudyListDatePlaceholder]. */
internal fun studyListRequestUrl(url: String, date: LocalDate): String =
    url.trim().replace(StudyListDatePlaceholder, date.toString())

private val StudyListJson = Json { ignoreUnknownKeys = true }
private val StudyListKeys = listOf("day", "offered", "ids", "studied", "points")

/**
 * Reads the IDs from a plain JSON list, or from a list stored under `offered`, `ids`, `studied`
 * or `points`, optionally inside a `day` object. Entries are numbers, strings, or objects that
 * carry the number as `externalId` or `id`.
 */
internal fun parseStudyListIds(json: String): List<String> =
    StudyListJson.parseToJsonElement(json).studyListIds().distinct()

private fun JsonElement.studyListIds(): List<String> =
    when (this) {
        is JsonArray -> mapNotNull { it.studyListId() }
        is JsonObject -> StudyListKeys.firstNotNullOfOrNull { key ->
            this[key]?.takeIf { it is JsonArray || it is JsonObject }?.studyListIds()
        } ?: error("The study list has no list of IDs.")
        else -> error("The study list has no list of IDs.")
    }

private fun JsonElement.studyListId(): String? =
    when (this) {
        is JsonPrimitive -> content.trim().takeIf { it.isNotEmpty() && it != "null" }
        is JsonObject -> (this["externalId"] ?: this["id"])?.studyListId()
        else -> null
    }

private fun fetchStudyList(url: String): String =
    try {
        requestStudyList(url)
    } catch (error: Exception) {
        Log.w("StudyList", "Unable to load the study list", error)
        throw error
    }

private fun requestStudyList(url: String): String {
    val connection = URL(url).openConnection() as HttpURLConnection
    try {
        connection.connectTimeout = 8_000
        connection.readTimeout = 8_000
        connection.setRequestProperty("Accept", "application/json")
        check(connection.responseCode in 200..299) { "Study list request failed: ${connection.responseCode}" }
        return connection.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
    } finally {
        connection.disconnect()
    }
}

private val LeadingChapterIds = Regex("^\\D{0,3}(\\d+(?:[・･,、/+&]\\d+)*)")
private val ChapterIdSeparators = Regex("[・･,、/+&]")

/**
 * The numbers a chapter or book title starts with, compared by value so "042" and "42" are the
 * same ID. A title covering several points lists them joined, like "321・336 あり得ない".
 */
internal fun studyListChapterIds(label: String): List<String> =
    LeadingChapterIds.find(audiobookSearchNormalize(label).trim())
        ?.groupValues
        ?.get(1)
        ?.split(ChapterIdSeparators)
        ?.map { it.trimStart('0') }
        .orEmpty()

data class StudyListChapters(
    /** Chapters for today's IDs, in study order. */
    val chapters: List<AudiobookSearchResult> = emptyList(),
    /** Today's IDs that no audiobook chapter carries. */
    val missingIds: List<String> = emptyList(),
)

internal fun studyListChapters(
    books: List<AudiobookLibraryBook>,
    ids: List<String>,
): StudyListChapters {
    val chaptersById = buildMap<String, MutableList<AudiobookSearchResult>> {
        for (book in books) {
            val chapterIds = mutableSetOf<String>()
            for (chapter in book.chapters) {
                val result = AudiobookSearchResult(
                    bookId = book.id,
                    bookTitle = book.title,
                    chapter = chapter,
                    matchedInText = false,
                )
                for (id in studyListChapterIds(chapter.label)) {
                    chapterIds += id
                    getOrPut(id) { mutableListOf() } += result
                }
            }
            // A book that is one dialogue carries the ID in its own title; it opens from the start.
            val result = AudiobookSearchResult(
                bookId = book.id,
                bookTitle = book.title,
                chapter = AudiobookLibraryChapter(label = book.title, startCharacter = 0),
                matchedInText = false,
            )
            for (id in studyListChapterIds(book.title)) {
                if (id !in chapterIds) getOrPut(id) { mutableListOf() } += result
            }
        }
    }
    val chapters = mutableListOf<AudiobookSearchResult>()
    val missing = mutableListOf<String>()
    for (id in ids) {
        val found = chaptersById[id.trim().trimStart('0')]
        if (found == null) missing += id else chapters += found
    }
    // One dialogue covering two of today's points is listed once.
    return StudyListChapters(chapters = chapters.distinct(), missingIds = missing)
}
