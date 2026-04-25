package com.hunterxdk.gymsololeveling.core.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hunterxdk.gymsololeveling.core.worker.StreakCheckWorker

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            StreakCheckWorker.schedule(context)
        }
    }
}
