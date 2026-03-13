package com.djoudini.iplayer.data.service

import com.wireguard.android.backend.Tunnel

/**
 * Implementation of the WireGuard Tunnel interface.
 * Tracks tunnel state and forwards state changes to the provided callback.
 */
class WireGuardTunnel(
    private val name: String,
    private val onStateChanged: (Tunnel.State) -> Unit = {},
) : Tunnel {

    @Volatile
    var state: Tunnel.State = Tunnel.State.DOWN
        private set

    override fun getName(): String = name

    override fun onStateChange(newState: Tunnel.State) {
        state = newState
        onStateChanged(newState)
    }
}
