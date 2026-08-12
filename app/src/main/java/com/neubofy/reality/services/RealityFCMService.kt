package com.neubofy.reality.services

import android.content.SharedPreferences
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.neubofy.reality.utils.TerminalLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * RealityFCMService
 *
 * Handles Firebase Cloud Messaging push notifications.
 *
 * Silent push from Notification Worker:
 *   data: { "action": "SYNC_CALENDAR" }
 *   → Triggers CalendarSyncWorker to fetch latest events from Google Calendar API
 *
 * Token lifecycle:
 *   - onNewToken() is called on first launch and whenever Firebase refreshes the token
 *   - Token is persisted in SharedPreferences
 *   - Registration/unregistration with worker is managed via Profile page cards
 *   - App update token changes are handled by AppUpdateReceiver
 */
class RealityFCMService : FirebaseMessagingService() {

    companion object {
        const val PREF_FCM_TOKEN = "reality_fcm_token"
        private const val PREF_REGISTERED_FCM_TOKEN = "registered_fcm_token"
        private const val PREF_CALENDAR_CHANNEL_ID = "calendar_channel_id"
        private const val PREF_CALENDAR_RESOURCE_ID = "calendar_resource_id"
        private const val PREF_CALENDAR_CHANNEL_REGISTERED = "calendar_channel_registered"
        private const val PREF_FCM_TOKEN_REGISTERED = "fcm_token_registered"
        private val httpClient = OkHttpClient()

        /**
         * Register the device's FCM token with the Reality Notification Worker.
         * Uses static backupPassword auth (never rotates).
         */
        fun registerTokenWithWorker(
            context: android.content.Context,
            notificationWorkerUrl: String,
            userId: String,
            backupPassword: String,
            fcmToken: String,
            callback: ((Boolean, String?) -> Unit)? = null
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val json = JSONObject().apply {
                        put("userId", userId)
                        put("backupPassword", backupPassword)
                        put("fcmToken", fcmToken)
                    }

                    val body = json.toString()
                        .toRequestBody("application/json".toMediaTypeOrNull())

                    val request = Request.Builder()
                        .url("$notificationWorkerUrl/api/register-fcm-token")
                        .post(body)
                        .build()

                    val response = httpClient.newCall(request).execute()
                    val responseBody = response.body?.string()

                    if (response.isSuccessful) {
                        TerminalLogger.log("FCM: Token registered with notification worker successfully")
                        // Persist registration status and the exact token that was registered
                        context.getSharedPreferences("reality_prefs", android.content.Context.MODE_PRIVATE)
                            .edit()
                            .putBoolean(PREF_FCM_TOKEN_REGISTERED, true)
                            .putString(PREF_REGISTERED_FCM_TOKEN, fcmToken)
                            .apply()
                        withContext(Dispatchers.Main) { callback?.invoke(true, null) }
                    } else {
                        TerminalLogger.log("FCM: Token registration failed (${response.code}): $responseBody")
                        withContext(Dispatchers.Main) { callback?.invoke(false, "Registration failed (${response.code})") }
                    }
                } catch (e: Exception) {
                    TerminalLogger.log("FCM: Error registering token: ${e.message}")
                    withContext(Dispatchers.Main) { callback?.invoke(false, e.message) }
                }
            }
        }

        /**
         * Unregister the device's FCM token from the Reality Notification Worker.
         * Called on sign out and account delete.
         */
        fun unregisterTokenFromWorker(
            context: android.content.Context,
            notificationWorkerUrl: String,
            userId: String,
            backupPassword: String,
            callback: ((Boolean, String?) -> Unit)? = null
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val json = JSONObject().apply {
                        put("userId", userId)
                        put("backupPassword", backupPassword)
                    }

                    val body = json.toString()
                        .toRequestBody("application/json".toMediaTypeOrNull())

                    val request = Request.Builder()
                        .url("$notificationWorkerUrl/api/unregister-fcm-token")
                        .post(body)
                        .build()

                    val response = httpClient.newCall(request).execute()
                    val responseBody = response.body?.string()

                    if (response.isSuccessful) {
                        TerminalLogger.log("FCM: Token unregistered from notification worker successfully")
                        context.getSharedPreferences("reality_prefs", android.content.Context.MODE_PRIVATE)
                            .edit()
                            .putBoolean(PREF_FCM_TOKEN_REGISTERED, false)
                            .remove(PREF_REGISTERED_FCM_TOKEN)
                            .apply()
                        withContext(Dispatchers.Main) { callback?.invoke(true, null) }
                    } else {
                        TerminalLogger.log("FCM: Token unregistration failed (${response.code}): $responseBody")
                        withContext(Dispatchers.Main) { callback?.invoke(false, "Unregistration failed (${response.code})") }
                    }
                } catch (e: Exception) {
                    TerminalLogger.log("FCM: Error unregistering token: ${e.message}")
                    withContext(Dispatchers.Main) { callback?.invoke(false, e.message) }
                }
            }
        }

        /**
         * Update the FCM token on the worker (after app update / token rotation).
         * Only called when a previously registered token has changed.
         */
        fun updateTokenOnWorker(
            context: android.content.Context,
            notificationWorkerUrl: String,
            userId: String,
            backupPassword: String,
            oldFcmToken: String,
            newFcmToken: String
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val json = JSONObject().apply {
                        put("userId", userId)
                        put("backupPassword", backupPassword)
                        put("oldFcmToken", oldFcmToken)
                        put("newFcmToken", newFcmToken)
                    }

                    val body = json.toString()
                        .toRequestBody("application/json".toMediaTypeOrNull())

                    val request = Request.Builder()
                        .url("$notificationWorkerUrl/api/update-fcm-token")
                        .post(body)
                        .build()

                    val response = httpClient.newCall(request).execute()
                    val responseBody = response.body?.string()

                    if (response.isSuccessful) {
                        TerminalLogger.log("FCM: Token updated on notification worker successfully")
                        context.getSharedPreferences("reality_prefs", android.content.Context.MODE_PRIVATE)
                            .edit()
                            .putString(PREF_REGISTERED_FCM_TOKEN, newFcmToken)
                            .apply()
                    } else {
                        TerminalLogger.log("FCM: Token update failed (${response.code}): $responseBody")
                    }
                } catch (e: Exception) {
                    TerminalLogger.log("FCM: Error updating token: ${e.message}")
                }
            }
        }

        /**
         * Register the Google Calendar webhook with Google's API.
         * Uses backupPassword in channel token for secure webhook verification.
         * Stores channel metadata for later unregistration.
         */
        fun registerCalendarWebhook(
            context: android.content.Context,
            notificationWorkerUrl: String,
            userId: String,
            backupPassword: String,
            googleAccessToken: String,
            callback: ((Boolean, String?) -> Unit)? = null
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val channelId = "reality-channel-$userId-$backupPassword"
                    // Secure channel token: includes backupPassword for worker-side verification
                    val channelToken = "reality-$userId-$backupPassword"

                    val json = JSONObject().apply {
                        put("id", channelId)
                        put("type", "web_hook")
                        put("address", "$notificationWorkerUrl/webhook/calendar")
                        put("token", channelToken)
                    }

                    val body = json.toString()
                        .toRequestBody("application/json".toMediaTypeOrNull())

                    val request = Request.Builder()
                        .url("https://www.googleapis.com/calendar/v3/calendars/primary/events/watch")
                        .addHeader("Authorization", "Bearer $googleAccessToken")
                        .addHeader("Content-Type", "application/json")
                        .post(body)
                        .build()

                    val response = httpClient.newCall(request).execute()
                    val responseBody = response.body?.string()

                    if (response.isSuccessful) {
                        TerminalLogger.log("FCM: Google Calendar webhook registered. Channel: $channelId")
                        
                        // Parse response to get resourceId for later unregistration
                        var resourceId = ""
                        try {
                            val responseJson = JSONObject(responseBody ?: "{}")
                            resourceId = responseJson.optString("resourceId", "")
                        } catch (_: Exception) {}
                        
                        // Store channel metadata for unregistration
                        context.getSharedPreferences("reality_prefs", android.content.Context.MODE_PRIVATE)
                            .edit()
                            .putBoolean(PREF_CALENDAR_CHANNEL_REGISTERED, true)
                            .putString(PREF_CALENDAR_CHANNEL_ID, channelId)
                            .putString(PREF_CALENDAR_RESOURCE_ID, resourceId)
                            .apply()
                        withContext(Dispatchers.Main) { callback?.invoke(true, null) }
                    } else {
                        TerminalLogger.log("FCM: Webhook registration failed (${response.code}): $responseBody")
                        withContext(Dispatchers.Main) { callback?.invoke(false, "Webhook registration failed (${response.code})") }
                    }
                } catch (e: Exception) {
                    TerminalLogger.log("FCM: Webhook registration error: ${e.message}")
                    withContext(Dispatchers.Main) { callback?.invoke(false, e.message) }
                }
            }
        }

        /**
         * Unregister the Google Calendar webhook by calling channels.stop API.
         * Uses stored channel metadata from registration.
         */
        fun unregisterCalendarWebhook(
            context: android.content.Context,
            googleAccessToken: String,
            callback: ((Boolean, String?) -> Unit)? = null
        ) {
            val prefs = context.getSharedPreferences("reality_prefs", android.content.Context.MODE_PRIVATE)
            val channelId = prefs.getString(PREF_CALENDAR_CHANNEL_ID, null)
            val resourceId = prefs.getString(PREF_CALENDAR_RESOURCE_ID, null)

            if (channelId.isNullOrEmpty() || resourceId.isNullOrEmpty()) {
                TerminalLogger.log("FCM: No calendar channel to unregister (no stored metadata)")
                // Clear the flag anyway
                prefs.edit()
                    .putBoolean(PREF_CALENDAR_CHANNEL_REGISTERED, false)
                    .remove(PREF_CALENDAR_CHANNEL_ID)
                    .remove(PREF_CALENDAR_RESOURCE_ID)
                    .apply()
                callback?.invoke(true, null)
                return
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val json = JSONObject().apply {
                        put("id", channelId)
                        put("resourceId", resourceId)
                    }

                    val body = json.toString()
                        .toRequestBody("application/json".toMediaTypeOrNull())

                    val request = Request.Builder()
                        .url("https://www.googleapis.com/calendar/v3/channels/stop")
                        .addHeader("Authorization", "Bearer $googleAccessToken")
                        .addHeader("Content-Type", "application/json")
                        .post(body)
                        .build()

                    val response = httpClient.newCall(request).execute()

                    if (response.isSuccessful || response.code == 404) {
                        // 404 means channel already expired — that's fine
                        TerminalLogger.log("FCM: Google Calendar webhook unregistered. Channel: $channelId")
                    } else {
                        val responseBody = response.body?.string()
                        TerminalLogger.log("FCM: Webhook unregistration failed (${response.code}): $responseBody")
                    }

                    // Always clear local state regardless of API response
                    prefs.edit()
                        .putBoolean(PREF_CALENDAR_CHANNEL_REGISTERED, false)
                        .remove(PREF_CALENDAR_CHANNEL_ID)
                        .remove(PREF_CALENDAR_RESOURCE_ID)
                        .apply()
                    withContext(Dispatchers.Main) { callback?.invoke(true, null) }
                } catch (e: Exception) {
                    TerminalLogger.log("FCM: Webhook unregistration error: ${e.message}")
                    // Still clear local state on error
                    prefs.edit()
                        .putBoolean(PREF_CALENDAR_CHANNEL_REGISTERED, false)
                        .remove(PREF_CALENDAR_CHANNEL_ID)
                        .remove(PREF_CALENDAR_RESOURCE_ID)
                        .apply()
                    withContext(Dispatchers.Main) { callback?.invoke(false, e.message) }
                }
            }
        }

        /**
         * Check if FCM token is registered with the worker.
         */
        fun isFcmTokenRegistered(context: android.content.Context): Boolean {
            return context.getSharedPreferences("reality_prefs", android.content.Context.MODE_PRIVATE)
                .getBoolean(PREF_FCM_TOKEN_REGISTERED, false)
        }

        /**
         * Check if Calendar channel is registered.
         */
        fun isCalendarChannelRegistered(context: android.content.Context): Boolean {
            return context.getSharedPreferences("reality_prefs", android.content.Context.MODE_PRIVATE)
                .getBoolean(PREF_CALENDAR_CHANNEL_REGISTERED, false)
        }

        /**
         * Get the last registered FCM token (for app update comparison).
         */
        fun getRegisteredFcmToken(context: android.content.Context): String? {
            return context.getSharedPreferences("reality_prefs", android.content.Context.MODE_PRIVATE)
                .getString(PREF_REGISTERED_FCM_TOKEN, null)
        }
    }

    /**
     * Called when Firebase generates a new device token.
     * Only stores it locally. Registration/updates are handled by
     * Profile page cards and AppUpdateReceiver respectively.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        TerminalLogger.log("FCM: New token generated")

        // Save token locally
        val prefs = getSharedPreferences("reality_prefs", MODE_PRIVATE)
        prefs.edit().putString(PREF_FCM_TOKEN, token).apply()
    }

    /**
     * Called when a silent FCM message is received from the notification worker.
     * Triggers a one-time CalendarSyncWorker to fetch updated events.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val action = remoteMessage.data["action"]
        TerminalLogger.log("FCM: Message received. Action: $action")

        // 1. Silent google calendar sync trigger
        if (action == "SYNC_CALENDAR") {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val syncRequest = androidx.work.OneTimeWorkRequestBuilder<com.neubofy.reality.workers.CalendarSyncWorker>()
                        .addTag("fcm_triggered_calendar_sync")
                        .build()
                    androidx.work.WorkManager.getInstance(applicationContext).enqueue(syncRequest)
                    TerminalLogger.log("FCM: CalendarSyncWorker enqueued from FCM push")
                } catch (e: Exception) {
                    TerminalLogger.log("FCM: Failed to enqueue CalendarSyncWorker: ${e.message}")
                }
            }
        }

        // 2. Custom administrative/informational notifications (data payload)
        val title = remoteMessage.data["title"]
        val message = remoteMessage.data["message"]
        if (!title.isNullOrEmpty() && !message.isNullOrEmpty()) {
            com.neubofy.reality.utils.NotificationHelper.showNotification(applicationContext, title, message)
        }

        // 3. Campaign notifications (FCM Console notification payload)
        remoteMessage.notification?.let {
            val notifTitle = it.title ?: "Reality"
            val notifBody = it.body ?: ""
            com.neubofy.reality.utils.NotificationHelper.showNotification(applicationContext, notifTitle, notifBody)
        }
    }
}
