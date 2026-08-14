package moe.antimony.hoshi.features.reader

enum class SasayakiVnBlankTapResult {
    Ignored,
    DismissLookup,
    TogglePlayback,
    AdvanceScreen,
}

fun sasayakiBindsAudioToVisibleVnScreen(
    pauseAtScreenEnd: Boolean,
    viewMode: ReaderViewMode,
    hasPlayer: Boolean,
): Boolean = pauseAtScreenEnd && viewMode == ReaderViewMode.VisualNovel && hasPlayer

fun sasayakiVnBlankTapResult(
    pauseAtScreenEnd: Boolean,
    viewMode: ReaderViewMode,
    hasPlayer: Boolean,
    waitingForContinue: Boolean,
    lookupVisible: Boolean,
): SasayakiVnBlankTapResult {
    if (lookupVisible) return SasayakiVnBlankTapResult.DismissLookup
    if (!sasayakiBindsAudioToVisibleVnScreen(pauseAtScreenEnd, viewMode, hasPlayer)) {
        return SasayakiVnBlankTapResult.Ignored
    }
    return if (waitingForContinue) {
        SasayakiVnBlankTapResult.AdvanceScreen
    } else {
        SasayakiVnBlankTapResult.TogglePlayback
    }
}
