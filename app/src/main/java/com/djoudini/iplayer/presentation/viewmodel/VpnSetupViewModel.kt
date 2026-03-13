package com.djoudini.iplayer.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.djoudini.iplayer.data.local.entity.VpnConnectionTestResult
import com.djoudini.iplayer.data.local.entity.VpnProviderInfo
import com.djoudini.iplayer.data.local.entity.VpnSetupState
import com.djoudini.iplayer.data.local.entity.VpnSetupStep
import com.djoudini.iplayer.data.repository.VpnSetupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for VPN Setup Wizard.
 */
@HiltViewModel
class VpnSetupViewModel @Inject constructor(
    private val vpnSetupRepository: VpnSetupRepository,
) : ViewModel() {

    val setupState: StateFlow<VpnSetupState> = vpnSetupRepository.setupState

    /**
     * Get all available VPN providers.
     */
    fun getAvailableProviders(): List<VpnProviderInfo> {
        return vpnSetupRepository.getAvailableProviders()
    }

    /**
     * Get popular VPN providers.
     */
    fun getPopularProviders(): List<VpnProviderInfo> {
        return vpnSetupRepository.getPopularProviders()
    }

    /**
     * Select a VPN provider.
     */
    fun selectProvider(provider: VpnProviderInfo) {
        vpnSetupRepository.selectProvider(provider)
    }

    /**
     * Set login credentials.
     */
    fun setCredentials(
        username: String? = null,
        password: String? = null,
        accountNumber: String? = null,
        apiKey: String? = null,
    ) {
        vpnSetupRepository.setCredentials(username, password, accountNumber, apiKey)
    }

    /**
     * Authenticate with provider.
     */
    fun authenticateWithProvider(
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        viewModelScope.launch {
            vpnSetupRepository.authenticateWithProvider()
                .onSuccess { onSuccess() }
                .onFailure { onError(it.message ?: "Authentication failed") }
        }
    }

    /**
     * Import VPN config file from a content URI (result of file picker).
     */
    fun importConfigFile(
        uri: Uri,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        viewModelScope.launch {
            vpnSetupRepository.importVpnConfig(uri)
                .onSuccess { onSuccess() }
                .onFailure { onError(it.message ?: "Import fehlgeschlagen") }
        }
    }

    /**
     * Select a VPN server.
     */
    fun selectServer(serverId: String) {
        vpnSetupRepository.selectServer(serverId)
    }

    /**
     * Test VPN connection.
     */
    fun testConnection(
        onSuccess: (VpnConnectionTestResult) -> Unit,
        onError: (String) -> Unit,
    ) {
        viewModelScope.launch {
            vpnSetupRepository.testConnection()
                .onSuccess { onSuccess(it) }
                .onFailure { onError(it.message ?: "Test failed") }
        }
    }

    /**
     * Complete VPN setup.
     */
    fun completeSetup(
        enableAutoConnect: Boolean,
        onComplete: () -> Unit,
        onError: (String) -> Unit,
    ) {
        viewModelScope.launch {
            vpnSetupRepository.completeSetup(enableAutoConnect)
                .onSuccess { onComplete() }
                .onFailure { onError(it.message ?: "Setup failed") }
        }
    }

    /**
     * Go to previous step.
     */
    fun goToPreviousStep() {
        vpnSetupRepository.goToPreviousStep()
    }

    /**
     * Reset setup.
     */
    fun resetSetup() {
        vpnSetupRepository.resetSetup()
    }

    /**
     * Clear error.
     */
    fun clearError() {
        vpnSetupRepository.clearError()
    }

    /**
     * Get current step number.
     */
    fun getCurrentStepNumber(): Int {
        return setupState.value.currentStep.stepNumber
    }

    /**
     * Get total steps.
     */
    fun getTotalSteps(): Int {
        return VpnSetupStep.ProviderSelection.totalSteps
    }
}
