package com.djoudini.iplayer.data.local.entity

/**
 * VPN Provider information.
 */
data class VpnProviderInfo(
    val id: String,
    val name: String,
    val displayName: String,
    val logoResId: Int? = null, // Local resource ID
    val websiteUrl: String,
    val setupGuideUrl: String,
    val supportedProtocols: List<VpnProtocol>,
    val authType: VpnAuthType,
    val hasApi: Boolean = false,
    val apiBaseUrl: String? = null,
    val configFormat: VpnConfigFormat,
    val isPopular: Boolean = false,
    val isPremium: Boolean = true,
    val features: List<String> = emptyList(),
    val description: String? = null,
)

/**
 * VPN Authentication Type.
 */
enum class VpnAuthType {
    NONE,           // No authentication needed (built-in servers)
    USERNAME_PASSWORD,  // Traditional username/password
    ACCOUNT_NUMBER,     // Account number (like Mullvad)
    API_KEY,           // API key authentication
    OAUTH,            // OAuth 2.0
    CERTIFICATE,      // Certificate-based
    MANUAL_CONFIG     // Manual configuration file
}

/**
 * VPN Config File Format.
 */
enum class VpnConfigFormat {
    WIREGUARD,    // .conf files
    OPENVPN,      // .ovpn files
    BOTH,
    CUSTOM
}

/**
 * VPN Setup Step in the wizard.
 */
sealed class VpnSetupStep {
    object ProviderSelection : VpnSetupStep()
    object AuthMethod : VpnSetupStep()
    object Login : VpnSetupStep()
    object ConfigImport : VpnSetupStep()
    object ServerSelection : VpnSetupStep()
    object ConnectionTest : VpnSetupStep()
    object Complete : VpnSetupStep()
    
    val stepNumber: Int
        get() = when (this) {
            is ProviderSelection -> 1
            is AuthMethod -> 2
            is Login -> 3
            is ConfigImport -> 3
            is ServerSelection -> 4
            is ConnectionTest -> 5
            is Complete -> 6
        }
    
    val totalSteps: Int = 6
}

/**
 * VPN Setup State.
 */
data class VpnSetupState(
    val currentStep: VpnSetupStep = VpnSetupStep.ProviderSelection,
    val selectedProvider: VpnProviderInfo? = null,
    val authMethod: VpnAuthType = VpnAuthType.NONE,
    val username: String? = null,
    val password: String? = null,
    val accountNumber: String? = null,
    val apiKey: String? = null,
    val configFilePath: String? = null,
    val selectedServerId: String? = null,
    val connectionTestResult: VpnConnectionTestResult? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    val progress: Float
        get() = currentStep.stepNumber.toFloat() / currentStep.totalSteps.toFloat()
}

/**
 * VPN Connection Test Result.
 */
data class VpnConnectionTestResult(
    val success: Boolean,
    val pingMs: Int? = null,
    val downloadSpeedMbps: Int? = null,
    val uploadSpeedMbps: Int? = null,
    val serverLocation: String? = null,
    val newIpAddress: String? = null,
    val errorMessage: String? = null,
)

/**
 * Popular VPN Providers (predefined list).
 */
object KnownVpnProviders {
    
    val BUILTIN_FREE = VpnProviderInfo(
        id = "builtin_free",
        name = "Kostenlose Server",
        displayName = "Integrierte Kostenlose Server",
        websiteUrl = "",
        setupGuideUrl = "",
        supportedProtocols = listOf(VpnProtocol.WIREGUARD),
        authType = VpnAuthType.NONE,
        hasApi = false,
        configFormat = VpnConfigFormat.CUSTOM,
        isPopular = true,
        isPremium = false,
        features = listOf(
            "Keine Registrierung",
            "Sofort nutzbar",
            "10 Server-Standorte",
            "Basis-Geschwindigkeit"
        ),
        description = "Unsere kostenlosen integrierten VPN-Server. Keine Anmeldung erforderlich."
    )
    
    val MULLVAD = VpnProviderInfo(
        id = "mullvad",
        name = "Mullvad",
        displayName = "Mullvad VPN",
        websiteUrl = "https://mullvad.net",
        setupGuideUrl = "https://mullvad.net/en/help",
        supportedProtocols = listOf(VpnProtocol.WIREGUARD, VpnProtocol.OPENVPN_UDP),
        authType = VpnAuthType.ACCOUNT_NUMBER,
        hasApi = false,
        configFormat = VpnConfigFormat.WIREGUARD,
        isPopular = true,
        isPremium = true,
        features = listOf(
            "Keine Logs",
            "Unbegrenzte Geräte",
            "WireGuard Support",
            "Port Forwarding"
        ),
        description = "Schwedischer VPN-Anbieter mit Fokus auf Privatsphäre."
    )
    
