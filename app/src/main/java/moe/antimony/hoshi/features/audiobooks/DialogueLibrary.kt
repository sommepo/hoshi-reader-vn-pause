package moe.antimony.hoshi.features.audiobooks

import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import moe.antimony.hoshi.di.CacheDir
import moe.antimony.hoshi.di.FilesDir
import moe.antimony.hoshi.di.IoDispatcher
import moe.antimony.hoshi.epub.BookRepository
import moe.antimony.hoshi.epub.BookSortOption
import moe.antimony.hoshi.features.bookshelf.BookshelfRepository
import moe.antimony.hoshi.importing.BookImportBundle
import moe.antimony.hoshi.importing.ImportDirectoryFile
import moe.antimony.hoshi.importing.ImportFileType
import moe.antimony.hoshi.importing.planBookImportBundles

/**
 * A server folder of audiobooks (EPUB, subtitle and audio of the same name) that the Audiobooks
 * tab can download from, so new books arrive without copying files to the device by hand.
 */
interface DialogueLibrarySource {
    /** The link to the library's index; blank when none is set. */
    val url: Flow<String>

    suspend fun setUrl(url: String)

    /** How many books on the server are not on this device. Throws when the index cannot be read. */
    suspend fun countNew(url: String): Int

    /**
     * Downloads and imports every book that is not on this device, reporting the book being
     * worked on (from 1) and the total. Returns how many were imported.
     */
    suspend fun downloadNew(url: String, onProgress: (current: Int, total: Int) -> Unit): Int
}

@Serializable
internal data class DialogueLibraryFile(
    val name: String,
    val size: Long = 0,
    val modified: Long = 0,
)

@Serializable
internal data class DialogueLibraryIndex(
    val files: List<DialogueLibraryFile> = emptyList(),
)

private val DialogueLibraryJson = Json { ignoreUnknownKeys = true }

internal fun parseDialogueLibraryIndex(json: String): DialogueLibraryIndex =
    DialogueLibraryJson.decodeFromString(DialogueLibraryIndex.serializer(), json)

/** Pairs the index's EPUBs with their subtitle and audio, by the rules folder import uses. */
internal fun DialogueLibraryIndex.bundles(): List<BookImportBundle<DialogueLibraryFile>> {
    fun filesOf(type: ImportFileType) = files
        .filter { '/' !in it.name && '\\' !in it.name && type.matchesDisplayName(it.name) }
        .map { ImportDirectoryFile(key = it, displayName = it.name) }
    return planBookImportBundles(
        books = filesOf(ImportFileType.Epub),
        subtitles = filesOf(ImportFileType.SasayakiSubtitle),
        audio = filesOf(ImportFileType.SasayakiAudiobook),
    )
}

/** Changes whenever any file of the book is replaced on the server. */
internal fun BookImportBundle<DialogueLibraryFile>.signature(): String =
    listOfNotNull(book.key, subtitle?.key, audio?.key)
        .joinToString(separator = "|") { "${it.name}:${it.size}:${it.modified}" }

/** [imported] maps the signature of each book already downloaded to the ID it was imported as. */
internal fun newDialogueBundles(
    bundles: List<BookImportBundle<DialogueLibraryFile>>,
    imported: Map<String, String>,
    libraryBookIds: Set<String>,
): List<BookImportBundle<DialogueLibraryFile>> =
    bundles.filter { bundle -> imported[bundle.signature()]?.let { it in libraryBookIds } != true }

/** The address of a file stored beside the index, with its name escaped for a URL. */
internal fun dialogueLibraryFileUrl(indexUrl: String, name: String): String =
    URL(URL(indexUrl.trim()), URI(null, null, name, null).toASCIIString()).toString()

@Singleton
internal class DialogueLibraryRepository @Inject constructor(
    studyListDataStore: StudyListDataStore,
    private val bookshelfRepository: BookshelfRepository,
    private val bookRepository: BookRepository,
    @param:FilesDir private val filesDir: File,
    @param:CacheDir private val cacheDir: File,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : DialogueLibrarySource {
    private val dataStore: DataStore<Preferences> = studyListDataStore.dataStore
    private val importedFile get() = File(filesDir, "dialogue-library-imported.json")
    private val downloadDir get() = File(cacheDir, "dialogue-library")

    override val url: Flow<String> = dataStore.data.map { it[KEY_URL].orEmpty() }

    override suspend fun setUrl(url: String) {
        dataStore.edit { it[KEY_URL] = url.trim() }
    }

    override suspend fun countNew(url: String): Int = withContext(ioDispatcher) {
        newBundles(url).size
    }

    override suspend fun downloadNew(url: String, onProgress: (Int, Int) -> Unit): Int =
        withContext(ioDispatcher) {
            val bundles = newBundles(url)
            bundles.forEachIndexed { index, bundle ->
                ensureActive()
                onProgress(index + 1, bundles.size)
                val workDir = File(downloadDir, index.toString())
                try {
                    workDir.deleteRecursively()
                    workDir.mkdirs()
                    fun fetched(file: DialogueLibraryFile?): Uri? = file?.let {
                        val target = File(workDir, it.name)
                        download(dialogueLibraryFileUrl(url, it.name), target)
                        Uri.fromFile(target)
                    }
                    val bookId = bookshelfRepository.importBookBundle(
                        bookUri = requireNotNull(fetched(bundle.book.key)),
                        subtitleUri = fetched(bundle.subtitle?.key),
                        audioUri = fetched(bundle.audio?.key),
                        alwaysCopyAudio = true,
                    )
                    // Recorded book by book, so an interrupted run resumes where it stopped.
                    saveImported(loadImported() + (bundle.signature() to bookId))
                } finally {
                    workDir.deleteRecursively()
                }
            }
            bundles.size
        }

    private suspend fun newBundles(url: String): List<BookImportBundle<DialogueLibraryFile>> {
        val index = parseDialogueLibraryIndex(fetchText(url.trim()))
        val libraryBookIds = bookRepository.loadBookEntries(BookSortOption.Title)
            .mapTo(mutableSetOf()) { it.metadata.id }
        return newDialogueBundles(index.bundles(), loadImported(), libraryBookIds)
    }

    private fun loadImported(): Map<String, String> =
        runCatching {
            DialogueLibraryJson.decodeFromString(ImportedSerializer, importedFile.readText())
        }.getOrDefault(emptyMap())

    private fun saveImported(imported: Map<String, String>) {
        val partial = File(filesDir, "${importedFile.name}.part")
        partial.writeText(DialogueLibraryJson.encodeToString(ImportedSerializer, imported))
        check(partial.renameTo(importedFile)) { "Unable to record the downloaded book." }
    }

    private fun fetchText(url: String): String =
        open(url) { connection -> connection.inputStream.use { it.readBytes().toString(Charsets.UTF_8) } }

    private fun download(url: String, target: File) {
        open(url) { connection ->
            connection.inputStream.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
        }
    }

    private fun <T> open(url: String, read: (HttpURLConnection) -> T): T {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 8_000
            connection.readTimeout = 30_000
            check(connection.responseCode in 200..299) { "Dialogue library request failed: ${connection.responseCode}" }
            return read(connection)
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        val KEY_URL = stringPreferencesKey("dialogueLibraryUrl")
        val ImportedSerializer = MapSerializer(String.serializer(), String.serializer())
    }
}
