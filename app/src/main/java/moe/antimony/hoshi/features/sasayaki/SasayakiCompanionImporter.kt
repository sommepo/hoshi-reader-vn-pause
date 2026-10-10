package moe.antimony.hoshi.features.sasayaki

import android.content.ContentResolver
import android.net.Uri
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import moe.antimony.hoshi.di.IoDispatcher
import moe.antimony.hoshi.epub.BookRepository
import moe.antimony.hoshi.epub.EpubBook
import moe.antimony.hoshi.epub.SasayakiPlaybackData
import moe.antimony.hoshi.epub.SasayakiSidecarRepository
import moe.antimony.hoshi.importing.ImportFileType
import moe.antimony.hoshi.importing.validateImportFile

/**
 * Attaches the subtitle and audiobook found next to an EPUB during a folder import, doing what the
 * Sasayaki sheet does by hand. A book that already has a subtitle match or an audio source keeps it,
 * so importing the same folder again only fills in what is missing.
 */
@Singleton
internal class SasayakiCompanionImporter internal constructor(
    private val sidecars: SasayakiSidecarRepository,
    private val readSubtitle: (Uri) -> ByteArray,
    private val copyAudioToPrivateStorage: suspend () -> Boolean,
    private val copyAudio: (File, Uri) -> String,
    private val ioDispatcher: CoroutineDispatcher,
) {
    @Inject
    constructor(
        contentResolver: ContentResolver,
        bookRepository: BookRepository,
        settingsRepository: SasayakiSettingsRepository,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ) : this(
        sidecars = bookRepository,
        readSubtitle = { uri ->
            contentResolver.validateImportFile(uri, ImportFileType.SasayakiSubtitle)
            contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "Unable to open subtitle file." }.readBytes()
            }
        },
        copyAudioToPrivateStorage = { settingsRepository.settings.first().copyAudiobookToPrivateStorage },
        copyAudio = { bookRoot, uri -> SasayakiAudioRepository(bookRoot).importAudio(contentResolver, uri) },
        ioDispatcher = ioDispatcher,
    )

    suspend fun attach(
        bookRoot: File,
        book: EpubBook,
        subtitleUri: Uri?,
        audioUri: Uri?,
        alwaysCopyAudio: Boolean = false,
    ) = withContext(ioDispatcher) {
        sidecars.attachSasayakiCompanions(
            bookRoot = bookRoot,
            book = book,
            readSubtitle = subtitleUri?.let { uri -> { readSubtitle(uri) } },
            audioUri = audioUri?.toString(),
            copyAudio = {
                if (audioUri != null && (alwaysCopyAudio || copyAudioToPrivateStorage())) copyAudio(bookRoot, audioUri) else null
            },
        )
    }
}

/** Same starting value as the search window slider in the Sasayaki sheet. */
private const val DefaultSearchWindow = 200

/**
 * [copyAudio] returns the file name the audiobook was copied to inside the book, or null to keep
 * playing it from [audioUri].
 */
internal suspend fun SasayakiSidecarRepository.attachSasayakiCompanions(
    bookRoot: File,
    book: EpubBook,
    readSubtitle: (() -> ByteArray)?,
    audioUri: String?,
    copyAudio: suspend () -> String?,
) {
    if (readSubtitle != null && loadSasayakiMatch(bookRoot) == null) {
        val match = SasayakiMatcher.match(
            book = book,
            cues = SasayakiParser.parseCues(readSubtitle()),
            searchWindow = DefaultSearchWindow,
        )
        saveSasayakiMatch(bookRoot, match)
    }
    if (audioUri != null) {
        val playback = loadSasayakiPlayback(bookRoot) ?: SasayakiPlaybackData(lastPosition = 0.0)
        if (playback.audioUri.isNullOrBlank() && playback.audioFileName.isNullOrBlank()) {
            val copiedFileName = copyAudio()
            saveSasayakiPlayback(
                bookRoot,
                playback.copy(
                    audioUri = if (copiedFileName == null) audioUri else null,
                    audioFileName = copiedFileName,
                ),
            )
        }
    }
}
