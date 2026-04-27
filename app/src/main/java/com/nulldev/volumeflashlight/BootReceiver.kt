package com.nulldev.volumeflashlight

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val prefs = context.getSharedPreferences(FlashlightService.PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(FlashlightService.PREF_AUTO_START, false)) return
        context.startForegroundService(Intent(context, FlashlightService::class.java))
    }
}
