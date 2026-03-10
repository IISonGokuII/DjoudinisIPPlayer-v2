package com.djoudini.iplayer.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class XtreamAuthResponse(
    @Json(name = "user_info") val userInfo: XtreamUserInfo?,
    @Json(name = "server_info") val serverInfo: XtreamServerInfo?,
)

@JsonClass(generateAdapter = true)
data class XtreamUserInfo(
    @Json(name = "username") val username: String?,
    @Json(name = "password") val password: String?,
    @Json(name = "message") val message: String?,
    @Json(name = "auth") val auth: Int?,
    @Json(name = "status") val status: String?,
    @Json(name = "exp_date") val expDate: String?,
    @Json(name = "is_trial") val isTrial: String?,
    @Json(name = "active_cons") val activeCons: String?,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "max_connections") val maxConnections: String?,
    @Json(name = "allowed_output_formats") val allowedOutputFormats: List<String>?,
)

@JsonClass(generateAdapter = true)
data class XtreamServerInfo(
    @Json(name = "url") val url: String?,
    @Json(name = "port") val port: String?,
    @Json(name = "https_port") val httpsPort: String?,
    @Json(name = "server_protocol") val serverProtocol: String?,
    @Json(name = "rtmp_port") val rtmpPort: String?,
    @Json(name = "timezone") val timezone: String?,
    @Json(name = "timestamp_now") val timestampNow: Long?,
    @Json(name = "time_now") val timeNow: String?,
)

@JsonClass(generateAdapter = true)
data class XtreamCategoryDto(
    @Json(name = "category_id") val categoryId: String?,
    @Json(name = "category_name") val categoryName: String?,
    @Json(name = "parent_id") val parentId: Int?,
)

@JsonClass(generateAdapter = true)
data class XtreamStreamDto(
    @Json(name = "num") val num: Int?,
    @Json(name = "name") val name: String?,
    @Json(name = "stream_type") val streamType: String?,
    @Json(name = "stream_id") val streamId: Int?,
    @Json(name = "stream_icon") val streamIcon: String?,
    @Json(name = "epg_channel_id") val epgChannelId: String?,
    @Json(name = "added") val added: String?,
    @Json(name = "category_id") val categoryId: String?,
    @Json(name = "custom_sid") val customSid: String?,
    @Json(name = "tv_archive") val tvArchive: Int?,
    @Json(name = "direct_source") val directSource: String?,
    @Json(name = "tv_archive_duration") val tvArchiveDuration: Int?,
    @Json(name = "container_extension") val containerExtension: String?,
    // VOD-specific
    @Json(name = "plot") val plot: String?,
    @Json(name = "cast") val cast: String?,
    @Json(name = "director") val director: String?,
    @Json(name = "genre") val genre: String?,
    @Json(name = "release_date") val releaseDate: String?,
    @Json(name = "rating") val rating: String?,
    @Json(name = "tmdb") val tmdb: String?,
    // Series-specific
    @Json(name = "series_id") val seriesId: Int?,
    @Json(name = "cover") val cover: String?,
    @Json(name = "last_modified") val lastModified: String?,
)

@JsonClass(generateAdapter = true)
data class XtreamSeriesInfoResponse(
    @Json(name = "seasons") val seasons: List<XtreamSeasonDto>?,
    @Json(name = "info") val info: XtreamStreamDto?,
    @Json(name = "episodes") val episodes: Map<String, List<XtreamEpisodeDto>>?,
)

@JsonClass(generateAdapter = true)
data class XtreamSeasonDto(
    @Json(name = "air_date") val airDate: String?,
    @Json(name = "episode_count") val episodeCount: Int?,
    @Json(name = "id") val id: Int?,
    @Json(name = "name") val name: String?,
    @Json(name = "overview") val overview: String?,
    @Json(name = "season_number") val seasonNumber: Int?,
    @Json(name = "cover") val cover: String?,
)

@JsonClass(generateAdapter = true)
data class XtreamEpisodeDto(
    @Json(name = "id") val id: String?,
    @Json(name = "episode_num") val episodeNum: Int?,
    @Json(name = "title") val title: String?,
    @Json(name = "container_extension") val containerExtension: String?,
    @Json(name = "info") val info: XtreamEpisodeInfoDto?,
    @Json(name = "season") val season: Int?,
    @Json(name = "direct_source") val directSource: String?,
)

@JsonClass(generateAdapter = true)
data class XtreamEpisodeInfoDto(
    @Json(name = "tmdb_id") val tmdbId: Int?,
    @Json(name = "release_date") val releaseDate: String?,
    @Json(name = "plot") val plot: String?,
    @Json(name = "duration_secs") val durationSecs: Int?,
    @Json(name = "duration") val duration: String?,
    @Json(name = "movie_image") val movieImage: String?,
    @Json(name = "rating") val rating: Double?,
)
