package com.djoudini.iplayer.presentation.navigation

/**
 * Type-safe navigation route definitions for the entire app.
 */
sealed class Route(val route: String) {

    // --- Onboarding ---
    data object Onboarding : Route("onboarding")
    data object LoginXtream : Route("login_xtream")
    data object LoginM3u : Route("login_m3u")
    data object CategoryFilter : Route("category_filter/{playlistId}") {
        fun create(playlistId: Long) = "category_filter/$playlistId"
    }

    // --- Main ---
    data object Dashboard : Route("dashboard")

    // --- Content Lists ---
    data object LiveCategories : Route("live_categories")
    data object VodCategories : Route("vod_categories")
    data object SeriesCategories : Route("series_categories")

    data object SeriesDetail : Route("series_detail/{seriesId}") {
        fun create(seriesId: Long) = "series_detail/$seriesId"
    }
    data object VodDetail : Route("vod_detail/{contentType}/{contentId}") {
        fun create(contentId: Long) = "vod_detail/vod/$contentId"
    }

    // --- Player ---
    data object Player : Route("player/{contentType}/{contentId}") {
        fun create(contentType: String, contentId: Long) = "player/$contentType/$contentId"
    }

    // --- Multi-View ---
    data object MultiView : Route("multi_view")

    // --- EPG ---
    data object EpgGrid : Route("epg_grid")

    // --- Settings ---
    data object Settings : Route("settings")
    data object PlayerSettings : Route("player_settings")
    data object AccountInfo : Route("account_info")

    // --- Search ---
    data object Search : Route("search")
    
    // --- Favorites ---
    data object Favorites : Route("favorites")
}

/** Navigation argument keys */
object NavArgs {
    const val PLAYLIST_ID = "playlistId"
    const val CATEGORY_ID = "categoryId"
    const val SERIES_ID = "seriesId"
    const val CONTENT_TYPE = "contentType"
    const val CONTENT_ID = "contentId"
}
