package moe.antimony.hoshi.importing

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document

internal data class ImportDirectoryDocument<T>(
    val key: T,
    val name: String,
    val isDirectory: Boolean,
    val isVirtual: Boolean,
)

internal data class ImportDirectoryFile<T>(
    val key: T,
    val displayName: String,
)

internal interface ImportDirectoryTree<T> {
    fun children(directoryKey: T): List<ImportDirectoryDocument<T>>
}

internal class ImportDirectoryScanner<T>(
    private val tree: ImportDirectoryTree<T>,
) {
    fun scan(rootKey: T, type: ImportFileType): List<ImportDirectoryFile<T>> =
        scan(rootKey, listOf(type)).getValue(type)

    /** Walks the tree once and returns the files of each requested type. */
    fun scan(rootKey: T, types: List<ImportFileType>): Map<ImportFileType, List<ImportDirectoryFile<T>>> {
        val output = types.associateWith { mutableListOf<ImportDirectoryFile<T>>() }
        collect(directoryKey = rootKey, prefix = "", output = output)
        return output.mapValues { (_, files) ->
            files.sortedWith(
                compareBy<ImportDirectoryFile<T>> { it.displayName.lowercase() }
                    .thenBy { it.displayName },
            )
        }
    }

    private fun collect(
        directoryKey: T,
        prefix: String,
        output: Map<ImportFileType, MutableList<ImportDirectoryFile<T>>>,
    ) {
        tree.children(directoryKey).forEach { child ->
            val displayName = child.name.takeIf { it.isNotBlank() } ?: return@forEach
            val relativeName = if (prefix.isBlank()) displayName else "$prefix/$displayName"
            when {
                child.isVirtual -> Unit
                child.isDirectory -> collect(
                    directoryKey = child.key,
                    prefix = relativeName,
                    output = output,
                )
                else -> output.entries.firstOrNull { (type, _) -> type.matchesDisplayName(displayName) }
                    ?.value
                    ?.add(ImportDirectoryFile(key = child.key, displayName = relativeName))
            }
        }
    }
}

internal class SafImportDirectoryScanner(
    private val contentResolver: ContentResolver,
) {
    fun scan(treeUri: Uri, type: ImportFileType): List<ImportDirectoryFile<Uri>> =
        scan(treeUri, listOf(type)).getValue(type)

    fun scan(treeUri: Uri, types: List<ImportFileType>): Map<ImportFileType, List<ImportDirectoryFile<Uri>>> {
        val rootDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
        val rootDocumentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootDocumentId)
        return ImportDirectoryScanner(SafImportDirectoryTree(contentResolver, treeUri)).scan(rootDocumentUri, types)
    }
}

private class SafImportDirectoryTree(
    private val contentResolver: ContentResolver,
    private val treeUri: Uri,
) : ImportDirectoryTree<Uri> {
    override fun children(directoryKey: Uri): List<ImportDirectoryDocument<Uri>> {
        val directoryDocumentId = DocumentsContract.getDocumentId(directoryKey)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, directoryDocumentId)
        return contentResolver.query(
            childrenUri,
            ChildProjection,
            null,
            null,
            null,
        )?.use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    val documentId = cursor.string(Document.COLUMN_DOCUMENT_ID) ?: continue
                    val name = cursor.string(Document.COLUMN_DISPLAY_NAME)
                        ?: documentId.substringAfterLast('/')
                    val mimeType = cursor.string(Document.COLUMN_MIME_TYPE)
                    val flags = cursor.int(Document.COLUMN_FLAGS)
                    add(
                        ImportDirectoryDocument(
                            key = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId),
                            name = name,
                            isDirectory = mimeType == Document.MIME_TYPE_DIR,
                            isVirtual = flags and Document.FLAG_VIRTUAL_DOCUMENT != 0,
                        ),
                    )
                }
            }
        }.orEmpty()
    }
}

private val ChildProjection = arrayOf(
    Document.COLUMN_DOCUMENT_ID,
    Document.COLUMN_DISPLAY_NAME,
    Document.COLUMN_MIME_TYPE,
    Document.COLUMN_FLAGS,
)

private fun Cursor.string(columnName: String): String? {
    val index = getColumnIndex(columnName)
    return if (index >= 0 && !isNull(index)) getString(index) else null
}

private fun Cursor.int(columnName: String): Int {
    val index = getColumnIndex(columnName)
    return if (index >= 0 && !isNull(index)) getInt(index) else 0
}
