# VPN Integration - Nächste Schritte

## ✅ Abgeschlossene Implementierungen

### 1. VPN Setup Wizard (Komplett)
- **Mobile Version**: Vollständiger 6-Schritt Wizard
- **TV Version**: TV-optimierte Version mit Focus-Navigation
- **Features**:
  - Provider-Auswahl (7 Anbieter + eigene Konfiguration)
  - Login (Username/Password oder Account Number)
  - Config-Import (WireGuard/OpenVPN)
  - Server-Auswahl
  - Connection-Test (Speed-Test Simulation)
  - Setup-Abschluss mit Auto-Connect Option

### 2. VPN Infrastructure
- ✅ VpnRepository mit simulierten Servern
- ✅ VpnSetupRepository für Wizard-State
- ✅ VpnSetupViewModel
- ✅ VPN Settings (Mobile & TV)
- ✅ VPN Status im Dashboard
- ✅ Boot-Receiver für Auto-Connect
- ✅ DataStore Preferences für VPN-Einstellungen

### 3. Navigation Integration
- ✅ Route.VpnSetup in Routes.kt
- ✅ Integration in AppNavGraph (Mobile & TV)
- ✅ Setup Wizard Button in Settings

---

## 🔧 Nächste Schritte für echte VPN-Funktionalität

### A. WireGuard Library Integration

#### 1. Dependencies hinzufügen (build.gradle.kts)

```kotlin
// WireGuard Android Library
implementation("com.wireguard.android:backend:1.0.0")

// Optional: WireGuard UI Components
implementation("com.wireguard.android:ui:1.0.0")
```

#### 2. AndroidManifest.xml erweitern

```xml
<!-- VPN Service Permission -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />

<!-- VPN Service Declaration -->
<service
    android:name=".data.service.VpnService"
    android:enabled="true"
    android:exported="false"
    android:permission="android.permission.BIND_VPN_SERVICE">
    <intent-filter>
        <action android:name="android.net.VpnService" />
    </intent-filter>
</service>

<!-- Foreground Service -->
<service
    android:name=".data.service.VpnForegroundService"
    android:enabled="true"
    android:exported="false"
    android:foregroundServiceType="specialUse">
    <property
        android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
        android:value="vpn" />
</service>
```

#### 3. VpnService Implementierung

Erstelle neue Datei: `app/src/main/java/com/djoudini/iplayer/data/service/VpnService.kt`

