package moe.antimony.hoshi.features.audiobooks

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
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
            searchDispatcher = Dispatchers.Unconfined,
            coroutineScope = scope,
        )

        viewModel.reload()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(emptyList<AudiobookLibraryBook>(), viewModel.uiState.value.books)
    }

    private fun viewModel(books: List<AudiobookLibraryBook>) =
        AudiobooksViewModel(
            library = object : AudiobookLibrarySource {
                override suspend fun loadLibrary(): List<AudiobookLibraryBook> = books
            },
            searchDispatcher = Dispatchers.Unconfined,
            coroutineScope = scope,
        )
}
