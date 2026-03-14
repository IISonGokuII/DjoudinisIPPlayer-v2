package com.djoudini.iplayer.util

import android.content.Context
import android.content.Intent
import android.os.Build

fun Context.startCompatService(intent: Intent) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        startForegroundService(intent)
    } else {
        startService(intent)
    }
}
