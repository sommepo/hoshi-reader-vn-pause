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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.LinearProgressIndicator
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
        onStudyListUrlChange = viewModel::onStudyListUrlChange,
        onDialogueLibraryUrlChange = viewModel::onDialogueLibraryUrlChange,
        onDownloadDialogues = viewModel::onDownloadDialogues,
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
    onStudyListUrlChange: (String) -> Unit,
    onDialogueLibraryUrlChange: (String) -> Unit,
    onDownloadDialogues: () -> Unit,
    onOpen: (AudiobookOpenRequest) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showStudyListDialog by remember { mutableStateOf(false) }
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
                    item(key = "today") {
                        AudiobooksTodayHeader(
                            today = uiState.today,
                            horizontalPaddingDp = layoutSpec.pageHorizontalPaddingDp,
                            onEditStudyList = { showStudyListDialog = true },
                        )
                    }
                    (uiState.today as? AudiobooksTodayState.Loaded)?.let { today ->
                        itemsIndexed(
                            items = today.list.chapters,
                            key = { index, _ -> "today:$index" },
                        ) { _, result ->
                            AudiobookChapterRow(
                                label = result.chapter.label,
                                supporting = result.bookTitle.takeIf {
                                    uiState.books.size > 1 && it != result.chapter.label
                                },
                                horizontalPaddingDp = layoutSpec.pageHorizontalPaddingDp,
                                indentLevel = 0,
                                onClick = { onOpen(AudiobookOpenRequest(result.bookId, result.chapter.startCharacter)) },
                            )
                        }
                    }
                    if (uiState.dialogueLibraryUrl.isNotBlank()) {
                        item(key = "dialogue-library") {
                            DialogueLibraryRow(
                                state = uiState.dialogueLibrary,
                                horizontalPaddingDp = layoutSpec.pageHorizontalPaddingDp,
                                onDownload = onDownloadDialogues,
                            )
                        }
                    }
                    item(key = "today-divider") {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    if (uiState.books.isEmpty()) {
                        item(key = "empty") {
                            AudiobooksMessage(
                                title = stringResource(R.string.audiobooks_empty_title),
                                body = stringResource(R.string.audiobooks_empty_body),
                                modifier = Modifier.fillParentMaxWidth(),
                            )
                        }
                    }
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
    if (showStudyListDialog) {
        StudyListDialog(
            studyListUrl = uiState.studyListUrl,
            dialogueLibraryUrl = uiState.dialogueLibraryUrl,
            onSave = { studyListUrl, dialogueLibraryUrl ->
                onStudyListUrlChange(studyListUrl)
                onDialogueLibraryUrlChange(dialogueLibraryUrl)
                showStudyListDialog = false
            },
            onDismiss = { showStudyListDialog = false },
        )
    }
}

@Composable
private fun AudiobooksTodayHeader(
    today: AudiobooksTodayState,
    horizontalPaddingDp: Int,
    onEditStudyList: () -> Unit,
) {
    val note = when (today) {
        AudiobooksTodayState.Unset -> stringResource(R.string.audiobooks_today_setup)
        AudiobooksTodayState.Loading -> stringResource(R.string.audiobooks_today_loading)
        AudiobooksTodayState.Failed -> stringResource(R.string.audiobooks_today_failed)
        is AudiobooksTodayState.Loaded -> when {
            today.list.missingIds.isNotEmpty() -> stringResource(
                R.string.audiobooks_today_missing,
                today.list.missingIds.joinToString(separator = ", "),
            )
            today.list.chapters.isEmpty() -> stringResource(R.string.audiobooks_today_empty)
            else -> null
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = horizontalPaddingDp.dp, end = (horizontalPaddingDp - 12).coerceAtLeast(0).dp)
            .padding(top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.audiobooks_today_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (note != null) {
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = onEditStudyList) {
            Icon(
                Icons.Rounded.Settings,
                contentDescription = stringResource(R.string.audiobooks_links_title),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DialogueLibraryRow(
    state: DialogueLibraryState,
    horizontalPaddingDp: Int,
    onDownload: () -> Unit,
) {
    val message = when (state) {
        is DialogueLibraryState.Idle ->
            if (state.newBooks == 0) return
            else pluralStringResource(R.plurals.audiobooks_library_new, state.newBooks, state.newBooks)
        is DialogueLibraryState.Downloading ->
            if (state.total == 0) stringResource(R.string.audiobooks_library_checking)
            else stringResource(R.string.audiobooks_library_downloading, state.current, state.total)
        DialogueLibraryState.Failed -> stringResource(R.string.audiobooks_library_failed)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPaddingDp.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            when (state) {
                is DialogueLibraryState.Idle -> FilledTonalButton(onClick = onDownload) {
                    Text(stringResource(R.string.action_download))
                }
                DialogueLibraryState.Failed -> FilledTonalButton(onClick = onDownload) {
                    Text(stringResource(R.string.audiobooks_library_retry))
                }
                is DialogueLibraryState.Downloading -> Unit
            }
        }
        if (state is DialogueLibraryState.Downloading) {
            if (state.total == 0) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                LinearProgressIndicator(
                    progress = { (state.current - 1).coerceAtLeast(0) / state.total.toFloat() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun StudyListDialog(
    studyListUrl: String,
    dialogueLibraryUrl: String,
    onSave: (studyListUrl: String, dialogueLibraryUrl: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val studyListState = rememberTextFieldState(studyListUrl)
    val dialogueLibraryState = rememberTextFieldState(dialogueLibraryUrl)
    val save = { onSave(studyListState.text.toString(), dialogueLibraryState.text.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.audiobooks_links_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    state = studyListState,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.audiobooks_study_list_title)) },
                    placeholder = { Text(stringResource(R.string.audiobooks_study_list_placeholder)) },
                    lineLimits = hoshiSingleLineTextFieldLineLimits(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                    colors = hoshiOutlinedTextFieldColors(),
                )
                Text(
                    text = stringResource(R.string.audiobooks_study_list_body, StudyListDatePlaceholder),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    state = dialogueLibraryState,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.audiobooks_library_title)) },
                    placeholder = { Text(stringResource(R.string.audiobooks_library_placeholder)) },
                    lineLimits = hoshiSingleLineTextFieldLineLimits(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                    onKeyboardAction = { save() },
                    colors = hoshiOutlinedTextFieldColors(),
                )
                Text(
                    text = stringResource(R.string.audiobooks_library_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = save) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
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
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    Column(
        modifier = modifier
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
