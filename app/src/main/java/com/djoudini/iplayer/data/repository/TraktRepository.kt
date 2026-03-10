package com.djoudini.iplayer.data.repository

import com.djoudini.iplayer.data.local.dao.WatchProgressDao
import com.djoudini.iplayer.data.local.preferences.AppPreferences
import com.djoudini.iplayer.data.remote.api.TraktApi
import com.djoudini.iplayer.data.remote.dto.TraktIds
import com.djoudini.iplayer.data.remote.dto.TraktMediaItem
import com.djoudini.iplayer.data.remote.dto.TraktSyncRequest
import com.djoudini.iplayer.data.remote.dto.TraktTokenRequest
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Trakt.tv synchronization.
 * Handles:
 * - OAuth token management (with refresh)
 * - Syncing completed watches to Trakt history
 * - Scrobble (real-time progress reporting)
 * - Fetching watchlist
 */
@Singleton
class TraktRepository @Inject constructor(
    private val traktApi: TraktApi,
    private val watchProgressDao: WatchProgressDao,
    private val appPreferences: AppPreferences,
) {
    /**
     * Sync all unsynced watch progress entries to Trakt.tv.
     * Only syncs items that have a TMDB ID and are marked as completed.
     */
    suspend fun syncWatchedToTrakt() {
        val token = appPreferences.traktAccessToken.first() ?: run {
            Timber.w("Trakt sync skipped: no access token")
            return
        }
        val bearer = "Bearer $token"

        val unsyncedItems = watchProgressDao.getUnsyncedForTrakt()
        if (unsyncedItems.isEmpty()) {
            Timber.d("Trakt sync: nothing to sync")
            return
        }

        // Group by content type and batch sync
        val completedItems = unsyncedItems.filter { it.isCompleted && it.traktId != null }

        if (completedItems.isEmpty()) return

        // Build movie sync request
        val movieItems = completedItems
            .filter { it.contentType == "vod" }
            .map { progress ->
                TraktMediaItem(
                    ids = TraktIds(tmdb = progress.traktId),
                    watchedAt = java.time.Instant.ofEpochMilli(progress.lastWatchedAt).toString(),
                )
            }

        // Build episode sync request
        val episodeItems = completedItems
            .filter { it.contentType == "episode" }
            .map { progress ->
                TraktMediaItem(
                    ids = TraktIds(tmdb = progress.traktId),
                    watchedAt = java.time.Instant.ofEpochMilli(progress.lastWatchedAt).toString(),
                )
            }

        try {
            if (movieItems.isNotEmpty()) {
                val request = TraktSyncRequest(movies = movieItems)
                val response = traktApi.addToHistory(bearer, request)
                Timber.i("Trakt sync: added ${response.added?.movies ?: 0} movies")
            }

            if (episodeItems.isNotEmpty()) {
                val request = TraktSyncRequest(episodes = episodeItems)
                val response = traktApi.addToHistory(bearer, request)
                Timber.i("Trakt sync: added ${response.added?.episodes ?: 0} episodes")
            }

            // Mark all as synced
            completedItems.forEach { progress ->
                watchProgressDao.markTraktSynced(progress.id)
            }
        } catch (e: Exception) {
            Timber.e(e, "Trakt sync failed")
            // Don't mark as synced; will retry next time
        }
    }

    /**
     * Send scrobble start event (when playback begins).
     */
    suspend fun scrobbleStart(tmdbId: Int, contentType: String, progress: Float) {
        val token = appPreferences.traktAccessToken.first() ?: return
        val bearer = "Bearer $token"

        val item = TraktMediaItem(ids = TraktIds(tmdb = tmdbId))
        val request = when (contentType) {
            "vod" -> TraktSyncRequest(movie = item, progress = progress * 100)
            "episode" -> TraktSyncRequest(episode = item, progress = progress * 100)
            else -> return
        }

        try {
            traktApi.scrobbleStart(bearer, request)
        } catch (e: Exception) {
            Timber.w(e, "Trakt scrobble start failed")
        }
    }

    /**
     * Send scrobble stop event (when playback ends or user exits).
     */
    suspend fun scrobbleStop(tmdbId: Int, contentType: String, progress: Float) {
        val token = appPreferences.traktAccessToken.first() ?: return
        val bearer = "Bearer $token"

        val item = TraktMediaItem(ids = TraktIds(tmdb = tmdbId))
        val request = when (contentType) {
            "vod" -> TraktSyncRequest(movie = item, progress = progress * 100)
            "episode" -> TraktSyncRequest(episode = item, progress = progress * 100)
            else -> return
        }

        try {
            traktApi.scrobbleStop(bearer, request)
        } catch (e: Exception) {
            Timber.w(e, "Trakt scrobble stop failed")
        }
    }

    /**
     * Exchange authorization code for access + refresh tokens.
     */
    suspend fun exchangeCodeForToken(code: String, clientId: String, clientSecret: String) {
        val request = TraktTokenRequest(
            code = code,
            clientId = clientId,
            clientSecret = clientSecret,
            grantType = "authorization_code",
        )

        val response = traktApi.getToken(request)
        val expiresAt = (response.createdAt + response.expiresIn) * 1000

        appPreferences.saveTraktTokens(
            accessToken = response.accessToken,
            refreshToken = response.refreshToken,
            expiresAt = expiresAt,
        )

        Timber.i("Trakt: tokens saved, expires at $expiresAt")
    }

    /**
     * Refresh expired access token using the refresh token.
     */
    suspend fun refreshToken(clientId: String, clientSecret: String): Boolean {
        val refreshToken = appPreferences.getTraktRefreshToken() ?: return false

        return try {
            val request = TraktTokenRequest(
                refreshToken = refreshToken,
                clientId = clientId,
                clientSecret = clientSecret,
                grantType = "refresh_token",
            )
            val response = traktApi.getToken(request)
            val expiresAt = (response.createdAt + response.expiresIn) * 1000

            appPreferences.saveTraktTokens(
                accessToken = response.accessToken,
                refreshToken = response.refreshToken,
                expiresAt = expiresAt,
            )
            true
        } catch (e: Exception) {
            Timber.e(e, "Trakt token refresh failed")
            false
        }
    }

    /**
     * Disconnect Trakt.tv account.
     */
    suspend fun disconnect() {
        appPreferences.clearTraktTokens()
    }
}
