package com.djoudini.iplayer.data.local.entity

/**
 * VPN Provider information.
 */
data class VpnProviderInfo(
    val id: String,
    val name: String,
    val displayName: String,
    val logoResId: Int? = null,
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
    NONE,
    USERNAME_PASSWORD,
    ACCOUNT_NUMBER,
    API_KEY,
    OAUTH,
    CERTIFICATE,
    MANUAL_CONFIG,
}

/**
 * VPN Config File Format.
 */
enum class VpnConfigFormat {
    WIREGUARD,
    OPENVPN,
    BOTH,
    CUSTOM,
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
            is ConfigImport -> 2
            is ServerSelection -> 3
            is ConnectionTest -> 3
            is Complete -> 4
        }

    val totalSteps: Int = 4
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
 * The app currently supports one production-ready flow: importing a real
 * WireGuard configuration from the user's provider.
 */
object KnownVpnProviders {
    val MANUAL = VpnProviderInfo(
        id = "manual",
        name = "WireGuard",
        displayName = "Eigene WireGuard-Konfiguration",
        websiteUrl = "",
        setupGuideUrl = "",
        supportedProtocols = listOf(VpnProtocol.WIREGUARD),
        authType = VpnAuthType.MANUAL_CONFIG,
        hasApi = false,
        configFormat = VpnConfigFormat.WIREGUARD,
        isPopular = true,
        isPremium = false,
        features = listOf(
            "WireGuard .conf",
            "Echte Endpunkte aus deiner Datei",
            "Keine Demo-Server",
            "Volle Kontrolle",
        ),
        description = "Importiere deine echte WireGuard-Konfiguration und verbinde dich ohne Platzhalterserver.",
    )

    val ALL_PROVIDERS = listOf(MANUAL)
    val POPULAR_PROVIDERS = ALL_PROVIDERS
}
