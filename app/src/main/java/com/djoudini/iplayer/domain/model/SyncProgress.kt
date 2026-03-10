package com.djoudini.iplayer.domain.model

/**
 * Represents the progress state of a sync operation (playlist, EPG, etc.).
 * Emitted as StateFlow from repository to ViewModel to Compose UI.
 */
data class SyncProgress(
    /** Current phase label, e.g. "Parsing channels...", "Loading EPG..." */
    val phase: String = "",

    /** Progress value from 0.0f to 1.0f. -1f means indeterminate. */
    val progress: Float = 0f,

    /** Total items to process (if known) */
    val totalItems: Int = 0,

    /** Items processed so far */
    val processedItems: Int = 0,

    /** Whether sync is currently running */
    val isActive: Boolean = false,

    /** Error message if sync failed, null otherwise */
    val error: String? = null,
) {
    val isCompleted: Boolean get() = !isActive && progress >= 1f && error == null
    val isFailed: Boolean get() = error != null
    val isIndeterminate: Boolean get() = progress < 0f

    companion object {
        val Idle = SyncProgress()
        fun indeterminate(phase: String) = SyncProgress(phase = phase, progress = -1f, isActive = true)
        fun active(phase: String, progress: Float) = SyncProgress(phase = phase, progress = progress.coerceIn(0f, 1f), isActive = true)
        fun completed() = SyncProgress(progress = 1f, isActive = false)
        fun failed(error: String) = SyncProgress(error = error, isActive = false)
    }
}
