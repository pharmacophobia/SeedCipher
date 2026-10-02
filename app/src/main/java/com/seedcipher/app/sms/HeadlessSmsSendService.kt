package com.seedcipher.app.sms

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Service required by Android system for Default SMS App status to handle quick responses (RESPOND_VIA_MESSAGE).
 */
class HeadlessSmsSendService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