    val NORDVPN = VpnProviderInfo(
        id = "nordvpn",
        name = "NordVPN",
        displayName = "NordVPN",
        websiteUrl = "https://nordvpn.com",
        setupGuideUrl = "https://support.nordvpn.com",
        supportedProtocols = listOf(VpnProtocol.OPENVPN_UDP, VpnProtocol.OPENVPN_TCP, VpnProtocol.WIREGUARD),
        authType = VpnAuthType.USERNAME_PASSWORD,
        hasApi = false,
        configFormat = VpnConfigFormat.OPENVPN,
        isPopular = true,
        isPremium = true,
        features = listOf(
            "Double VPN",
            "5500+ Server",
            "Dedizierte IPs",
            "Threat Protection"
        ),
        description = "Einer der größten VPN-Anbieter weltweit."
    )
    
    val EXPRESSVPN = VpnProviderInfo(
        id = "expressvpn",
        name = "ExpressVPN",
        displayName = "ExpressVPN",
        websiteUrl = "https://expressvpn.com",
        setupGuideUrl = "https://expressvpn.com/support",
        supportedProtocols = listOf(VpnProtocol.OPENVPN_UDP, VpnProtocol.OPENVPN_TCP, VpnProtocol.IKEV2),
        authType = VpnAuthType.USERNAME_PASSWORD,
        hasApi = false,
        configFormat = VpnConfigFormat.OPENVPN,
        isPopular = true,
        isPremium = true,
        features = listOf(
            "Lightway Protokoll",
            "Split Tunneling",
            "Network Lock",
            "94 Länder"
        ),
        description = "Premium VPN mit hoher Geschwindigkeit."
    )
    
    val PROTONVPN = VpnProviderInfo(
        id = "protonvpn",
        name = "ProtonVPN",
        displayName = "ProtonVPN",
        websiteUrl = "https://protonvpn.com",
        setupGuideUrl = "https://protonvpn.com/support",
        supportedProtocols = listOf(VpnProtocol.WIREGUARD, VpnProtocol.OPENVPN_UDP),
        authType = VpnAuthType.USERNAME_PASSWORD,
        hasApi = false,
        configFormat = VpnConfigFormat.WIREGUARD,
        isPopular = true,
        isPremium = false,
        features = listOf(
            "Kostenlose Version",
            "Secure Core",
            "P2P Support",
            "Schweizer Datenschutz"
        ),
        description = "VPN von den Machern von ProtonMail."
    )
    
    val SURFSHARK = VpnProviderInfo(
        id = "surfshark",
        name = "Surfshark",
        displayName = "Surfshark",
        websiteUrl = "https://surfshark.com",
        setupGuideUrl = "https://surfshark.com/support",
        supportedProtocols = listOf(VpnProtocol.WIREGUARD, VpnProtocol.OPENVPN_UDP, VpnProtocol.IKEV2),
        authType = VpnAuthType.USERNAME_PASSWORD,
        hasApi = false,
        configFormat = VpnConfigFormat.WIREGUARD,
        isPopular = true,
        isPremium = true,
        features = listOf(
            "Unbegrenzte Geräte",
            "CleanWeb",
            "MultiHop",
            "Günstiger Preis"
        ),
        description = "Preiswerter VPN mit unbegrenzten Geräten."
    )
    
    val MANUAL = VpnProviderInfo(
        id = "manual",
        name = "Manuell",
        displayName = "Eigene Konfiguration",
        websiteUrl = "",
        setupGuideUrl = "",
        supportedProtocols = listOf(VpnProtocol.WIREGUARD, VpnProtocol.OPENVPN_UDP, VpnProtocol.OPENVPN_TCP),
        authType = VpnAuthType.MANUAL_CONFIG,
        hasApi = false,
        configFormat = VpnConfigFormat.BOTH,
        isPopular = false,
        isPremium = false,
        features = listOf(
            "WireGuard .conf",
            "OpenVPN .ovpn",
            "Beliebiger Anbieter",
            "Volle Kontrolle"
        ),
        description = "Importiere Konfigurationsdateien von beliebigen VPN-Anbietern."
    )
    
    val ALL_PROVIDERS = listOf(
        BUILTIN_FREE,
        MULLVAD,
        NORDVPN,
        EXPRESSVPN,
        PROTONVPN,
        SURFSHARK,
        MANUAL
    )
    
    val POPULAR_PROVIDERS = ALL_PROVIDERS.filter { it.isPopular }
}