```kotlin
package com.djoudini.iplayer.data.service

import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import com.wireguard.android.backend.WireGuardBackend
import com.wireguard.android.backend.GoBackend
import com.wireguard.config.Config
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@AndroidEntryPoint
class VpnService : VpnService() {

    @Inject
    lateinit var vpnStateManager: VpnStateManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val backend: WireGuardBackend = GoBackend.getInstance(this)
    
    private var vpnInterface: ParcelFileDescriptor? = null
    private var currentTunnelName: String? = null

    override fun onCreate() {
        super.onCreate()
        Timber.d("VpnService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                val config = intent.getStringExtra(EXTRA_CONFIG)
                val tunnelName = intent.getStringExtra(EXTRA_TUNNEL_NAME)
                startVpn(config, tunnelName)
            }
            ACTION_DISCONNECT -> stopVpn()
        }
        return START_STICKY
    }

    private fun startVpn(configString: String?, tunnelName: String?) {
        if (configString == null || tunnelName == null) {
            stopSelf()
            return
        }

        try {
            // Parse WireGuard config
            val config = Config.parse(configString)
            
            // Prepare VpnService builder
            val builder = Builder()
                .setMtu(config.`interface`.mtu ?: 1420)
                .addAddress(
                    config.`interface`.addresses.firstOrNull()?.address ?: "10.0.0.1",
                    config.`interface`.addresses.firstOrNull()?.networkPrefixLength ?: 24
                )
                .addDnsServer(config.`interface`.dns.firstOrNull()?.hostAddress ?: "8.8.8.8")
                .setBlocking(true)
                .setMetered(false)

            // Add routes
            config.peers.forEach { peer ->
                peer.allowedIps.forEach { ip ->
                    builder.addRoute(ip.address, ip.networkPrefixLength)
                }
            }

            // Establish VPN connection
            vpnInterface = builder.establish()
            currentTunnelName = tunnelName

            // Start WireGuard backend
            backend.setState(
                vpnInterface!!.fileDescriptor,
                config,
                null
            )

            // Update state
            vpnStateManager.setConnected(tunnelName)

            // Start foreground service
            startForeground(NOTIFICATION_ID, createNotification())

            Timber.d("VPN started: $tunnelName")

        } catch (e: Exception) {
            Timber.e(e, "Failed to start VPN")
            vpnStateManager.setError(e.message ?: "Unknown error")
            stopSelf()
        }
    }

    private fun stopVpn() {
        try {
            currentTunnelName?.let { name ->
                backend.setState(
                    vpnInterface?.fileDescriptor ?: return,
                    null,
                    null
                )
                vpnStateManager.setDisconnected(name)
            }

            vpnInterface?.close()
            vpnInterface = null
            currentTunnelName = null

            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()

            Timber.d("VPN stopped")

        } catch (e: Exception) {
            Timber.e(e, "Failed to stop VPN")
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, VPN_CHANNEL_ID)
            .setContentTitle("VPN ist aktiv")
            .setContentText(currentTunnelName ?: "Verbunden")
            .setSmallIcon(R.drawable.ic_vpn_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        stopVpn()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_CONNECT = "com.djoudini.iplayer.vpn.CONNECT"
        const val ACTION_DISCONNECT = "com.djoudini.iplayer.vpn.DISCONNECT"
        const val EXTRA_CONFIG = "config"
        const val EXTRA_TUNNEL_NAME = "tunnelName"
        const val NOTIFICATION_ID = 1001
        const val VPN_CHANNEL_ID = "vpn_service_channel"
    }
}
```

#### 4. VpnStateManager erstellen

```kotlin
package com.djoudini.iplayer.data.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VpnStateManager @Inject constructor() {
    
    private val _state = MutableStateFlow(VpnState.Disconnected)
    val state: StateFlow<VpnState> = _state.asStateFlow()

    fun setConnected(tunnelName: String) {
        _state.value = VpnState.Connected(tunnelName)
    }

    fun setDisconnected(tunnelName: String) {
        _state.value = VpnState.Disconnected
    }

    fun setError(message: String) {
        _state.value = VpnState.Error(message)
    }

    fun isConnected(): Boolean {
        return _state.value is VpnState.Connected
    }
}

sealed class VpnState {
    object Disconnected : VpnState()
    data class Connected(val tunnelName: String) : VpnState()
    data class Error(val message: String) : VpnState()
}
```

#### 5. VpnRepositoryImpl aktualisieren

Aktualisiere `VpnRepositoryImpl.kt` um echte VPN-Funktionalität:

```kotlin
@Inject
lateinit var vpnStateManager: VpnStateManager

override suspend fun connect(serverId: String): Result<Unit> {
    return withContext(Dispatchers.Default) {
        try {
            // Get config for server
            val config = getVpnConfigForServer(serverId)
            
            // Start VPN service
            val intent = Intent(context, VpnService::class.java).apply {
                action = VpnService.ACTION_CONNECT
                putExtra(VpnService.EXTRA_CONFIG, config)
                putExtra(VpnService.EXTRA_TUNNEL_NAME, serverId)
            }
            
            // Request VPN permission and start service
            // This needs to be called from an Activity context
            context.startActivity(intent)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

private suspend fun getVpnConfigForServer(serverId: String): String {
    // Fetch WireGuard config from provider API
    // or generate from stored credentials
    return """
        [Interface]
        PrivateKey = YOUR_PRIVATE_KEY
        Address = 10.0.0.2/24
        DNS = 8.8.8.8
        
        [Peer]
        PublicKey = SERVER_PUBLIC_KEY
        Endpoint = ${getServerEndpoint(serverId)}
        AllowedIPs = 0.0.0.0/0
    """.trimIndent()
}
```

