package com.djoudini.iplayer.data.service

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages VPN permission requests across the app.
 *
 * Android requires VpnService.prepare() to be called from an Activity context
 * (it shows a system dialog). This manager signals MainActivity when permission
 * is needed so it can launch the dialog and relay the result.
 */
@Singleton
class VpnPermissionManager @Inject constructor() {

    private val _permissionRequest = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Emits whenever the VPN system permission dialog should be shown. */
    val permissionRequest: SharedFlow<Unit> = _permissionRequest.asSharedFlow()

    private var pendingCallback: (() -> Unit)? = null

    /**
     * Request VPN permission. If already granted, [onGranted] is called immediately
     * by MainActivity; otherwise the system dialog is shown first.
     */
    fun requestPermission(onGranted: () -> Unit) {
        pendingCallback = onGranted
        _permissionRequest.tryEmit(Unit)
    }

    /** Called by MainActivity when the user accepted the VPN permission dialog. */
    fun onPermissionGranted() {
        pendingCallback?.invoke()
        pendingCallback = null
    }

    /** Called by MainActivity when the user denied the VPN permission dialog. */
    fun onPermissionDenied() {
        pendingCallback = null
    }
}
