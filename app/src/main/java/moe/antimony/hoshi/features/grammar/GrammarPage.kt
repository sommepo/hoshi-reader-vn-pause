package moe.antimony.hoshi.features.grammar

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import moe.antimony.hoshi.features.audiobooks.StudyListDataStore

/** Where the Grammar tab's page lives: a grammar library web page on the user's own server. */
interface GrammarPageSource {
    /** The page's link; blank when none is set. */
    val url: Flow<String>

    suspend fun setUrl(url: String)
}

@Singleton
class GrammarPageRepository @Inject constructor(
    studyListDataStore: StudyListDataStore,
) : GrammarPageSource {
    private val dataStore = studyListDataStore.dataStore

    override val url: Flow<String> = dataStore.data.map { it[KEY_URL].orEmpty() }

    override suspend fun setUrl(url: String) {
        dataStore.edit { it[KEY_URL] = url.trim() }
    }

    private companion object {
        val KEY_URL = stringPreferencesKey("grammarPageUrl")
    }
}

/** A grammar point's ID as the page writes it in its address: `/grammar/#321`. */
internal fun grammarCardId(fragment: String): String? =
    fragment.trim().takeIf { it.isNotEmpty() && it.length <= 12 && it.all(Char::isLetterOrDigit) }

/** The page's address, opened at one grammar point when [cardId] names one. */
internal fun grammarPageAddress(pageUrl: String, cardId: String?): String {
    val page = pageUrl.trim().substringBefore('#')
    val id = cardId?.let(::grammarCardId) ?: return page
    return "$page#$id"
}

/**
 * True when [target] stays inside the grammar page: same server and under the page's own folder.
 * Anything else, like the server's other pages, opens outside the app.
 */
internal fun grammarPageContains(pageUrl: String, target: String): Boolean {
    val page = runCatching { URI(pageUrl.trim()) }.getOrNull() ?: return false
    val other = runCatching { URI(target.trim()) }.getOrNull() ?: return false
    if (!page.scheme.equals(other.scheme, ignoreCase = true)) return false
    if (!page.host.equals(other.host, ignoreCase = true) || page.port != other.port) return false
    val folder = page.path.orEmpty().substringBeforeLast('/', missingDelimiterValue = "") + "/"
    val path = other.path.orEmpty().ifEmpty { "/" }
    return path.startsWith(folder) || "$path/" == folder
}
