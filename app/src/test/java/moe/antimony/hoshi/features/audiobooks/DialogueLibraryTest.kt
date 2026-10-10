package moe.antimony.hoshi.features.audiobooks

import org.junit.Assert.assertEquals
import org.junit.Test

class DialogueLibraryTest {
    private val index = parseDialogueLibraryIndex(
        """
        {"version":1,"files":[
          {"name":"文法会話 N2 第1巻（19–43）.epub","size":100,"modified":10},
          {"name":"文法会話 N2 第1巻（19–43）.m4b","size":2000,"modified":11},
          {"name":"文法会話 N2 第1巻（19–43）.srt","size":30,"modified":12},
          {"name":"文法会話 N2 第2巻（44–68）.epub","size":101,"modified":20},
          {"name":"../outside.epub","size":1,"modified":1},
          {"name":"notes.txt","size":1,"modified":1}
        ]}
        """.trimIndent(),
    )

    @Test
    fun pairsEachBookWithItsSubtitleAndAudioAndIgnoresOtherFiles() {
        assertEquals(
            listOf(
                Triple(
                    "文法会話 N2 第1巻（19–43）.epub",
                    "文法会話 N2 第1巻（19–43）.srt",
                    "文法会話 N2 第1巻（19–43）.m4b",
                ),
                Triple("文法会話 N2 第2巻（44–68）.epub", null, null),
            ),
            index.bundles().map { Triple(it.book.displayName, it.subtitle?.displayName, it.audio?.displayName) },
        )
    }

    @Test
    fun aBookIsNewUntilItIsRecordedAndStillInTheLibrary() {
        val bundles = index.bundles()
        val first = bundles[0].signature()

        assertEquals(bundles, newDialogueBundles(bundles, imported = emptyMap(), libraryBookIds = emptySet()))
        assertEquals(
            listOf(bundles[1]),
            newDialogueBundles(bundles, imported = mapOf(first to "book-1"), libraryBookIds = setOf("book-1")),
        )
        // Deleted from the device, so it is offered again.
        assertEquals(
            bundles,
            newDialogueBundles(bundles, imported = mapOf(first to "book-1"), libraryBookIds = emptySet()),
        )
    }

    @Test
    fun aReplacedFileOnTheServerMakesTheBookNewAgain() {
        val before = index.bundles()[0].signature()
        val replaced = parseDialogueLibraryIndex(
            """
            {"files":[
              {"name":"文法会話 N2 第1巻（19–43）.epub","size":100,"modified":10},
              {"name":"文法会話 N2 第1巻（19–43）.m4b","size":2500,"modified":99},
              {"name":"文法会話 N2 第1巻（19–43）.srt","size":30,"modified":12}
            ]}
            """.trimIndent(),
        ).bundles()

        assertEquals(
            replaced,
            newDialogueBundles(replaced, imported = mapOf(before to "book-1"), libraryBookIds = setOf("book-1")),
        )
    }

    @Test
    fun fileAddressesSitBesideTheIndexWithTheNameEscaped() {
        assertEquals(
            "https://books.example:8789/dialogues/data/" +
                "%E4%BC%9A%E8%A9%B1%20N2%20%E7%AC%AC1%E5%B7%BB%EF%BC%8819%E2%80%9343%EF%BC%89.m4b",
            dialogueLibraryFileUrl(
                "https://books.example:8789/dialogues/data/index.json",
                "会話 N2 第1巻（19–43）.m4b",
            ),
        )
    }
}
