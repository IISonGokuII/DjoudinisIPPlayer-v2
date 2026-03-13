package com.djoudini.iplayer.data.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages VPN connection state across the app.
 */
@Singleton
class VpnStateManager @Inject constructor() {
    
    private val _state = MutableStateFlow<VpnState>(VpnState.Disconnected)
    val state: StateFlow<VpnState> = _state.asStateFlow()

    private var connectedTunnelName: String? = null

    fun setConnected(tunnelName: String) {
        connectedTunnelName = tunnelName
        _state.value = VpnState.Connected(tunnelName, System.currentTimeMillis())
    }

    fun setDisconnected() {
        val oldName = connectedTunnelName
        connectedTunnelName = null
        _state.value = VpnState.Disconnected
    }

    fun setConnecting(tunnelName: String) {
        _state.value = VpnState.Connecting(tunnelName)
    }

    fun setDisconnecting() {
        _state.value = VpnState.Disconnecting
    }

    fun setError(message: String) {
        _state.value = VpnState.Error(message)
    }

    fun isConnected(): Boolean {
        return _state.value is VpnState.Connected
    }

    fun getCurrentTunnelName(): String? {
        return connectedTunnelName
    }

    fun getConnectedDuration(): Long? {
        return when (val currentState = _state.value) {
            is VpnState.Connected -> System.currentTimeMillis() - currentState.connectedSince
            else -> null
        }
    }
}

/**
 * VPN connection state sealed class.
 */
sealed class VpnState {
    object Disconnected : VpnState()
    data class Connecting(override val tunnelName: String) : VpnState()
    data class Connected(
        override val tunnelName: String,
        val connectedSince: Long,
    ) : VpnState()
    object Disconnecting : VpnState()
    data class Error(val message: String) : VpnState()
    
    val isConnected: Boolean get() = this is Connected
    val isConnecting: Boolean get() = this is Connecting || this is Disconnecting
    val isError: Boolean get() = this is Error
    
    open val tunnelName: String?
        get() = when (this) {
            is Connected -> tunnelName
            is Connecting -> tunnelName
            else -> null
        }
}
