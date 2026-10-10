package moe.antimony.hoshi.features.grammar

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import moe.antimony.hoshi.features.audiobooks.AudiobookLibraryBook
import moe.antimony.hoshi.features.audiobooks.AudiobookLibraryChapter
import moe.antimony.hoshi.features.audiobooks.AudiobookLibrarySource
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GrammarPageTest {
    private val scope = CoroutineScope(Dispatchers.Unconfined + Job())
    private val page = "https://study.example:8789/grammar/"

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun addressesOpenThePageAtAGrammarPoint() {
        assertEquals("$page#321", grammarPageAddress(page, "321"))
        assertEquals("$page#321", grammarPageAddress(" $page#12 ", "321"))
        assertEquals(page, grammarPageAddress(page, null))
        assertEquals(page, grammarPageAddress(page, "javascript:alert(1)"))
    }

    @Test
    fun onlyPlainIdsCountAsGrammarPoints() {
        assertEquals("521", grammarCardId(" 521 "))
        assertNull(grammarCardId(""))
        assertNull(grammarCardId("a/b"))
        assertNull(grammarCardId("1234567890123"))
    }

    @Test
    fun navigationStaysInsideThePagesOwnFolder() {
        assertTrue(grammarPageContains(page, "https://study.example:8789/grammar/#321"))
        assertTrue(grammarPageContains(page, "https://study.example:8789/grammar/index.html?v=2"))
        assertTrue(grammarPageContains(page, "https://study.example:8789/grammar"))
        assertFalse(grammarPageContains(page, "https://study.example:8789/"))
        assertFalse(grammarPageContains(page, "https://study.example:8789/transfer/"))
        assertFalse(grammarPageContains(page, "https://study.example/grammar/"))
        assertFalse(grammarPageContains(page, "http://study.example:8789/grammar/"))
        assertFalse(grammarPageContains(page, "https://elsewhere.example:8789/grammar/"))
        assertFalse(grammarPageContains(page, "intent://grammar"))
    }

    @Test
    fun offersTheDialogueOfTheGrammarPointBeingShown() {
        val book = AudiobookLibraryBook(
            id = "n2",
            title = "N2",
            chapters = listOf(
                AudiobookLibraryChapter(label = "321・336 あり得ない", startCharacter = 0),
                AudiobookLibraryChapter(label = "521 戻れるものなら", startCharacter = 80),
            ),
        )
        val source = FakePage()
        val viewModel = GrammarViewModel(
            page = source,
            library = object : AudiobookLibrarySource {
                override suspend fun loadLibrary(): List<AudiobookLibraryBook> = listOf(book)
            },
            coroutineScope = scope,
        )
        assertEquals("", viewModel.uiState.value.pageUrl)

        viewModel.onPageUrlChange(page)
        viewModel.onCardChanged("336")
        assertEquals(page, viewModel.uiState.value.pageUrl)
        assertNull(viewModel.uiState.value.dialogue)

        viewModel.reloadLibrary()
        assertEquals("321・336 あり得ない", viewModel.uiState.value.dialogue?.chapter?.label)

        viewModel.onCardChanged("999")
        assertNull(viewModel.uiState.value.dialogue)

        viewModel.onCardChanged("")
        assertNull(viewModel.uiState.value.cardId)
    }

    private class FakePage : GrammarPageSource {
        private val state = MutableStateFlow("")
        override val url: Flow<String> = state

        override suspend fun setUrl(url: String) {
            state.value = url
        }
    }
}
