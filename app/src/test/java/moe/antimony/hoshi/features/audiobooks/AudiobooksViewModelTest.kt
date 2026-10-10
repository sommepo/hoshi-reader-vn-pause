package moe.antimony.hoshi.features.audiobooks

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudiobooksViewModelTest {
    private val scope = CoroutineScope(Dispatchers.Unconfined + Job())
    private val n3 = AudiobookLibraryBook(
        id = "n3",
        title = "JLPT N3",
        chapters = listOf(
            AudiobookLibraryChapter(label = "042 〜てしまう", startCharacter = 0),
            AudiobookLibraryChapter(label = "043 〜ておく", startCharacter = 50),
        ),
    )
    private val n2 = AudiobookLibraryBook(id = "n2", title = "JLPT N2", chapters = emptyList())

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun startsLoadingAndShowsTheLibraryAfterReload() {
        val viewModel = viewModel(listOf(n3, n2))
        assertTrue(viewModel.uiState.value.isLoading)

        viewModel.reload()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(listOf(n3, n2), state.books)
        assertEquals(emptySet<String>(), state.expandedBookIds)
        assertFalse(state.isSearching)
    }

    @Test
    fun aSingleAudiobookOpensItsChapterListStraightAway() {
        val viewModel = viewModel(listOf(n3))

        viewModel.reload()

        assertEquals(setOf("n3"), viewModel.uiState.value.expandedBookIds)
    }

    @Test
    fun typingAQueryProducesResultsAndClearingItReturnsToTheLibrary() {
        val viewModel = viewModel(listOf(n3, n2))
        viewModel.reload()

        viewModel.onQueryChange("43")

        assertTrue(viewModel.uiState.value.isSearching)
        assertEquals(listOf("043 〜ておく"), viewModel.uiState.value.results.map { it.chapter.label })

        viewModel.onQueryChange("")

        assertFalse(viewModel.uiState.value.isSearching)
        assertEquals(emptyList<AudiobookSearchResult>(), viewModel.uiState.value.results)
    }

    @Test
    fun aQueryTypedBeforeTheLibraryLoadsIsAppliedOnceItArrives() {
        val viewModel = viewModel(listOf(n3))

        viewModel.onQueryChange("42")
        assertEquals(emptyList<AudiobookSearchResult>(), viewModel.uiState.value.results)
        viewModel.reload()

        assertEquals(listOf("042 〜てしまう"), viewModel.uiState.value.results.map { it.chapter.label })
    }

    @Test
    fun togglingABookExpandsAndCollapsesItAndSurvivesReload() {
        val viewModel = viewModel(listOf(n3, n2))
        viewModel.reload()

        viewModel.onToggleBook("n3")
        viewModel.reload()
        assertEquals(setOf("n3"), viewModel.uiState.value.expandedBookIds)

        viewModel.onToggleBook("n3")
        assertEquals(emptySet<String>(), viewModel.uiState.value.expandedBookIds)
    }

    @Test
    fun aFailingLibraryShowsAnEmptyListInsteadOfLoadingForever() {
        val viewModel = AudiobooksViewModel(
            library = object : AudiobookLibrarySource {
                override suspend fun loadLibrary(): List<AudiobookLibraryBook> = error("unreadable")
            },
            studyList = FakeStudyList(),
            dialogueLibrary = FakeDialogueLibrary(),
            searchDispatcher = Dispatchers.Unconfined,
            coroutineScope = scope,
        )

        viewModel.reload()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(emptyList<AudiobookLibraryBook>(), viewModel.uiState.value.books)
    }

    @Test
    fun todayIsUnsetUntilAStudyListLinkIsSaved() {
        val studyList = FakeStudyList(ids = listOf("43", "999", "42"))
        val viewModel = viewModel(listOf(n3, n2), studyList)
        viewModel.reload()
        assertEquals(AudiobooksTodayState.Unset, viewModel.uiState.value.today)

        viewModel.onStudyListUrlChange("https://study.example/today?date={date}")

        val state = viewModel.uiState.value
        assertEquals("https://study.example/today?date={date}", state.studyListUrl)
        val today = (state.today as AudiobooksTodayState.Loaded).list
        assertEquals(listOf("043 〜ておく", "042 〜てしまう"), today.chapters.map { it.chapter.label })
        assertEquals(listOf("999"), today.missingIds)
    }

    @Test
    fun todayIsFetchedAgainOnReloadAndKeepsTheLastListWhenTheFetchFails() {
        val studyList = FakeStudyList(url = "https://study.example/today", ids = listOf("42"))
        val viewModel = viewModel(listOf(n3), studyList)
        viewModel.reload()

        studyList.ids = listOf("42", "43")
        viewModel.reload()
        assertEquals(
            listOf("042 〜てしまう", "043 〜ておく"),
            (viewModel.uiState.value.today as AudiobooksTodayState.Loaded).list.chapters.map { it.chapter.label },
        )

        studyList.ids = null
        viewModel.reload()
        assertEquals(
            listOf("042 〜てしまう", "043 〜ておく"),
            (viewModel.uiState.value.today as AudiobooksTodayState.Loaded).list.chapters.map { it.chapter.label },
        )
    }

    @Test
    fun todayReportsAFailureWhenTheListHasNeverLoaded() {
        val viewModel = viewModel(listOf(n3), FakeStudyList(url = "https://study.example/today", ids = null))

        viewModel.reload()

        assertEquals(AudiobooksTodayState.Failed, viewModel.uiState.value.today)
    }

    @Test
    fun countsNewLibraryBooksAndDownloadsThemOnRequest() {
        val library = FakeDialogueLibrary(url = "https://books.example/index.json", newBooks = 2)
        val viewModel = viewModel(listOf(n3), dialogueLibrary = library)
        viewModel.reload()
        assertEquals(DialogueLibraryState.Idle(newBooks = 2), viewModel.uiState.value.dialogueLibrary)

        viewModel.onDownloadDialogues()

        assertEquals(listOf(1 to 2, 2 to 2), library.progress)
        assertEquals(DialogueLibraryState.Idle(newBooks = 0), viewModel.uiState.value.dialogueLibrary)
    }

    @Test
    fun reportsAnUnreachableLibraryAndDoesNothingWithoutALink() {
        val unreachable = FakeDialogueLibrary(url = "https://books.example/index.json", newBooks = null)
        val failing = viewModel(listOf(n3), dialogueLibrary = unreachable)
        failing.reload()
        assertEquals(DialogueLibraryState.Failed, failing.uiState.value.dialogueLibrary)

        val unset = FakeDialogueLibrary(newBooks = 5)
        val idle = viewModel(listOf(n3), dialogueLibrary = unset)
        idle.reload()
        idle.onDownloadDialogues()
        assertEquals(DialogueLibraryState.Idle(newBooks = 0), idle.uiState.value.dialogueLibrary)
        assertEquals(emptyList<Pair<Int, Int>>(), unset.progress)
    }

    private class FakeDialogueLibrary(
        url: String = "",
        var newBooks: Int? = 0,
    ) : DialogueLibrarySource {
        private val urlState = MutableStateFlow(url)
        val progress = mutableListOf<Pair<Int, Int>>()
        override val url: Flow<String> = urlState

        override suspend fun setUrl(url: String) {
            urlState.value = url
        }

        override suspend fun countNew(url: String): Int = newBooks ?: error("offline")

        override suspend fun downloadNew(url: String, onProgress: (Int, Int) -> Unit): Int {
            val total = newBooks ?: error("offline")
            repeat(total) {
                progress += (it + 1) to total
                onProgress(it + 1, total)
            }
            newBooks = 0
            return total
        }
    }

    private class FakeStudyList(
        url: String = "",
        var ids: List<String>? = emptyList(),
    ) : StudyListSource {
        private val urlState = MutableStateFlow(url)
        override val url: Flow<String> = urlState

        override suspend fun setUrl(url: String) {
            urlState.value = url
        }

        override suspend fun loadToday(url: String): List<String> = ids ?: error("offline")
    }

    private fun viewModel(
        books: List<AudiobookLibraryBook>,
        studyList: StudyListSource = FakeStudyList(),
        dialogueLibrary: DialogueLibrarySource = FakeDialogueLibrary(),
    ) =
        AudiobooksViewModel(
            library = object : AudiobookLibrarySource {
                override suspend fun loadLibrary(): List<AudiobookLibraryBook> = books
            },
            studyList = studyList,
            dialogueLibrary = dialogueLibrary,
            searchDispatcher = Dispatchers.Unconfined,
            coroutineScope = scope,
        )
}
