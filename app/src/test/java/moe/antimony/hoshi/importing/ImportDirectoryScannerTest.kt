package moe.antimony.hoshi.importing

import org.junit.Assert.assertEquals
import org.junit.Test

class ImportDirectoryScannerTest {
    @Test
    fun recursivelyFindsMatchingFilesWithRelativeDisplayNames() {
        val tree = FakeImportDirectoryTree(
            "root" to listOf(
                directory("series", "Series"),
                file("root-book", "Root.EPUB"),
                file("notes", "notes.txt"),
            ),
            "series" to listOf(
                file("chapter-one", "01.epub"),
                directory("extras", "Extras"),
            ),
            "extras" to listOf(
                file("appendix", "Appendix.epub"),
            ),
        )

        val files = ImportDirectoryScanner(tree).scan("root", ImportFileType.Epub)

        assertEquals(
            listOf(
                ImportDirectoryFile(key = "root-book", displayName = "Root.EPUB"),
                ImportDirectoryFile(key = "chapter-one", displayName = "Series/01.epub"),
                ImportDirectoryFile(key = "appendix", displayName = "Series/Extras/Appendix.epub"),
            ),
            files,
        )
    }

    @Test
    fun ignoresVirtualDocumentsAndUsesRelativePathToDisambiguateDuplicates() {
        val tree = FakeImportDirectoryTree(
            "root" to listOf(
                directory("a", "A"),
                directory("b", "B"),
                file("virtual", "virtual.epub", isVirtual = true),
            ),
            "a" to listOf(file("a-book", "book.epub")),
            "b" to listOf(file("b-book", "book.epub")),
        )

        val files = ImportDirectoryScanner(tree).scan("root", ImportFileType.Epub)

        assertEquals(
            listOf(
                ImportDirectoryFile(key = "a-book", displayName = "A/book.epub"),
                ImportDirectoryFile(key = "b-book", displayName = "B/book.epub"),
            ),
            files,
        )
    }

    @Test
    fun findsSeveralFileTypesInOneWalk() {
        val tree = FakeImportDirectoryTree(
            "root" to listOf(
                file("book", "521.epub"),
                file("subtitle", "521.srt"),
                file("audio", "521.m4b"),
                file("notes", "521.txt"),
            ),
        )

        val files = ImportDirectoryScanner(tree).scan(
            "root",
            listOf(ImportFileType.Epub, ImportFileType.SasayakiSubtitle, ImportFileType.SasayakiAudiobook),
        )

        assertEquals(
            mapOf(
                ImportFileType.Epub to listOf(ImportDirectoryFile(key = "book", displayName = "521.epub")),
                ImportFileType.SasayakiSubtitle to
                    listOf(ImportDirectoryFile(key = "subtitle", displayName = "521.srt")),
                ImportFileType.SasayakiAudiobook to
                    listOf(ImportDirectoryFile(key = "audio", displayName = "521.m4b")),
            ),
            files,
        )
    }

    private class FakeImportDirectoryTree(
        vararg entries: Pair<String, List<ImportDirectoryDocument<String>>>,
    ) : ImportDirectoryTree<String> {
        private val children = entries.toMap()

        override fun children(directoryKey: String): List<ImportDirectoryDocument<String>> =
            children[directoryKey].orEmpty()
    }

    private companion object {
        fun directory(key: String, name: String): ImportDirectoryDocument<String> =
            ImportDirectoryDocument(
                key = key,
                name = name,
                isDirectory = true,
                isVirtual = false,
            )

        fun file(key: String, name: String, isVirtual: Boolean = false): ImportDirectoryDocument<String> =
            ImportDirectoryDocument(
                key = key,
                name = name,
                isDirectory = false,
                isVirtual = isVirtual,
            )
    }
}
