package com.djoudini.iplayer.data.remote.api

import com.djoudini.iplayer.data.remote.dto.TraktHistoryItem
import com.djoudini.iplayer.data.remote.dto.TraktSyncRequest
import com.djoudini.iplayer.data.remote.dto.TraktSyncResponse
import com.djoudini.iplayer.data.remote.dto.TraktTokenRequest
import com.djoudini.iplayer.data.remote.dto.TraktTokenResponse
import com.djoudini.iplayer.data.remote.dto.TraktWatchlistItem
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Trakt.tv API for watch progress sync and watchlist management.
 * Requires OAuth2 authentication (device code flow for TV).
 */
interface TraktApi {

    // --- OAuth ---

    @POST("oauth/token")
    suspend fun getToken(
        @Body request: TraktTokenRequest,
    ): TraktTokenResponse

    // --- Sync: History (watched items) ---

    @POST("sync/history")
    suspend fun addToHistory(
        @Header("Authorization") bearer: String,
        @Body request: TraktSyncRequest,
    ): TraktSyncResponse

    @POST("sync/history/remove")
    suspend fun removeFromHistory(
        @Header("Authorization") bearer: String,
        @Body request: TraktSyncRequest,
    ): TraktSyncResponse

    @GET("sync/history/movies")
    suspend fun getMovieHistory(
        @Header("Authorization") bearer: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50,
    ): List<TraktHistoryItem>

    @GET("sync/history/episodes")
    suspend fun getEpisodeHistory(
        @Header("Authorization") bearer: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50,
    ): List<TraktHistoryItem>

    // --- Sync: Watchlist ---

    @POST("sync/watchlist")
    suspend fun addToWatchlist(
        @Header("Authorization") bearer: String,
        @Body request: TraktSyncRequest,
    ): TraktSyncResponse

    @POST("sync/watchlist/remove")
    suspend fun removeFromWatchlist(
        @Header("Authorization") bearer: String,
        @Body request: TraktSyncRequest,
    ): TraktSyncResponse

    @GET("sync/watchlist/movies")
    suspend fun getMovieWatchlist(
        @Header("Authorization") bearer: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50,
    ): List<TraktWatchlistItem>

    @GET("sync/watchlist/shows")
    suspend fun getShowWatchlist(
        @Header("Authorization") bearer: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50,
    ): List<TraktWatchlistItem>

    // --- Playback Progress (Scrobble) ---

    @POST("scrobble/start")
    suspend fun scrobbleStart(
        @Header("Authorization") bearer: String,
        @Body request: TraktSyncRequest,
    ): TraktSyncResponse

    @POST("scrobble/pause")
    suspend fun scrobblePause(
        @Header("Authorization") bearer: String,
        @Body request: TraktSyncRequest,
    ): TraktSyncResponse

    @POST("scrobble/stop")
    suspend fun scrobbleStop(
        @Header("Authorization") bearer: String,
        @Body request: TraktSyncRequest,
    ): TraktSyncResponse
}
