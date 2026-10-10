package moe.antimony.hoshi.features.grammar

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import moe.antimony.hoshi.R
import moe.antimony.hoshi.features.audiobooks.AudiobookOpenRequest
import moe.antimony.hoshi.ui.hoshiOutlinedTextFieldColors
import moe.antimony.hoshi.ui.hoshiSingleLineTextFieldLineLimits

private const val GrammarEmbedAsset = "hoshi-web/grammar/grammar-embed.js"

@Composable
internal fun GrammarView(
    /** A grammar point another tab asked to see. */
    cardRequest: String?,
    onCardRequestConsumed: () -> Unit,
    onOpenAudiobook: (AudiobookOpenRequest) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GrammarViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.reloadLibrary()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    var editingLink by rememberSaveable { mutableStateOf(false) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        val pageUrl = uiState.pageUrl
        when {
            pageUrl == null -> CircularProgressIndicator()
            pageUrl.isBlank() || editingLink -> GrammarLinkForm(
                url = pageUrl,
                onSave = { url ->
                    viewModel.onPageUrlChange(url)
                    editingLink = false
                },
            )
            else -> {
                GrammarPage(
                    pageUrl = pageUrl,
                    cardRequest = cardRequest,
                    onCardRequestConsumed = onCardRequestConsumed,
                    onCardChanged = viewModel::onCardChanged,
                    onEditLink = { editingLink = true },
                    modifier = Modifier.fillMaxSize(),
                )
                uiState.dialogue?.let { dialogue ->
                    ExtendedFloatingActionButton(
                        onClick = {
                            onOpenAudiobook(AudiobookOpenRequest(dialogue.bookId, dialogue.chapter.startCharacter))
                        },
                        icon = { Icon(Icons.Rounded.PlayArrow, contentDescription = null) },
                        text = { Text(stringResource(R.string.grammar_play_dialogue)) },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(20.dp),
                    )
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun GrammarPage(
    pageUrl: String,
    cardRequest: String?,
    onCardRequestConsumed: () -> Unit,
    onCardChanged: (String) -> Unit,
    onEditLink: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val currentOnCardChanged by rememberUpdatedState(onCardChanged)
    var failed by remember(pageUrl) { mutableStateOf(false) }
    var canGoBack by remember { mutableStateOf(false) }
    // Survives leaving the tab, so the page comes back at the grammar point it was on.
    var lastCardId by rememberSaveable(pageUrl) { mutableStateOf<String?>(null) }
    val embedScript = remember {
        context.assets.open(GrammarEmbedAsset).use { it.readBytes().toString(Charsets.UTF_8) }
    }
    val webView = remember(pageUrl) {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            addJavascriptInterface(
                object {
                    @JavascriptInterface
                    fun onCard(id: String) {
                        post {
                            lastCardId = grammarCardId(id)
                            currentOnCardChanged(id)
                        }
                    }
                },
                "HoshiGrammar",
            )
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    if (!request.isForMainFrame || grammarPageContains(pageUrl, request.url.toString())) return false
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, request.url)) }
                    return true
                }

                override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                    failed = false
                }

                override fun onPageFinished(view: WebView, url: String?) {
                    // Only the grammar page itself is trusted with the bridge's script.
                    if (url != null && grammarPageContains(pageUrl, url)) {
                        view.evaluateJavascript(embedScript, null)
                    }
                }

                override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
                    canGoBack = view.canGoBack()
                }

                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                    if (request.isForMainFrame) failed = true
                }
            }
            loadUrl(grammarPageAddress(pageUrl, lastCardId))
        }
    }
    DisposableEffect(webView) {
        onDispose {
            currentOnCardChanged("")
            webView.destroy()
        }
    }
    LaunchedEffect(cardRequest, webView) {
        val id = cardRequest?.let(::grammarCardId)
        if (cardRequest != null) onCardRequestConsumed()
        if (id != null) webView.loadUrl(grammarPageAddress(pageUrl, id))
    }
    BackHandler(enabled = canGoBack && !failed) { webView.goBack() }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        AndroidView(factory = { webView }, modifier = Modifier.fillMaxSize())
        if (failed) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            ) {
                Text(
                    text = stringResource(R.string.grammar_page_failed),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
                Button(onClick = { webView.loadUrl(grammarPageAddress(pageUrl, lastCardId)) }) {
                    Text(stringResource(R.string.action_try_again))
                }
                TextButton(onClick = onEditLink) {
                    Text(stringResource(R.string.grammar_link_change))
                }
            }
        }
    }
}

@Composable
private fun GrammarLinkForm(
    url: String,
    onSave: (String) -> Unit,
) {
    val urlState = rememberTextFieldState(url)
    Column(
        modifier = Modifier
            .widthIn(max = 560.dp)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.grammar_setup_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.grammar_setup_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        OutlinedTextField(
            state = urlState,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.grammar_link_title)) },
            placeholder = { Text(stringResource(R.string.grammar_link_placeholder)) },
            lineLimits = hoshiSingleLineTextFieldLineLimits(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
            onKeyboardAction = { onSave(urlState.text.toString()) },
            colors = hoshiOutlinedTextFieldColors(),
        )
        Button(onClick = { onSave(urlState.text.toString()) }) {
            Text(stringResource(R.string.action_save))
        }
    }
}
