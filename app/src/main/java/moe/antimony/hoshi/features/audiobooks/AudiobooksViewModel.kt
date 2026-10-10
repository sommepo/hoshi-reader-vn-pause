package moe.antimony.hoshi.features.audiobooks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.antimony.hoshi.di.DefaultDispatcher

data class AudiobooksUiState(
    val isLoading: Boolean = true,
    val books: List<AudiobookLibraryBook> = emptyList(),
    val query: String = "",
    val results: List<AudiobookSearchResult> = emptyList(),
    val expandedBookIds: Set<String> = emptySet(),
    val studyListUrl: String = "",
    val today: AudiobooksTodayState = AudiobooksTodayState.Unset,
    val dialogueLibraryUrl: String = "",
    val dialogueLibrary: DialogueLibraryState = DialogueLibraryState.Idle(),
) {
    val isSearching: Boolean
        get() = audiobookSearchTerms(query).isNotEmpty()
}

/** The Today section of the Audiobooks tab: the chapters for the IDs studied today. */
sealed interface AudiobooksTodayState {
    /** No study list link has been set. */
    data object Unset : AudiobooksTodayState

    data object Loading : AudiobooksTodayState

    data object Failed : AudiobooksTodayState

    data class Loaded(val list: StudyListChapters) : AudiobooksTodayState
}

/** What the Audiobooks tab knows about the server folder it can download books from. */
sealed interface DialogueLibraryState {
    /** [newBooks] books on the server are not on this device. */
    data class Idle(val newBooks: Int = 0) : DialogueLibraryState

    data class Downloading(val current: Int, val total: Int) : DialogueLibraryState

    /** The last check or download did not finish. */
    data object Failed : DialogueLibraryState
}

@HiltViewModel
internal class AudiobooksViewModel internal constructor(
    private val library: AudiobookLibrarySource,
    private val studyList: StudyListSource,
    private val dialogueLibrary: DialogueLibrarySource,
    private val searchDispatcher: CoroutineDispatcher,
    private val coroutineScope: CoroutineScope?,
) : ViewModel() {
    @Inject
    constructor(
        library: AudiobookLibraryRepository,
        studyList: StudyListRepository,
        dialogueLibrary: DialogueLibraryRepository,
        @DefaultDispatcher searchDispatcher: CoroutineDispatcher,
    ) : this(
        library = library,
        studyList = studyList,
        dialogueLibrary = dialogueLibrary,
        searchDispatcher = searchDispatcher,
        coroutineScope = null,
    )

    private val scope: CoroutineScope
        get() = coroutineScope ?: viewModelScope

    private val _uiState = MutableStateFlow(AudiobooksUiState())
    val uiState: StateFlow<AudiobooksUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var searchJob: Job? = null
    private var todayJob: Job? = null
    private var todayIds: List<String>? = null
    private var dialogueJob: Job? = null

    init {
        scope.launch {
            dialogueLibrary.url.distinctUntilChanged().collect { url ->
                _uiState.update { it.copy(dialogueLibraryUrl = url) }
                checkDialogueLibrary()
            }
        }
        scope.launch {
            studyList.url.distinctUntilChanged().collect { url ->
                _uiState.update { it.copy(studyListUrl = url) }
                todayIds = null
                loadToday()
            }
        }
    }

    fun reload() {
        loadJob?.cancel()
        loadJob = scope.launch {
            val books = runCatching { library.loadLibrary() }.getOrDefault(emptyList())
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    books = books,
                    // A single audiobook has nothing to choose between, so show its chapters at once.
                    expandedBookIds = state.expandedBookIds.intersect(books.mapTo(mutableSetOf()) { it.id })
                        .ifEmpty { books.singleOrNull()?.let { setOf(it.id) }.orEmpty() },
                )
            }
            search()
            showToday()
            loadToday()
            checkDialogueLibrary()
        }
    }

    fun onDialogueLibraryUrlChange(url: String) {
        scope.launch { dialogueLibrary.setUrl(url) }
    }

    fun onDownloadDialogues() {
        val url = _uiState.value.dialogueLibraryUrl
        if (url.isBlank() || _uiState.value.dialogueLibrary is DialogueLibraryState.Downloading) return
        dialogueJob?.cancel()
        dialogueJob = scope.launch {
            _uiState.update { it.copy(dialogueLibrary = DialogueLibraryState.Downloading(current = 0, total = 0)) }
            val next = try {
                dialogueLibrary.downloadNew(url) { current, total ->
                    _uiState.update { it.copy(dialogueLibrary = DialogueLibraryState.Downloading(current, total)) }
                }
                DialogueLibraryState.Idle()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                DialogueLibraryState.Failed
            }
            _uiState.update { it.copy(dialogueLibrary = next) }
            // Books that did arrive show up even when a later one failed.
            reload()
        }
    }

    private fun checkDialogueLibrary() {
        if (_uiState.value.dialogueLibrary is DialogueLibraryState.Downloading) return
        dialogueJob?.cancel()
        val url = _uiState.value.dialogueLibraryUrl
        if (url.isBlank()) {
            _uiState.update { it.copy(dialogueLibrary = DialogueLibraryState.Idle()) }
            return
        }
        dialogueJob = scope.launch {
            val next = try {
                DialogueLibraryState.Idle(newBooks = dialogueLibrary.countNew(url))
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                DialogueLibraryState.Failed
            }
            _uiState.update { it.copy(dialogueLibrary = next) }
        }
    }

    fun onStudyListUrlChange(url: String) {
        scope.launch { studyList.setUrl(url) }
    }

    /** Fetches today's IDs again; the chapters shown meanwhile stay until the answer arrives. */
    private fun loadToday() {
        todayJob?.cancel()
        val url = _uiState.value.studyListUrl
        if (url.isBlank()) {
            _uiState.update { it.copy(today = AudiobooksTodayState.Unset) }
            return
        }
        if (todayIds == null) {
            _uiState.update { it.copy(today = AudiobooksTodayState.Loading) }
        }
        todayJob = scope.launch {
            val ids = try {
                studyList.loadToday(url)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                null
            }
            if (ids != null) {
                todayIds = ids
                showToday()
            } else if (todayIds == null) {
                _uiState.update { it.copy(today = AudiobooksTodayState.Failed) }
            }
        }
    }

    private fun showToday() {
        val ids = todayIds ?: return
        _uiState.update { state ->
            state.copy(today = AudiobooksTodayState.Loaded(studyListChapters(state.books, ids)))
        }
    }

    fun onQueryChange(query: String) {
        if (query == _uiState.value.query) return
        _uiState.update { it.copy(query = query) }
        search()
    }

    fun onToggleBook(bookId: String) {
        _uiState.update { state ->
            state.copy(
                expandedBookIds = if (bookId in state.expandedBookIds) {
                    state.expandedBookIds - bookId
                } else {
                    state.expandedBookIds + bookId
                },
            )
        }
    }

    private fun search() {
        searchJob?.cancel()
        val state = _uiState.value
        searchJob = scope.launch {
            val results = withContext(searchDispatcher) {
                searchAudiobookLibrary(state.books, state.query)
            }
            _uiState.update { current ->
                if (current.query == state.query && current.books === state.books) {
                    current.copy(results = results)
                } else {
                    current
                }
            }
        }
    }
}
