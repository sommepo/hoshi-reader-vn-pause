package moe.antimony.hoshi.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface AppRoute : NavKey {
    @Serializable
    data object MainRoute : AppRoute

    @Serializable
    data object BooksRoute : AppRoute

    @Serializable
    data object AudiobooksRoute : AppRoute

    @Serializable
    data object DictionaryRoute : AppRoute

    @Serializable
    data object StatisticsRoute : AppRoute

    @Serializable
    data object SettingsRoute : AppRoute

    @Serializable
    data class SettingsDetailRoute(
        val section: SettingsDetailSection,
    ) : AppRoute

    @Serializable
    data class AnkiCardFormatRoute(
        val formatId: String,
    ) : AppRoute

    @Serializable
    data object AnkiAdvancedRoute : AppRoute

    @Serializable
    data class ReaderRoute(
        val bookId: String,
        /** Whole-book character offset to open at instead of the saved bookmark. */
        val startCharacter: Int? = null,
        /** Opens in Audiobook mode without changing the saved view mode. */
        val audiobook: Boolean = false,
        val autoplay: Boolean = false,
    ) : AppRoute
}

@Serializable
enum class SettingsDetailSection {
    Dictionaries,
    Anki,
    Profiles,
    Appearance,
    Behavior,
    Advanced,
    Diagnostics,
    About,
}
