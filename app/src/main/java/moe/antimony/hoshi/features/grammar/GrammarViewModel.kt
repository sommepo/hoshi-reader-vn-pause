package moe.antimony.hoshi.features.grammar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import moe.antimony.hoshi.features.audiobooks.AudiobookLibraryBook
import moe.antimony.hoshi.features.audiobooks.AudiobookLibraryRepository
import moe.antimony.hoshi.features.audiobooks.AudiobookLibrarySource
import moe.antimony.hoshi.features.audiobooks.AudiobookSearchResult
import moe.antimony.hoshi.features.audiobooks.studyListChapters

data class GrammarUiState(
    /** Null until the saved link has been read. */
    val pageUrl: String? = null,
    /** The grammar point the page is showing, if any. */
    val cardId: String? = null,
    /** The audiobook chapter for [cardId], when the library has one. */
    val dialogue: AudiobookSearchResult? = null,
)

@HiltViewModel
internal class GrammarViewModel internal constructor(
    private val page: GrammarPageSource,
    private val library: AudiobookLibrarySource,
    private val coroutineScope: CoroutineScope?,
) : ViewModel() {
    @Inject
    constructor(
        page: GrammarPageRepository,
        library: AudiobookLibraryRepository,
    ) : this(page = page, library = library, coroutineScope = null)

    private val scope: CoroutineScope
        get() = coroutineScope ?: viewModelScope

    private val _uiState = MutableStateFlow(GrammarUiState())
    val uiState: StateFlow<GrammarUiState> = _uiState.asStateFlow()

    private var books: List<AudiobookLibraryBook> = emptyList()
    private var libraryJob: Job? = null

    init {
        scope.launch {
            page.url.distinctUntilChanged().collect { url ->
                _uiState.update { it.copy(pageUrl = url) }
            }
        }
    }

    /** Reads the audiobook library again, so chapters downloaded since are offered. */
    fun reloadLibrary() {
        libraryJob?.cancel()
        libraryJob = scope.launch {
            books = runCatching { library.loadLibrary() }.getOrDefault(emptyList())
            showDialogue()
        }
    }

    fun onPageUrlChange(url: String) {
        scope.launch { page.setUrl(url) }
    }

    fun onCardChanged(fragment: String) {
        _uiState.update { it.copy(cardId = grammarCardId(fragment)) }
        showDialogue()
    }

    private fun showDialogue() {
        _uiState.update { state ->
            state.copy(
                dialogue = state.cardId?.let { studyListChapters(books, listOf(it)).chapters.firstOrNull() },
            )
        }
    }
}
