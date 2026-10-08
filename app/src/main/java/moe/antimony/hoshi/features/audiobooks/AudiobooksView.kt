package moe.antimony.hoshi.features.audiobooks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import moe.antimony.hoshi.R
import moe.antimony.hoshi.features.bookshelf.MainShellLayoutSpec
import moe.antimony.hoshi.ui.hoshiOutlinedTextFieldColors
import moe.antimony.hoshi.ui.hoshiSingleLineTextFieldLineLimits
import moe.antimony.hoshi.ui.rememberSyncedTextFieldState

/** What the Audiobooks tab asks the app to open. */
data class AudiobookOpenRequest(
    val bookId: String,
    /** Whole-book character offset to start from, or null to continue where the book was left. */
    val startCharacter: Int?,
)

@Composable
internal fun AudiobooksView(
    layoutSpec: MainShellLayoutSpec,
    onOpen: (AudiobookOpenRequest) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AudiobooksViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.reload()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    AudiobooksContent(
        uiState = uiState,
        layoutSpec = layoutSpec,
        onQueryChange = viewModel::onQueryChange,
        onToggleBook = viewModel::onToggleBook,
        onOpen = onOpen,
        modifier = modifier,
    )
}

@Composable
internal fun AudiobooksContent(
    uiState: AudiobooksUiState,
    layoutSpec: MainShellLayoutSpec,
    onQueryChange: (String) -> Unit,
    onToggleBook: (String) -> Unit,
    onOpen: (AudiobookOpenRequest) -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val queryScrollState = rememberScrollState()
    // State-based field so IME composition (kana to kanji) is never interrupted by a state round trip.
    val queryState = rememberSyncedTextFieldState(
        value = uiState.query,
        onValueChange = onQueryChange,
        scrollState = queryScrollState,
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = layoutSpec.contentMaxWidthDp.dp),
        ) {
            OutlinedTextField(
                state = queryState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = layoutSpec.pageHorizontalPaddingDp.dp)
                    .padding(top = 12.dp, bottom = 8.dp),
                placeholder = { Text(stringResource(R.string.audiobooks_search_placeholder)) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = if (uiState.query.isNotEmpty()) {
                    {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.audiobooks_search_clear),
                            )
                        }
                    }
                } else {
                    null
                },
                lineLimits = hoshiSingleLineTextFieldLineLimits(),
                scrollState = queryScrollState,
                textStyle = MaterialTheme.typography.titleMedium,
                shape = RoundedCornerShape(28.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                onKeyboardAction = { keyboard?.hide() },
                colors = hoshiOutlinedTextFieldColors(),
            )
            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                uiState.books.isEmpty() -> AudiobooksMessage(
                    title = stringResource(R.string.audiobooks_empty_title),
                    body = stringResource(R.string.audiobooks_empty_body),
                )
                uiState.isSearching && uiState.results.isEmpty() -> AudiobooksMessage(
                    title = stringResource(R.string.reader_no_search_results),
                    body = stringResource(R.string.audiobooks_no_results_body),
                )
                uiState.isSearching -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    items(items = uiState.results) { result ->
                        AudiobookChapterRow(
                            label = result.chapter.label,
                            supporting = if (result.matchedInText) {
                                stringResource(R.string.audiobooks_result_in_text, result.bookTitle)
                            } else {
                                result.bookTitle
                            },
                            horizontalPaddingDp = layoutSpec.pageHorizontalPaddingDp,
                            indentLevel = 0,
                            onClick = {
                                keyboard?.hide()
                                onOpen(AudiobookOpenRequest(result.bookId, result.chapter.startCharacter))
                            },
                        )
                    }
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    uiState.books.forEach { book ->
                        val expanded = book.id in uiState.expandedBookIds
                        item(key = "book:${book.id}") {
                            AudiobookBookRow(
                                book = book,
                                expanded = expanded,
                                horizontalPaddingDp = layoutSpec.pageHorizontalPaddingDp,
                                onToggle = { onToggleBook(book.id) },
                                onResume = { onOpen(AudiobookOpenRequest(book.id, startCharacter = null)) },
                            )
                        }
                        if (expanded) {
                            itemsIndexed(
                                items = book.chapters,
                                key = { index, _ -> "chapter:${book.id}:$index" },
                            ) { _, chapter ->
                                AudiobookChapterRow(
                                    label = chapter.label,
                                    supporting = null,
                                    horizontalPaddingDp = layoutSpec.pageHorizontalPaddingDp,
                                    indentLevel = chapter.indentLevel + 1,
                                    onClick = { onOpen(AudiobookOpenRequest(book.id, chapter.startCharacter)) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AudiobookBookRow(
    book: AudiobookLibraryBook,
    expanded: Boolean,
    horizontalPaddingDp: Int,
    onToggle: () -> Unit,
    onResume: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .heightIn(min = 72.dp)
                .padding(horizontal = horizontalPaddingDp.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = stringResource(
                    if (expanded) R.string.audiobooks_collapse_book else R.string.audiobooks_expand_book,
                ),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.audiobooks_chapter_count,
                        book.chapters.size,
                        book.chapters.size,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            FilledTonalIconButton(
                onClick = onResume,
                modifier = Modifier.size(52.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = stringResource(R.string.audiobooks_resume_book),
                    modifier = Modifier.size(30.dp),
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun AudiobookChapterRow(
    label: String,
    supporting: String?,
    horizontalPaddingDp: Int,
    indentLevel: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 60.dp)
            .padding(
                start = (horizontalPaddingDp + indentLevel.coerceIn(0, 4) * 20).dp,
                end = horizontalPaddingDp.dp,
                top = 8.dp,
                bottom = 8.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (supporting != null) {
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Icon(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AudiobooksMessage(
    title: String,
    body: String,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
