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
    private fun importBasedProvider(
        id: String,
        displayName: String,
        description: String,
        websiteUrl: String,
        setupGuideUrl: String,
        isPremium: Boolean = true,
    ) = VpnProviderInfo(
        id = id,
        name = displayName,
        displayName = displayName,
        websiteUrl = websiteUrl,
        setupGuideUrl = setupGuideUrl,
        supportedProtocols = listOf(VpnProtocol.WIREGUARD),
        authType = VpnAuthType.MANUAL_CONFIG,
        hasApi = false,
        configFormat = VpnConfigFormat.WIREGUARD,
        isPopular = true,
        isPremium = isPremium,
        features = listOf(
            "Mit Handy vorbereiten",
            "Import deiner WireGuard-Datei",
            "Kein Fake-Login",
            "Nutzt echte Endpunkte deines Kontos",
        ),
        description = description,
    )

    val MANUAL = VpnProviderInfo(
        id = "manual",
        name = "WireGuard",
        displayName = "Eigene WireGuard-Konfiguration",
        websiteUrl = "https://www.wireguard.com/",
        setupGuideUrl = "https://www.wireguard.com/quickstart/",
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

    val PROTON = importBasedProvider(
        id = "proton",
        displayName = "Proton VPN",
        description = "Fuehrt dich zum Import deiner eigenen WireGuard-Datei von Proton VPN.",
        websiteUrl = "https://protonvpn.com/",
        setupGuideUrl = "https://protonvpn.com/support/wireguard-configurations/",
        isPremium = false,
    )

    val MULLVAD = importBasedProvider(
        id = "mullvad",
        displayName = "Mullvad",
        description = "Fuehrt dich zum Import deiner eigenen WireGuard-Datei von Mullvad.",
        websiteUrl = "https://mullvad.net/",
        setupGuideUrl = "https://mullvad.net/en/help/wireguard-android",
    )

    val NORD = importBasedProvider(
        id = "nordvpn",
        displayName = "NordVPN",
        description = "Fuehrt dich zum Import deiner eigenen WireGuard-Datei von NordVPN.",
        websiteUrl = "https://nordvpn.com/",
        setupGuideUrl = "https://support.nordvpn.com/hc/en-us/articles/19928244437777-Installing-and-using-NordVPN-on-Android-TV-or-Nvidia-Shield",
    )

    val SURFSHARK = importBasedProvider(
        id = "surfshark",
        displayName = "Surfshark",
        description = "Fuehrt dich zum Import deiner eigenen WireGuard-Datei von Surfshark.",
        websiteUrl = "https://surfshark.com/",
        setupGuideUrl = "https://support.surfshark.com/hc/en-us/articles/360024352553-How-to-set-up-Surfshark-on-Android-TV",
    )

    val PIA = importBasedProvider(
        id = "pia",
        displayName = "Private Internet Access",
        description = "Fuehrt dich zum Import deiner eigenen WireGuard-Datei von PIA.",
        websiteUrl = "https://www.privateinternetaccess.com/",
        setupGuideUrl = "https://helpdesk.privateinternetaccess.com/",
    )

    val IVPN = importBasedProvider(
        id = "ivpn",
        displayName = "IVPN",
        description = "Fuehrt dich zum Import deiner eigenen WireGuard-Datei von IVPN.",
        websiteUrl = "https://www.ivpn.net/",
        setupGuideUrl = "https://www.ivpn.net/setup/linux-wireguard/",
    )

    val WINDSCRIBE = importBasedProvider(
        id = "windscribe",
        displayName = "Windscribe",
        description = "Fuehrt dich zum Import deiner eigenen WireGuard-Datei von Windscribe.",
        websiteUrl = "https://windscribe.com/",
        setupGuideUrl = "https://windscribe.com/features/config-generators/",
        isPremium = false,
    )

    val HIDE_ME = importBasedProvider(
        id = "hide-me",
        displayName = "hide.me",
        description = "Fuehrt dich zum Import deiner eigenen WireGuard-Datei von hide.me.",
        websiteUrl = "https://hide.me/",
        setupGuideUrl = "https://hide.me/en/knowledgebase/how-to-connect-via-wireguard-configuration-file/",
        isPremium = false,
    )

    val OVPN = importBasedProvider(
        id = "ovpn",
        displayName = "OVPN",
        description = "Fuehrt dich zum Import deiner eigenen WireGuard-Datei von OVPN.",
        websiteUrl = "https://www.ovpn.com/en",
        setupGuideUrl = "https://support.ovpn.com/hc/en-us/articles/17180274155924-How-do-I-enter-a-public-key-in-the-configuration-generator",
    )

    val PRIVADO = importBasedProvider(
        id = "privado",
        displayName = "PrivadoVPN",
        description = "Fuehrt dich zum Import deiner eigenen WireGuard-Datei von PrivadoVPN.",
        websiteUrl = "https://privadovpn.com/",
        setupGuideUrl = "https://support.privadovpn.com/kb/article/1133-wireguard%25C2%25AE-app-for-windows/",
        isPremium = false,
    )

    val EXPRESS = importBasedProvider(
        id = "expressvpn",
        displayName = "ExpressVPN",
        description = "Gefuehrter Handy-/Import-Flow fuer ExpressVPN.",
        websiteUrl = "https://www.expressvpn.com/",
        setupGuideUrl = "https://www.expressvpn.com/support/",
    )

    val CYBERGHOST = importBasedProvider(
        id = "cyberghost",
        displayName = "CyberGhost",
        description = "Gefuehrter Handy-/Import-Flow fuer CyberGhost.",
        websiteUrl = "https://www.cyberghostvpn.com/",
        setupGuideUrl = "https://support.cyberghostvpn.com/",
    )

    val IPVANISH = importBasedProvider(
        id = "ipvanish",
        displayName = "IPVanish",
        description = "Gefuehrter Handy-/Import-Flow fuer IPVanish.",
        websiteUrl = "https://www.ipvanish.com/",
        setupGuideUrl = "https://support.ipvanish.com/",
    )

    val PUREVPN = importBasedProvider(
        id = "purevpn",
        displayName = "PureVPN",
        description = "Gefuehrter Handy-/Import-Flow fuer PureVPN.",
        websiteUrl = "https://www.purevpn.com/",
        setupGuideUrl = "https://www.purevpn.com/support",
    )

    val TUNNELBEAR = importBasedProvider(
        id = "tunnelbear",
        displayName = "TunnelBear",
        description = "Gefuehrter Handy-/Import-Flow fuer TunnelBear.",
        websiteUrl = "https://www.tunnelbear.com/",
        setupGuideUrl = "https://help.tunnelbear.com/",
        isPremium = false,
    )

    val HOTSPOT = importBasedProvider(
        id = "hotspotshield",
        displayName = "Hotspot Shield",
        description = "Gefuehrter Handy-/Import-Flow fuer Hotspot Shield.",
        websiteUrl = "https://www.hotspotshield.com/",
        setupGuideUrl = "https://support.hotspotshield.com/",
        isPremium = false,
    )

    val VYPR = importBasedProvider(
        id = "vyprvpn",
        displayName = "VyprVPN",
        description = "Gefuehrter Handy-/Import-Flow fuer VyprVPN.",
        websiteUrl = "https://www.vyprvpn.com/",
        setupGuideUrl = "https://support.vyprvpn.com/",
    )

    val FASTEST = importBasedProvider(
        id = "fastestvpn",
        displayName = "FastestVPN",
        description = "Gefuehrter Handy-/Import-Flow fuer FastestVPN.",
        websiteUrl = "https://fastestvpn.com/",
        setupGuideUrl = "https://support.fastestvpn.com/",
    )

    val STRONG = importBasedProvider(
        id = "strongvpn",
        displayName = "StrongVPN",
        description = "Gefuehrter Handy-/Import-Flow fuer StrongVPN.",
        websiteUrl = "https://strongvpn.com/",
        setupGuideUrl = "https://support.strongvpn.com/",
    )

    val TORGUARD = importBasedProvider(
        id = "torguard",
        displayName = "TorGuard",
        description = "Gefuehrter Handy-/Import-Flow fuer TorGuard.",
        websiteUrl = "https://torguard.net/",
        setupGuideUrl = "https://torguard.net/knowledgebase.php",
    )

    val AIRVPN = importBasedProvider(
        id = "airvpn",
        displayName = "AirVPN",
        description = "Gefuehrter Handy-/Import-Flow fuer AirVPN.",
        websiteUrl = "https://airvpn.org/",
        setupGuideUrl = "https://airvpn.org/guides/",
    )

    val CACTUS = importBasedProvider(
        id = "cactusvpn",
        displayName = "CactusVPN",
        description = "Gefuehrter Handy-/Import-Flow fuer CactusVPN.",
        websiteUrl = "https://www.cactusvpn.com/",
        setupGuideUrl = "https://www.cactusvpn.com/tutorials/",
    )

    val ZOOG = importBasedProvider(
        id = "zoogvpn",
        displayName = "ZoogVPN",
        description = "Gefuehrter Handy-/Import-Flow fuer ZoogVPN.",
        websiteUrl = "https://zoogvpn.com/",
        setupGuideUrl = "https://zoogvpn.com/support/",
        isPremium = false,
    )

    val VPN_UNLIMITED = importBasedProvider(
        id = "vpn-unlimited",
        displayName = "VPN Unlimited",
        description = "Gefuehrter Handy-/Import-Flow fuer VPN Unlimited.",
        websiteUrl = "https://www.vpnunlimited.com/",
        setupGuideUrl = "https://www.vpnunlimited.com/help",
    )

    val MOZILLA = importBasedProvider(
        id = "mozilla-vpn",
        displayName = "Mozilla VPN",
        description = "Gefuehrter Handy-/Import-Flow fuer Mozilla VPN.",
        websiteUrl = "https://www.mozilla.org/products/vpn/",
        setupGuideUrl = "https://support.mozilla.org/products/mozilla-vpn",
    )

    val NORTON = importBasedProvider(
        id = "norton-vpn",
        displayName = "Norton VPN",
        description = "Gefuehrter Handy-/Import-Flow fuer Norton VPN.",
        websiteUrl = "https://us.norton.com/products/norton-secure-vpn",
        setupGuideUrl = "https://support.norton.com/",
    )

    val BITDEFENDER = importBasedProvider(
        id = "bitdefender-vpn",
        displayName = "Bitdefender VPN",
        description = "Gefuehrter Handy-/Import-Flow fuer Bitdefender VPN.",
        websiteUrl = "https://www.bitdefender.com/solutions/vpn.html",
        setupGuideUrl = "https://www.bitdefender.com/consumer/support/",
    )

    val NAMECHEAP = importBasedProvider(
        id = "namecheap-vpn",
        displayName = "Namecheap FastVPN",
        description = "Gefuehrter Handy-/Import-Flow fuer FastVPN von Namecheap.",
        websiteUrl = "https://www.namecheap.com/vpn/",
        setupGuideUrl = "https://www.namecheap.com/support/knowledgebase/subcategory/223/fastvpn/",
    )

    val ADGUARD = importBasedProvider(
        id = "adguard-vpn",
        displayName = "AdGuard VPN",
        description = "Gefuehrter Handy-/Import-Flow fuer AdGuard VPN.",
        websiteUrl = "https://adguard-vpn.com/",
        setupGuideUrl = "https://adguard-vpn.com/kb/",
        isPremium = false,
    )

    val GENERIC = importBasedProvider(
        id = "generic-wireguard",
        displayName = "Beliebiger WireGuard-Anbieter",
        description = "Nutze jede echte WireGuard-Konfiguration, auch von deinem Router, NAS oder Server.",
        websiteUrl = "https://www.wireguard.com/",
        setupGuideUrl = "https://www.wireguard.com/quickstart/",
        isPremium = false,
    )

    val ALL_PROVIDERS = listOf(
        MANUAL,
        GENERIC,
        PROTON,
        MULLVAD,
        NORD,
        SURFSHARK,
        PIA,
        IVPN,
        WINDSCRIBE,
        HIDE_ME,
        OVPN,
        PRIVADO,
        EXPRESS,
        CYBERGHOST,
        IPVANISH,
        PUREVPN,
        TUNNELBEAR,
        HOTSPOT,
        VYPR,
        FASTEST,
        STRONG,
        TORGUARD,
        AIRVPN,
        CACTUS,
        ZOOG,
        VPN_UNLIMITED,
        MOZILLA,
        NORTON,
        BITDEFENDER,
        NAMECHEAP,
        ADGUARD,
    )
    val POPULAR_PROVIDERS = ALL_PROVIDERS
}
