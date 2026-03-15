package com.djoudini.iplayer.data.remote.api

import com.djoudini.iplayer.data.remote.dto.FootballDataMatchesResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface FootballDataApi {
    @GET("v4/matches")
    suspend fun getMatches(
        @Header("X-Auth-Token") authToken: String? = null,
        @Query("status") status: String? = null,
        @Query("dateFrom") dateFrom: String? = null,
        @Query("dateTo") dateTo: String? = null,
        @Query("competitions") competitions: String? = null,
    ): FootballDataMatchesResponse
}
