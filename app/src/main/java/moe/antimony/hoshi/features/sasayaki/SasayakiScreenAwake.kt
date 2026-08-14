package moe.antimony.hoshi.features.sasayaki

object SasayakiScreenAwake {
    fun shouldKeepScreenOn(
        isPlaying: Boolean,
        autoScroll: Boolean,
        waitingForContinue: Boolean = false,
    ): Boolean =
        (isPlaying && autoScroll) || waitingForContinue
}
