package com.djoudini.iplayer.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.djoudini.iplayer.presentation.ui.mobile.CategoryFilterScreen
import com.djoudini.iplayer.presentation.ui.mobile.DashboardScreen
import com.djoudini.iplayer.presentation.ui.mobile.EpgGridScreen
import com.djoudini.iplayer.presentation.ui.mobile.FavoritesScreen
import com.djoudini.iplayer.presentation.ui.mobile.LoginM3uScreen
import com.djoudini.iplayer.presentation.ui.mobile.LoginXtreamScreen
import com.djoudini.iplayer.presentation.ui.mobile.OnboardingScreen
import com.djoudini.iplayer.presentation.ui.mobile.PlayerScreen
import com.djoudini.iplayer.presentation.ui.mobile.SearchScreen
import com.djoudini.iplayer.presentation.ui.mobile.SeriesCategoriesScreen
import com.djoudini.iplayer.presentation.ui.mobile.SeriesDetailScreen
import com.djoudini.iplayer.presentation.ui.mobile.SettingsScreen
import com.djoudini.iplayer.presentation.ui.tv.TvSettingsScreen
import com.djoudini.iplayer.presentation.ui.mobile.VodCategoriesScreen
import com.djoudini.iplayer.presentation.ui.mobile.VodDetailScreen
import com.djoudini.iplayer.presentation.ui.mobile.LiveCategoriesScreen
import com.djoudini.iplayer.presentation.ui.mobile.MultiViewScreen
import com.djoudini.iplayer.presentation.ui.tv.TvEpgGridScreen
import com.djoudini.iplayer.presentation.ui.tv.TvLiveCategoriesScreen
import com.djoudini.iplayer.presentation.ui.tv.TvMultiViewScreen
import com.djoudini.iplayer.presentation.ui.tv.TvSearchScreen
import com.djoudini.iplayer.presentation.ui.tv.TvSeriesCategoriesScreen
import com.djoudini.iplayer.presentation.ui.tv.TvSeriesDetailScreen
import com.djoudini.iplayer.presentation.ui.tv.TvVodCategoriesScreen
import com.djoudini.iplayer.presentation.ui.tv.TvOnboardingScreen
import com.djoudini.iplayer.presentation.ui.tv.TvLoginXtreamScreen
import com.djoudini.iplayer.presentation.ui.tv.TvLoginM3uScreen
import com.djoudini.iplayer.presentation.ui.tv.TvDashboardScreen
import com.djoudini.iplayer.presentation.ui.tv.TvVodDetailScreen
import com.djoudini.iplayer.presentation.ui.tv.TvCategoryFilterScreen
import com.djoudini.iplayer.presentation.ui.tv.TvFavoritesScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String,
    isTvDevice: Boolean = false,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        // --- Onboarding ---
        composable(Route.Onboarding.route) {
            if (isTvDevice) {
                TvOnboardingScreen(
                    onXtreamLogin = { navController.navigate(Route.LoginXtream.route) },
                    onM3uLogin = { navController.navigate(Route.LoginM3u.route) },
                )
            } else {
                OnboardingScreen(
                    onXtreamLogin = { navController.navigate(Route.LoginXtream.route) },
                    onM3uLogin = { navController.navigate(Route.LoginM3u.route) },
                )
            }
        }

        composable(Route.LoginXtream.route) {
            if (isTvDevice) {
                TvLoginXtreamScreen(
                    onLoginSuccess = { playlistId ->
                        navController.navigate(Route.CategoryFilter.create(playlistId)) {
                            popUpTo(Route.Onboarding.route) { inclusive = false }
                        }
                    },
                    onBack = { navController.popBackStack() },
                )
            } else {
                LoginXtreamScreen(
                    onLoginSuccess = { playlistId ->
                        navController.navigate(Route.CategoryFilter.create(playlistId)) {
                            popUpTo(Route.Onboarding.route) { inclusive = false }
                        }
                    },
                    onBack = { navController.popBackStack() },
                )
            }
        }

        composable(Route.LoginM3u.route) {
            if (isTvDevice) {
                TvLoginM3uScreen(
                    onLoginSuccess = { playlistId ->
                        navController.navigate(Route.CategoryFilter.create(playlistId)) {
                            popUpTo(Route.Onboarding.route) { inclusive = false }
                        }
                    },
                    onBack = { navController.popBackStack() },
                )
            } else {
                LoginM3uScreen(
                    onLoginSuccess = { playlistId ->
                        navController.navigate(Route.CategoryFilter.create(playlistId)) {
                            popUpTo(Route.Onboarding.route) { inclusive = false }
                        }
                    },
                    onBack = { navController.popBackStack() },
                )
            }
        }

        composable(
            route = Route.CategoryFilter.route,
            arguments = listOf(navArgument(NavArgs.PLAYLIST_ID) { type = NavType.LongType }),
        ) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getLong(NavArgs.PLAYLIST_ID) ?: return@composable
            if (isTvDevice) {
                TvCategoryFilterScreen(
                    playlistId = playlistId,
                    onComplete = {
                        navController.navigate(Route.Dashboard.route) {
                            popUpTo(Route.Onboarding.route) { inclusive = true }
                        }
                    },
                )
            } else {
                CategoryFilterScreen(
                    playlistId = playlistId,
                    onComplete = {
                        navController.navigate(Route.Dashboard.route) {
                            popUpTo(Route.Onboarding.route) { inclusive = true }
                        }
                    },
                )
            }
        }

        // --- Dashboard ---
        composable(Route.Dashboard.route) {
            if (isTvDevice) {
                TvDashboardScreen(
                    onNavigateLive = { navController.navigate(Route.LiveCategories.route) },
                    onNavigateVod = { navController.navigate(Route.VodCategories.route) },
                    onNavigateSeries = { navController.navigate(Route.SeriesCategories.route) },
                    onNavigateEpg = { navController.navigate(Route.EpgGrid.route) },
                    onNavigateSettings = { navController.navigate(Route.Settings.route) },
                    onNavigateSearch = { navController.navigate(Route.Search.route) },
                    onNavigateMultiView = { navController.navigate(Route.MultiView.route) },
                    onNavigateFavorites = { navController.navigate(Route.Favorites.route) },
                    onContinueWatchingClick = { contentType, contentId ->
                        when (contentType) {
                            "vod" -> navController.navigate(Route.VodDetail.create(contentId))
                            "episode" -> navController.navigate(Route.Player.create("episode", contentId))
                            "channel" -> navController.navigate(Route.Player.create("channel", contentId))
                            else -> navController.navigate(Route.Player.create(contentType, contentId))
                        }
                    },
                )
            } else {
                DashboardScreen(
                    onNavigateLive = { navController.navigate(Route.LiveCategories.route) },
                    onNavigateVod = { navController.navigate(Route.VodCategories.route) },
                    onNavigateSeries = { navController.navigate(Route.SeriesCategories.route) },
                    onNavigateEpg = { navController.navigate(Route.EpgGrid.route) },
                    onNavigateSettings = { navController.navigate(Route.Settings.route) },
                    onNavigateSearch = { navController.navigate(Route.Search.route) },
                    onNavigateMultiView = { navController.navigate(Route.MultiView.route) },
                    onNavigateFavorites = { navController.navigate(Route.Favorites.route) },
                    onContinueWatchingClick = { contentType, contentId ->
                        when (contentType) {
                            "vod" -> navController.navigate(Route.VodDetail.create(contentId))
                            "episode" -> navController.navigate(Route.Player.create("episode", contentId))
                            "channel" -> navController.navigate(Route.Player.create("channel", contentId))
                            else -> {
                                // Try to navigate to player with original content type
                                navController.navigate(Route.Player.create(contentType, contentId))
                            }
                        }
                    },
                )
            }
        }

        // --- Content Lists (Split-pane: categories left, content right) ---
        composable(Route.LiveCategories.route) {
            if (isTvDevice) {
                TvLiveCategoriesScreen(
                    onChannelClick = { channelId ->
                        navController.navigate(Route.Player.create("channel", channelId))
                    },
                    onBack = { navController.popBackStack() },
                )
            } else {
                LiveCategoriesScreen(
                    onCategoryClick = {},
                    onChannelClick = { channelId ->
                        navController.navigate(Route.Player.create("channel", channelId))
                    },
                    onBack = { navController.popBackStack() },
                )
            }
        }

        composable(Route.VodCategories.route) {
            if (isTvDevice) {
                TvVodCategoriesScreen(
                    onVodClick = { vodId ->
                        navController.navigate(Route.VodDetail.create(vodId))
                    },
                    onBack = { navController.popBackStack() },
                )
            } else {
                VodCategoriesScreen(
                    onCategoryClick = {},
                    onVodClick = { vodId ->
                        navController.navigate(Route.VodDetail.create(vodId))
                    },
                    onBack = { navController.popBackStack() },
                )
            }
        }

        composable(
            route = Route.VodDetail.route,
            arguments = listOf(
                navArgument(NavArgs.CONTENT_TYPE) { type = NavType.StringType },
                navArgument(NavArgs.CONTENT_ID) { type = NavType.LongType },
            ),
        ) {
            if (isTvDevice) {
                TvVodDetailScreen(
                    onPlay = { vodId ->
                        navController.navigate(Route.Player.create("vod", vodId))
                    },
                    onBack = { navController.popBackStack() },
                )
            } else {
                VodDetailScreen(
                    onPlay = { vodId ->
                        navController.navigate(Route.Player.create("vod", vodId))
                    },
                    onBack = { navController.popBackStack() },
                )
            }
        }

        composable(Route.SeriesCategories.route) {
            if (isTvDevice) {
                TvSeriesCategoriesScreen(
                    onSeriesClick = { seriesId ->
                        navController.navigate(Route.SeriesDetail.create(seriesId))
                    },
                    onBack = { navController.popBackStack() },
                )
            } else {
                SeriesCategoriesScreen(
                    onCategoryClick = {},
                    onSeriesClick = { seriesId ->
                        navController.navigate(Route.SeriesDetail.create(seriesId))
                    },
                    onBack = { navController.popBackStack() },
                )
            }
        }

        composable(
            route = Route.SeriesDetail.route,
            arguments = listOf(navArgument(NavArgs.SERIES_ID) { type = NavType.LongType }),
        ) {
            if (isTvDevice) {
                TvSeriesDetailScreen(
                    onEpisodeClick = { episodeId ->
                        navController.navigate(Route.Player.create("episode", episodeId))
                    },
                    onBack = { navController.popBackStack() },
                )
            } else {
                SeriesDetailScreen(
                    onEpisodeClick = { episodeId ->
                        navController.navigate(Route.Player.create("episode", episodeId))
                    },
                    onBack = { navController.popBackStack() },
                )
            }
        }

        // --- Player ---
        composable(
            route = Route.Player.route,
            arguments = listOf(
                navArgument(NavArgs.CONTENT_TYPE) { type = NavType.StringType },
                navArgument(NavArgs.CONTENT_ID) { type = NavType.LongType },
            ),
        ) {
            PlayerScreen(
                onBack = { navController.popBackStack() },
            )
        }

        // --- EPG ---
        composable(Route.EpgGrid.route) {
            if (isTvDevice) {
                TvEpgGridScreen(
                    onChannelClick = { channelId ->
                        navController.navigate(Route.Player.create("channel", channelId))
                    },
                    onBack = { navController.popBackStack() },
                )
            } else {
                EpgGridScreen(
                    onChannelClick = { channelId ->
                        navController.navigate(Route.Player.create("channel", channelId))
                    },
                    onBack = { navController.popBackStack() },
                )
            }
        }

        // --- Multi-View ---
        composable(Route.MultiView.route) {
            if (isTvDevice) {
                TvMultiViewScreen(
                    onChannelClick = { channelId ->
                        navController.navigate(Route.Player.create("channel", channelId))
                    },
                    onBack = { navController.popBackStack() },
                )
            } else {
                MultiViewScreen(
                    onBack = { navController.popBackStack() },
                )
            }
        }

        // --- Search ---
        composable(Route.Search.route) {
            if (isTvDevice) {
                TvSearchScreen(
                    onChannelClick = { channelId ->
                        navController.navigate(Route.Player.create("channel", channelId))
                    },
                    onVodClick = { vodId ->
                        navController.navigate(Route.VodDetail.create(vodId))
                    },
                    onSeriesClick = { seriesId ->
                        navController.navigate(Route.SeriesDetail.create(seriesId))
                    },
                    onBack = { navController.popBackStack() },
                )
            } else {
                SearchScreen(
                    onItemClick = { contentType, contentId ->
                        when (contentType) {
                            "series" -> navController.navigate(Route.SeriesDetail.create(contentId))
                            "vod" -> navController.navigate(Route.VodDetail.create(contentId))
                            else -> navController.navigate(Route.Player.create(contentType, contentId))
                        }
                    },
                    onBack = { navController.popBackStack() },
                )
            }
        }

        // --- Settings ---
        composable(Route.Settings.route) {
            if (isTvDevice) {
                TvSettingsScreen(
                    onBack = { navController.popBackStack() },
                )
            } else {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                )
            }
        }
        
        // --- Favorites ---
        composable(Route.Favorites.route) {
            if (isTvDevice) {
                TvFavoritesScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onChannelClick = { channelId ->
                        navController.navigate(Route.Player.create("channel", channelId))
                    },
                    onVodClick = { vodId ->
                        navController.navigate(Route.VodDetail.create(vodId))
                    },
                    onSeriesClick = { seriesId ->
                        navController.navigate(Route.SeriesDetail.create(seriesId))
                    },
                )
            } else {
                FavoritesScreen(
                    onChannelClick = { channelId ->
                        navController.navigate(Route.Player.create("channel", channelId))
                    },
                    onVodClick = { vodId ->
                        navController.navigate(Route.VodDetail.create(vodId))
                    },
                    onSeriesClick = { seriesId ->
                        navController.navigate(Route.SeriesDetail.create(seriesId))
                    },
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
