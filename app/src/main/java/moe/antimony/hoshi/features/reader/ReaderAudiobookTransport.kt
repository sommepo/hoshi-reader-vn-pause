package moe.antimony.hoshi.features.reader

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import moe.antimony.hoshi.R

@Composable
internal fun ReaderAudiobookTransportBar(
    transport: ReaderAudiobookTransport,
    colors: ReaderChromeColors,
    backgroundColor: Color,
    playing: Boolean,
    canReplayLine: Boolean,
    onReplayLine: () -> Unit,
    onPreviousLine: () -> Unit,
    onTogglePlayback: () -> Unit,
    onNextLine: () -> Unit,
    onChapters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(transport.reservedBottomDp.dp),
    ) {
        // Fades the text column out behind the controls; the safe area below keeps its own content.
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(bottom = transport.bottomSafeAreaDp.dp)
                .background(
                    Brush.verticalGradient(
                        0f to backgroundColor.copy(alpha = 0f),
                        0.3f to backgroundColor,
                        1f to backgroundColor,
                    ),
                ),
        )
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .height(transport.barHeightDp.dp)
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ReaderAudiobookTransportButton(
                icon = Icons.Rounded.Replay,
                contentDescription = stringResource(R.string.audiobook_replay_line),
                sizeDp = transport.tertiaryButtonSizeDp,
                colors = colors,
                enabled = canReplayLine,
                onClick = onReplayLine,
            )
            ReaderAudiobookTransportButton(
                icon = Icons.Rounded.SkipPrevious,
                contentDescription = stringResource(R.string.audiobook_previous_line),
                sizeDp = transport.secondaryButtonSizeDp,
                colors = colors,
                onClick = onPreviousLine,
            )
            ReaderAudiobookTransportButton(
                icon = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = stringResource(
                    if (playing) R.string.sasayaki_pause else R.string.sasayaki_play,
                ),
                sizeDp = transport.primaryButtonSizeDp,
                colors = colors,
                primary = true,
                primaryContentColor = backgroundColor,
                onClick = onTogglePlayback,
            )
            ReaderAudiobookTransportButton(
                icon = Icons.Rounded.SkipNext,
                contentDescription = stringResource(R.string.audiobook_next_line),
                sizeDp = transport.secondaryButtonSizeDp,
                colors = colors,
                onClick = onNextLine,
            )
            ReaderAudiobookTransportButton(
                icon = Icons.AutoMirrored.Rounded.FormatListBulleted,
                contentDescription = stringResource(R.string.reader_chapters),
                sizeDp = transport.tertiaryButtonSizeDp,
                colors = colors,
                onClick = onChapters,
            )
        }
    }
}

@Composable
private fun ReaderAudiobookTransportButton(
    icon: ImageVector,
    contentDescription: String,
    sizeDp: Int,
    colors: ReaderChromeColors,
    onClick: () -> Unit,
    enabled: Boolean = true,
    primary: Boolean = false,
    primaryContentColor: Color = Color.Unspecified,
) {
    val contentColor = Color(colors.buttonContent)
    val container = if (primary) contentColor else Color(colors.buttonContainer)
    val tint = if (primary) primaryContentColor else contentColor
    Surface(
        modifier = Modifier.size(sizeDp.dp),
        shape = CircleShape,
        color = container,
        border = if (primary) {
            null
        } else {
            BorderStroke(readerButtonBorderWidthDp(colors).dp, Color(colors.buttonBorder))
        },
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (enabled) tint else tint.copy(alpha = 0.38f),
                modifier = Modifier.size((sizeDp * 0.5f).dp),
            )
        }
    }
}
