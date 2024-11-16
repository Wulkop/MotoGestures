package com.wully.motogestures

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && !BackgroundService.isRunning) {
            Log.d("BootReceiver", "Device has been booted. Staring service")
            val serviceIntent = Intent(context, BackgroundService::class.java)
            context.startService(serviceIntent)
        }
    }
}