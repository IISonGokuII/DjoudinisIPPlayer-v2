package com.djoudini.iplayer.data.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * IP Check Service.
 * Checks current public IP address and location.
 */
@Singleton
class IpCheckService @Inject constructor() {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Get current public IP address.
     */
    suspend fun getIpAddress(): IpInfo = withContext(Dispatchers.IO) {
        try {
            // Try multiple IP check services
            val services = listOf(
                "https://api.ipify.org?format=json",
                "https://ipapi.co/json/",
                "https://ipinfo.io/json",
            )
            
            for (serviceUrl in services) {
                try {
                    val request = Request.Builder()
                        .url(serviceUrl)
                        .build()
                    
                    val response = okHttpClient.newCall(request).execute()
                    val jsonString = response.body?.string() ?: continue
                    
                    return@withContext parseIpResponse(jsonString, serviceUrl)
                } catch (e: Exception) {
                    // Try next service
                    continue
                }
            }
            
            IpInfo(error = "All IP check services failed")
        } catch (e: Exception) {
            IpInfo(error = e.message)
        }
    }

    /**
     * Parse IP response from different services.
     */
    private fun parseIpResponse(jsonString: String, serviceUrl: String): IpInfo {
        return try {
            when {
                serviceUrl.contains("ipify") -> {
                    val response = json.decodeFromString<IpifyResponse>(jsonString)
                    IpInfo(ip = response.ip)
                }
                serviceUrl.contains("ipapi") -> {
                    val response = json.decodeFromString<IpApiResponse>(jsonString)
                    IpInfo(
                        ip = response.ip,
                        city = response.city,
                        region = response.region,
                        country = response.countryName,
                        isp = response.org,
                    )
                }
                serviceUrl.contains("ipinfo") -> {
                    val response = json.decodeFromString<IpInfoResponse>(jsonString)
                    IpInfo(
                        ip = response.ip,
                        city = response.city,
                        region = response.region,
                        country = response.country,
                        isp = response.org,
                    )
                }
                else -> IpInfo(error = "Unknown service")
            }
        } catch (e: Exception) {
            IpInfo(error = e.message)
        }
    }

    /**
     * Check if IP changed (for VPN verification).
     */
    suspend fun verifyIpChange(previousIp: String): Boolean {
        val currentIp = getIpAddress()
        return currentIp.ip != previousIp && currentIp.error == null
    }

    /**
     * Get IP with retry logic.
     */
    suspend fun getIpAddressWithRetries(retries: Int = 3): IpInfo {
        var lastError: String? = null
        
        for (i in 0 until retries) {
            val result = getIpAddress()
            if (result.error == null) {
                return result
            }
            lastError = result.error
            
            if (i < retries - 1) {
                kotlinx.coroutines.delay(1000L * (i + 1)) // Exponential backoff
            }
        }
        
        return IpInfo(error = lastError ?: "Unknown error")
    }
}

/**
 * IP information data class.
 */
data class IpInfo(
    val ip: String = "",
    val city: String? = null,
    val region: String? = null,
    val country: String? = null,
    val isp: String? = null,
    val error: String? = null,
) {
    val displayLocation: String
        get() = listOfNotNull(city, country).joinToString(", ")
            .ifEmpty { "Unknown" }
}

// ==================== API Response Models ====================

@Serializable
data class IpifyResponse(
    @SerialName("ip")
    val ip: String,
)

@Serializable
data class IpApiResponse(
    @SerialName("ip")
    val ip: String,
    @SerialName("city")
    val city: String? = null,
    @SerialName("region")
    val region: String? = null,
    @SerialName("country_name")
    val countryName: String? = null,
    @SerialName("org")
    val org: String? = null,
)

@Serializable
data class IpInfoResponse(
    @SerialName("ip")
    val ip: String,
    @SerialName("city")
    val city: String? = null,
    @SerialName("region")
    val region: String? = null,
    @SerialName("country")
    val country: String? = null,
    @SerialName("org")
    val org: String? = null,
)
