package com.djoudini.iplayer.data.remote.api

import com.djoudini.iplayer.data.remote.dto.TmdbCreditsDto
import com.djoudini.iplayer.presentation.viewmodel.CastMember
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val TMDB_BASE_URL = "https://api.themoviedb.org/3/"
private const val TMDB_API_KEY = "YOUR_TMDB_API_KEY"

@Singleton
class TmdbApi @Inject constructor() {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(TMDB_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    private val tmdbService = retrofit.create(TmdbService::class.java)

    suspend fun getMovieCredits(movieId: Int): TmdbCreditsDto {
        return tmdbService.getMovieCredits(movieId, TMDB_API_KEY)
    }

    suspend fun getCast(movieId: Int): List<CastMember> {
        return try {
            val credits = getMovieCredits(movieId)
            credits.cast.take(10).map { actor ->
                CastMember(
                    name = actor.name,
                    character = actor.character,
                    profilePath = actor.profilePath,
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private interface TmdbService {
        @GET("movie/{movie_id}/credits")
        suspend fun getMovieCredits(
            @Path("movie_id") movieId: Int,
            @Query("api_key") apiKey: String,
        ): TmdbCreditsDto
    }
}
