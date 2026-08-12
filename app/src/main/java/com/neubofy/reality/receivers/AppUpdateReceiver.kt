package com.neubofy.reality.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.firebase.messaging.FirebaseMessaging
import com.neubofy.reality.services.RealityFCMService
import com.neubofy.reality.utils.TerminalLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * AppUpdateReceiver
 *
 * Detects when the app is updated via MY_PACKAGE_REPLACED broadcast.
 * Checks if the FCM token has changed since last registration
 * and silently updates it on the notification worker.
 *
 * Logic:
 * 1. Read stored `registered_fcm_token` from SharedPrefs
 * 2. If null/empty → user never registered FCM token → skip
 * 3. Get current FCM token from Firebase
 * 4. If same as stored → no change → skip
 * 5. If different → call worker to update the token
 */
class AppUpdateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        TerminalLogger.log("APP UPDATE: App updated, checking FCM token...")

        val appContext = context.applicationContext

        // Check if user has a registered FCM token (i.e., they've opted in)
        val registeredToken = RealityFCMService.getRegisteredFcmToken(appContext)
        if (registeredToken.isNullOrEmpty()) {
            TerminalLogger.log("APP UPDATE: No registered FCM token found — user hasn't registered. Skipping.")
            return
        }

        // Check if user is signed in and has credentials
        val isSignedIn = com.neubofy.reality.google.GoogleAuthManager.isSignedIn(appContext)
        if (!isSignedIn) {
            TerminalLogger.log("APP UPDATE: User not signed in — skipping FCM token update.")
            return
        }

        val userId = com.neubofy.reality.utils.IdentityManager.getUserId(appContext)
        val backupPassword = com.neubofy.reality.utils.IdentityManager.getBackupPassword(appContext)
        val workerUrl = com.neubofy.reality.BuildConfig.NOTIFICATION_WORKER_URL

        if (userId.isEmpty() || backupPassword.isEmpty() || workerUrl.isEmpty()) {
            TerminalLogger.log("APP UPDATE: Missing credentials — skipping FCM token update.")
            return
        }

        // Get current FCM token and compare with registered one
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val currentToken = FirebaseMessaging.getInstance().token.await()

                if (currentToken == registeredToken) {
                    TerminalLogger.log("APP UPDATE: FCM token unchanged — no update needed.")
                    return@launch
                }

                TerminalLogger.log("APP UPDATE: FCM token changed! Updating on worker...")

                // Also save the new token locally
                appContext.getSharedPreferences("reality_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putString(RealityFCMService.PREF_FCM_TOKEN, currentToken)
                    .apply()

                RealityFCMService.updateTokenOnWorker(
                    appContext,
                    workerUrl,
                    userId,
                    backupPassword,
                    registeredToken,
                    currentToken
                )
            } catch (e: Exception) {
                TerminalLogger.log("APP UPDATE: Failed to check/update FCM token: ${e.message}")
            }
        }
    }
}
