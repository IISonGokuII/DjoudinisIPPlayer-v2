package com.djoudini.iplayer.di

import com.djoudini.iplayer.data.local.entity.PlayerConfig
import com.djoudini.iplayer.data.remote.api.TraktApi
import com.djoudini.iplayer.data.remote.api.XtreamApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(@ApplicationContext context: Context): OkHttpClient {
        val cacheSize = 50L * 1024 * 1024 // 50 MB HTTP cache
        val cache = Cache(context.cacheDir.resolve("http_cache"), cacheSize)
        return OkHttpClient.Builder()
            .cache(cache)
            // Längere Timeouts für langsame IPTV-Server und Fire TV
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS) // Erhöht von 120s auf 180s für Fire TV
            .writeTimeout(60, TimeUnit.SECONDS) // Erhöht für langsame Uploads
            .connectionPool(ConnectionPool(10, 5, TimeUnit.MINUTES))
            .retryOnConnectionFailure(true)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", PlayerConfig.DEFAULT_USER_AGENT)
                    .build()
                chain.proceed(request)
            }
            // Logging nur im Debug-Build für Performance
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                }
            )
            .build()
    }

    @Provides
    @Singleton
    fun provideXtreamApi(okHttpClient: OkHttpClient, moshi: Moshi): XtreamApi {
        return Retrofit.Builder()
            .baseUrl("https://localhost/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
            .build()
            .create(XtreamApi::class.java)
    }

    @Provides
    @Singleton
    fun provideTraktApi(okHttpClient: OkHttpClient, moshi: Moshi): TraktApi {
        val traktClient = okHttpClient.newBuilder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("Content-Type", "application/json")
                    .header("trakt-api-version", "2")
                    .header("trakt-api-key", "") // Set via BuildConfig in production
                    .build()
                chain.proceed(request)
            }
            .build()

        return Retrofit.Builder()
            .baseUrl("https://api.trakt.tv/")
            .client(traktClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(TraktApi::class.java)
    }
}
