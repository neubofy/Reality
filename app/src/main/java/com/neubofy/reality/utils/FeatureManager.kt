package com.neubofy.reality.utils

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FeatureManager(private val context: Context) {
    private val prefs: SharedPreferences = com.neubofy.reality.utils.SecurePreferences.get(context, "reality_features")

    companion object {
        const val ACTION_SUBSCRIPTION_STATUS_CHANGED = "com.neubofy.reality.SUBSCRIPTION_STATUS_CHANGED"
        private const val KEY_LAST_SUBSCRIPTION_VALIDATION = "last_subscription_validation_timestamp"
        private const val TTL_VALIDATION_INTERVAL_MS = 24 * 60 * 60 * 1000L // 24 hours
    }

    fun isAiEnabled(): Boolean {
        if (!isRealityProEnabled()) return false
        return prefs.getBoolean("feature_ai", false)
    }
    fun setAiEnabled(enabled: Boolean) = prefs.edit().putBoolean("feature_ai", enabled).apply()

    /**
     * Dynamically checks if Reality Pro is enabled.
     * Rather than relying on a static persisted boolean, this dynamically validates
     * that the user has an active verified paid subscription or an active trial.
     * If expired, it automatically and cleanly downgrades the app to the Forever Free tier.
     */
    fun isRealityProEnabled(): Boolean {
        val userWantsPro = prefs.getBoolean("feature_reality_pro", false)
        if (!userWantsPro) return false

        // Dynamic local verification against SecureTimeProvider
        val isEntitled = isRealityProVerified() || isTrialActive()
        if (!isEntitled) {
            // Subscription / trial has expired -> Auto demote to Forever Free
            downgradeToFreeTier()
            return false
        }
        return true
    }

    fun setRealityProEnabled(enabled: Boolean) = prefs.edit().putBoolean("feature_reality_pro", enabled).apply()

    /**
     * Gracefully demotes the user to the Forever Free tier:
     * - Disables Pro master toggle
     * - Disables Pro-only features (e.g., AI Chat)
     * - Emits local broadcast so open activities (Settings, Elite, Main) refresh immediately
     */
    fun downgradeToFreeTier() {
        prefs.edit()
            .putBoolean("feature_reality_pro", false)
            .putBoolean("feature_ai", false)
            .apply()

        try {
            val intent = android.content.Intent(ACTION_SUBSCRIPTION_STATUS_CHANGED).apply {
                setPackage(context.packageName)
            }
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            // Ignore in headless / background contexts
        }
    }

    /**
     * Periodically validates subscription status on app launch with a 24-hour TTL.
     * Runs strictly on app open (no WorkManager, AlarmManager, or background schedulers).
     * If 24 hours have elapsed and an internet connection is present, it revalidates
     * stored expiration timestamps against true network time.
     */
    fun validateSubscriptionOnAppOpen() {
        val userEmail = com.neubofy.reality.google.GoogleAuthManager.getUserEmail(context)
        val userId = com.neubofy.reality.utils.IdentityManager.getUserId(context)

        val hasSubscriptionRecord = (userEmail != null && userId.isNotEmpty()) &&
                (prefs.contains("feature_reality_pro_verified_until_$userId") ||
                 prefs.contains("trial_end_time_$userId") ||
                 prefs.getBoolean("feature_reality_pro", false))

        if (!hasSubscriptionRecord) {
            if (prefs.getBoolean("feature_reality_pro", false)) {
                downgradeToFreeTier()
            }
            return
        }

        val lastValidationTime = prefs.getLong(KEY_LAST_SUBSCRIPTION_VALIDATION, 0L)
        val now = System.currentTimeMillis()

        // 1. Fast local verification: if expired locally right now, demote immediately
        if (prefs.getBoolean("feature_reality_pro", false) && !isRealityProVerified() && !isTrialActive()) {
            downgradeToFreeTier()
        }

        // 2. Check 24-hour TTL
        if (lastValidationTime > 0 && (now - lastValidationTime) < TTL_VALIDATION_INTERVAL_MS) {
            return
        }

        // 3. Check for active internet connection
        if (!isNetworkConnected(context)) {
            return
        }

        // 4. Validate asynchronously using true network time
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                // Sync network time from internet to prevent local device clock tampering
                SecureTimeProvider.syncWithNetwork(context)
                val internetTime = SecureTimeProvider.currentTimeMillis(context)

                val paidEndTime = prefs.getLong("feature_reality_pro_verified_until_$userId", 0L)
                val trialEndTime = prefs.getLong("trial_end_time_$userId", 0L)

                val isPaidActive = paidEndTime > 0L && internetTime < paidEndTime
                val isTrialActive = trialEndTime > 0L && internetTime < trialEndTime

                if (!isPaidActive && !isTrialActive) {
                    downgradeToFreeTier()
                    TerminalLogger.log("SUBSCRIPTION: 24h network TTL check expired. Reverted to Forever Free tier.")
                }

                // If signed in, also sync identity in background to keep Cloudflare Worker D1 aligned
                if (com.neubofy.reality.google.GoogleAuthManager.isSignedIn(context)) {
                    try {
                        com.neubofy.reality.utils.IdentityManager.refreshIdentity(context, isManualTrigger = false)
                    } catch (e: Exception) {
                        // Silent fail if network drops
                    }
                }

                // Update 24h TTL timestamp
                prefs.edit().putLong(KEY_LAST_SUBSCRIPTION_VALIDATION, System.currentTimeMillis()).apply()
            } catch (e: Exception) {
                TerminalLogger.log("SUBSCRIPTION: Validation on app open error: ${e.message}")
            }
        }
    }

    private fun isNetworkConnected(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            val network = cm?.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            false
        }
    }

    fun getTrialStartTime(): Long {
        val userEmail = com.neubofy.reality.google.GoogleAuthManager.getUserEmail(context) ?: return 0L
        val userId = com.neubofy.reality.utils.IdentityManager.getUserId(context)
        if (userId.isEmpty()) return 0L
        return prefs.getLong("trial_start_time_$userId", 0L)
    }

    fun getTrialEndTime(): Long {
        val userEmail = com.neubofy.reality.google.GoogleAuthManager.getUserEmail(context) ?: return 0L
        val userId = com.neubofy.reality.utils.IdentityManager.getUserId(context)
        if (userId.isEmpty()) return 0L

        var trialEndTime = prefs.getLong("trial_end_time_$userId", 0L)
        return trialEndTime
    }

    fun getRealityProStartTime(): Long {
        val userEmail = com.neubofy.reality.google.GoogleAuthManager.getUserEmail(context) ?: return 0L
        val userId = com.neubofy.reality.utils.IdentityManager.getUserId(context)
        if (userId.isEmpty()) return 0L
        return prefs.getLong("feature_reality_pro_start_time_$userId", 0L)
    }

    fun setRealityProStartTime(timeMs: Long) {
        val userEmail = com.neubofy.reality.google.GoogleAuthManager.getUserEmail(context) ?: return
        val userId = com.neubofy.reality.utils.IdentityManager.getUserId(context)
        if (userId.isEmpty()) return
        prefs.edit().putLong("feature_reality_pro_start_time_$userId", timeMs).apply()
    }

    fun getRealityProEndTime(): Long {
        val userEmail = com.neubofy.reality.google.GoogleAuthManager.getUserEmail(context) ?: return 0L
        val userId = com.neubofy.reality.utils.IdentityManager.getUserId(context)
        if (userId.isEmpty()) return 0L
        return prefs.getLong("feature_reality_pro_verified_until_$userId", 0L)
    }

    fun isRealityProVerified(): Boolean {
        val userEmail = com.neubofy.reality.google.GoogleAuthManager.getUserEmail(context) ?: return false
        val userId = com.neubofy.reality.utils.IdentityManager.getUserId(context)
        if (userId.isEmpty()) return false

        // Check for 1-year timestamp
        val verifiedUntil = prefs.getLong("feature_reality_pro_verified_until_$userId", 0L)

        // Backwards compatibility for old boolean verified users (grace period)
        val legacyVerified = prefs.getBoolean("feature_reality_pro_verified_$userId", false)
        if (legacyVerified) {
            // Migrate them to 1 year from now
            setRealityProVerified(true)
            prefs.edit().remove("feature_reality_pro_verified_$userId").apply()
            return true
        }

        val secureNow = SecureTimeProvider.currentTimeMillis(context)
        return verifiedUntil > 0 && secureNow < verifiedUntil
    }

    fun setRealityProVerified(verified: Boolean, currentTimeMs: Long = SecureTimeProvider.currentTimeMillis(context), months: Int = 12) {
        val userEmail = com.neubofy.reality.google.GoogleAuthManager.getUserEmail(context) ?: return
        val userId = com.neubofy.reality.utils.IdentityManager.getUserId(context)
        if (userId.isEmpty()) return
        if (verified) {
            val durationMs = (365L / 12) * months * 24 * 60 * 60 * 1000
            val verifiedUntil = currentTimeMs + durationMs
            prefs.edit().putLong("feature_reality_pro_verified_until_$userId", verifiedUntil).apply()
        } else {
            // Batch both removals into a single atomic edit to avoid race conditions
            prefs.edit()
                .remove("feature_reality_pro_verified_until_$userId")
                .remove("feature_reality_pro_verified_$userId")
                .apply()
        }
    }

    // getDeviceUniqueId() removed — was dead code from an older device-binding approach


    fun isTrialActive(): Boolean {
        val userEmail = com.neubofy.reality.google.GoogleAuthManager.getUserEmail(context) ?: return false
        val userId = com.neubofy.reality.utils.IdentityManager.getUserId(context)
        if (userId.isEmpty()) return false

        // 1. Check local secure prefs
        var trialEndTime = prefs.getLong("trial_end_time_$userId", 0L)


        return trialEndTime > 0L && SecureTimeProvider.currentTimeMillis(context) < trialEndTime
    }

    fun hasUsedTrial(): Boolean {
        val userEmail = com.neubofy.reality.google.GoogleAuthManager.getUserEmail(context) ?: return false
        val userId = com.neubofy.reality.utils.IdentityManager.getUserId(context)
        if (userId.isEmpty()) return false

        // Check local
        if (prefs.contains("trial_end_time_$userId")) return true

        return false
    }




    fun isTapasyaEnabled(): Boolean = prefs.getBoolean("feature_tapasya", false)
    fun setTapasyaEnabled(enabled: Boolean) = prefs.edit().putBoolean("feature_tapasya", enabled).apply()

    fun isReminderEnabled(): Boolean = prefs.getBoolean("feature_reminder", false)
    fun setReminderEnabled(enabled: Boolean) = prefs.edit().putBoolean("feature_reminder", enabled).apply()

    fun isHealthConnectEnabled(): Boolean = prefs.getBoolean("feature_health", false)
    fun setHealthConnectEnabled(enabled: Boolean) = prefs.edit().putBoolean("feature_health", enabled).apply()
}
