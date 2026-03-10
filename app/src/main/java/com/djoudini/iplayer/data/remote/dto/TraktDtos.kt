package com.djoudini.iplayer.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// --- OAuth ---

@JsonClass(generateAdapter = true)
data class TraktTokenRequest(
    @Json(name = "code") val code: String? = null,
    @Json(name = "refresh_token") val refreshToken: String? = null,
    @Json(name = "client_id") val clientId: String,
    @Json(name = "client_secret") val clientSecret: String,
    @Json(name = "redirect_uri") val redirectUri: String = "urn:ietf:wg:oauth:2.0:oob",
    @Json(name = "grant_type") val grantType: String = "authorization_code",
)

@JsonClass(generateAdapter = true)
data class TraktTokenResponse(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "token_type") val tokenType: String,
    @Json(name = "expires_in") val expiresIn: Long,
    @Json(name = "refresh_token") val refreshToken: String,
    @Json(name = "scope") val scope: String,
    @Json(name = "created_at") val createdAt: Long,
)

// --- Device Code Flow (for TV) ---

@JsonClass(generateAdapter = true)
data class TraktDeviceCodeRequest(
    @Json(name = "client_id") val clientId: String,
)

@JsonClass(generateAdapter = true)
data class TraktDeviceCodeResponse(
    @Json(name = "device_code") val deviceCode: String,
    @Json(name = "user_code") val userCode: String,
    @Json(name = "verification_url") val verificationUrl: String,
    @Json(name = "expires_in") val expiresIn: Int,
    @Json(name = "interval") val interval: Int,
)

// --- Sync ---

@JsonClass(generateAdapter = true)
data class TraktSyncRequest(
    @Json(name = "movies") val movies: List<TraktMediaItem>? = null,
    @Json(name = "shows") val shows: List<TraktMediaItem>? = null,
    @Json(name = "episodes") val episodes: List<TraktMediaItem>? = null,
    // Scrobble fields
    @Json(name = "movie") val movie: TraktMediaItem? = null,
    @Json(name = "episode") val episode: TraktMediaItem? = null,
    @Json(name = "progress") val progress: Float? = null,
)

@JsonClass(generateAdapter = true)
data class TraktMediaItem(
    @Json(name = "ids") val ids: TraktIds? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "year") val year: Int? = null,
    @Json(name = "watched_at") val watchedAt: String? = null, // ISO 8601
    @Json(name = "season") val season: Int? = null,
    @Json(name = "number") val number: Int? = null,
)

@JsonClass(generateAdapter = true)
data class TraktIds(
    @Json(name = "trakt") val trakt: Int? = null,
    @Json(name = "slug") val slug: String? = null,
    @Json(name = "imdb") val imdb: String? = null,
    @Json(name = "tmdb") val tmdb: Int? = null,
)

@JsonClass(generateAdapter = true)
data class TraktSyncResponse(
    @Json(name = "added") val added: TraktSyncCount? = null,
    @Json(name = "deleted") val deleted: TraktSyncCount? = null,
    @Json(name = "not_found") val notFound: TraktSyncNotFound? = null,
)

@JsonClass(generateAdapter = true)
data class TraktSyncCount(
    @Json(name = "movies") val movies: Int? = null,
    @Json(name = "episodes") val episodes: Int? = null,
)

@JsonClass(generateAdapter = true)
data class TraktSyncNotFound(
    @Json(name = "movies") val movies: List<TraktMediaItem>? = null,
    @Json(name = "episodes") val episodes: List<TraktMediaItem>? = null,
)

// --- History ---

@JsonClass(generateAdapter = true)
data class TraktHistoryItem(
    @Json(name = "id") val id: Long,
    @Json(name = "watched_at") val watchedAt: String,
    @Json(name = "action") val action: String,
    @Json(name = "type") val type: String,
    @Json(name = "movie") val movie: TraktMediaItem? = null,
    @Json(name = "episode") val episode: TraktMediaItem? = null,
    @Json(name = "show") val show: TraktMediaItem? = null,
)

// --- Watchlist ---

@JsonClass(generateAdapter = true)
data class TraktWatchlistItem(
    @Json(name = "rank") val rank: Int,
    @Json(name = "id") val id: Long,
    @Json(name = "listed_at") val listedAt: String,
    @Json(name = "type") val type: String,
    @Json(name = "movie") val movie: TraktMediaItem? = null,
    @Json(name = "show") val show: TraktMediaItem? = null,
)
