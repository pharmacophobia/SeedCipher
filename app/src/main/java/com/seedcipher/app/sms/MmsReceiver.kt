package com.seedcipher.app.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * BroadcastReceiver required by Android system for Default SMS App status to handle WAP PUSH / MMS delivery.
 */
class MmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        // Stub implementation required for Default SMS Handler status
    }
}