---

### B. Provider-API Integration

#### 1. Mullvad API

```kotlin
// Mullvad verwendet Account Numbers
// Configs können von der Website heruntergeladen werden
// API: https://api.mullvad.net/

suspend fun fetchMullvadServers(accountNumber: String): List<VpnServer> {
    // Implement API call
    // Returns list of available servers
}

suspend fun fetchMullvadConfig(accountNumber: String, serverId: String): String {
    // Download WireGuard config
    // Returns config string
}
```

#### 2. ProtonVPN API

```kotlin
// ProtonVPN hat eine öffentliche API
// API: https://api.protonvpn.ch/

// Benötigt OAuth 2.0 Authentication
```

---

### C. UI Verbesserungen

#### 1. Echte Speed-Test Integration

```kotlin
// Verwende OkHttp für echte Speed-Tests
suspend fun performSpeedTest(): VpnSpeedTestResult {
    val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    // Download test
    val downloadStart = System.currentTimeMillis()
    val downloadRequest = Request.Builder()
        .url("https://speedtest.net/test file URL")
        .build()
    val downloadResponse = client.newCall(downloadRequest).execute()
    val downloadSize = downloadResponse.body?.bytes()?.size ?: 0
    val downloadTime = System.currentTimeMillis() - downloadStart
    val downloadSpeed = (downloadSize * 8) / downloadTime / 1000000 // Mbps
    
    // Upload test (similar)
    
    return VpnSpeedTestResult(
        downloadSpeedMbps = downloadSpeed,
        uploadSpeedMbps = uploadSpeed,
        pingMs = ping
    )
}
```

#### 2. IP-Check Integration

```kotlin
suspend fun checkIpAddress(): String {
    val client = OkHttpClient()
    val request = Request.Builder()
        .url("https://api.ipify.org?format=json")
        .build()
    
    val response = client.newCall(request).execute()
    val json = response.body?.string() ?: return "Unknown"
    
    // Parse JSON to get IP
    return Json.parseToJsonElement(json).jsonObject["ip"]?.toString() ?: "Unknown"
}
```

---

### D. Testing

#### 1. Unit Tests für VpnRepository

```kotlin
@Test
fun `connect should start VPN service`() = runTest {
    // Given
    val serverId = "de-frankfurt"
    
    // When
    val result = vpnRepository.connect(serverId)
    
    // Then
    assertTrue(result.isSuccess)
    verify(mockVpnService).start(serverId)
}
```

#### 2. Integration Tests

```kotlin
@Test
fun `VPN connection should change state to Connected`() = runTest {
    // Test VPN connection flow
}
```

---

## 📦 Dependencies für echte VPN-Funktion

In `app/build.gradle.kts` hinzufügen:

```kotlin
dependencies {
    // WireGuard
    implementation("com.wireguard.android:backend:1.0.0")
    
    // OkHttp für API Calls
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    
    // Kotlinx Serialization für JSON
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}
```

---

## 🚀 Zusammenfassung

Die aktuelle Implementierung bietet:
- ✅ Vollständiger Setup Wizard (Mobile & TV)
- ✅ State Management für VPN Setup
- ✅ UI für alle Setup-Schritte
- ✅ Navigation Integration
- ✅ Simulation der VPN-Funktionalität

Für echte VPN-Funktionalität:
1. WireGuard Library integrieren
2. VpnService implementieren
3. Provider-APIs anbinden
4. Echte Speed-Tests implementieren
5. IP-Check integrieren

Die bestehende Architektur ist darauf vorbereitet und kann schrittweise erweitert werden!
