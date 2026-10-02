package com.seedcipher.app.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action
        if (action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION || action == Telephony.Sms.Intents.SMS_DELIVER_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
            for (message in messages) {
                val sender = message.displayOriginatingAddress ?: continue
                val body = message.displayMessageBody ?: ""
                
                val updateIntent = Intent(ACTION_SMS_RECEIVED).apply {
                    putExtra(EXTRA_SENDER, sender)
                    putExtra(EXTRA_BODY, body)
                    setPackage(context?.packageName)
                }
                context?.sendBroadcast(updateIntent)
            }
        }
    }

    companion object {
        const val ACTION_SMS_RECEIVED = "com.seedcipher.app.ACTION_SMS_RECEIVED"
        const val EXTRA_SENDER = "extra_sender"
        const val EXTRA_BODY = "extra_body"
    }
}
