package com.djoudini.iplayer.data.remote.api

import com.djoudini.iplayer.data.remote.dto.XtreamAuthResponse
import com.djoudini.iplayer.data.remote.dto.XtreamCategoryDto
import com.djoudini.iplayer.data.remote.dto.XtreamSeriesInfoResponse
import com.djoudini.iplayer.data.remote.dto.XtreamStreamDto
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * Retrofit interface for Xtream Codes API.
 * All player_api.php endpoints receive a full @Url to support dynamic server URLs per playlist.
 */
interface XtreamApi {

    /** Authenticate and get account info (expiration, max connections, etc.) */
    @GET
    suspend fun authenticate(
        @Url url: String,
        @Query("username") username: String,
        @Query("password") password: String,
    ): XtreamAuthResponse

    /** Get live TV categories */
    @GET
    suspend fun getLiveCategories(
        @Url url: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_live_categories",
    ): List<XtreamCategoryDto>

    /** Get live TV streams */
    @GET
    suspend fun getLiveStreams(
        @Url url: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_live_streams",
        @Query("category_id") categoryId: String? = null,
    ): List<XtreamStreamDto>

    /** Get VOD categories */
    @GET
    suspend fun getVodCategories(
        @Url url: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_vod_categories",
    ): List<XtreamCategoryDto>

    /** Get VOD streams */
    @GET
    suspend fun getVodStreams(
        @Url url: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_vod_streams",
        @Query("category_id") categoryId: String? = null,
    ): List<XtreamStreamDto>

    /** Get series categories */
    @GET
    suspend fun getSeriesCategories(
        @Url url: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_series_categories",
    ): List<XtreamCategoryDto>

    /** Get series list */
    @GET
    suspend fun getSeries(
        @Url url: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_series",
        @Query("category_id") categoryId: String? = null,
    ): List<XtreamStreamDto>

    /** Get series info (episodes) */
    @GET
    suspend fun getSeriesInfo(
        @Url url: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_series_info",
        @Query("series_id") seriesId: String,
    ): XtreamSeriesInfoResponse

    /** Get VOD info (detailed info with plot, cast, director, etc.) */
    @GET
    suspend fun getVodInfo(
        @Url url: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_vod_info",
        @Query("vod_id") vodId: String,
    ): XtreamSeriesInfoResponse

    /** Fetch EPG by stream using short EPG */
    @GET
    suspend fun getShortEpg(
        @Url url: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_short_epg",
        @Query("stream_id") streamId: String,
        @Query("limit") limit: Int = 10,
    ): Map<String, Any>

    /** Fetch full M3U or XMLTV by absolute URL */
    @GET
    suspend fun fetchRawUrl(@Url url: String): okhttp3.ResponseBody
}
