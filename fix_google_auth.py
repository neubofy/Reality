import re

with open('app/src/main/java/com/neubofy/reality/google/GoogleAuthManager.kt', 'r') as f:
    content = f.read()

# Let's fix accessToken generation for Firebase users.
# We'll use GoogleAuthUtil in GoogleAuthManager.kt when getting credentials, or we'll fetch it on demand.
# In `exchangeCodeForTokens`, we can skip exchanging authCode if we use Firebase.

# Wait, `GoogleAuthUtil.getToken(context, account, scopes)` works in Android to get the token!
# Let's write a method in GoogleAuthManager to fetch access token via GoogleAuthUtil.

content = content.replace(
    '        val hasCustomAuth = !clientId.isNullOrBlank() && !clientSecret.isNullOrBlank()',
    """
        if (isFirebaseSession(context)) {
            val email = getUserEmail(context)
            if (!email.isNullOrBlank()) {
                val scopeString = "oauth2:" + ALL_SCOPES.joinToString(" ")
                try {
                    val accToken = com.google.android.gms.auth.GoogleAuthUtil.getToken(context, android.accounts.Account(email, "com.google"), scopeString)
                    getPrefs(context).edit().putString(KEY_ACCESS_TOKEN, accToken).apply()
                    val gBuilder = Credential.Builder(BearerToken.authorizationHeaderAccessMethod())
                        .setTransport(getHttpTransport())
                        .setJsonFactory(getJsonFactory())
                    return gBuilder.build().setAccessToken(accToken)
                } catch(e: Exception) {
                    TerminalLogger.log("GOOGLE AUTH: Failed to fetch token via GoogleAuthUtil - ${e.message}")
                }
            }
        }
        val hasCustomAuth = !clientId.isNullOrBlank() && !clientSecret.isNullOrBlank()"""
)


with open('app/src/main/java/com/neubofy/reality/google/GoogleAuthManager.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/neubofy/reality/google/FirebaseAuthProxyActivity.kt', 'r') as f:
    proxy_content = f.read()

proxy_content = proxy_content.replace(
    'gsoBuilder.requestServerAuthCode(defaultWebClientId, true)\n            ',
    ''
)

# And GoogleSignInHelper receiver issue:
with open('app/src/main/java/com/neubofy/reality/google/GoogleSignInHelper.kt', 'r') as f:
    helper = f.read()

# We'll move the broadcast receiver registration inside the proxy activity's launcher callback, no wait, the proxy is launched BY the helper, so the helper needs to wait for the broadcast.
# But the review says: "The LocalBroadcastManager receiver registered in GoogleSignInHelper.kt is anonymous and not tied to the Activity lifecycle. If the calling Activity is destroyed or recreated... it creates a memory leak... failures in proxy activity do not unregister the receiver."
# Since GoogleSignInHelper is an object, we can't tie it easily to the lifecycle unless we use lifecycleObserver.
# Alternatively, since we just start a proxy activity, we don't need a broadcast receiver. We can just use `startActivityForResult` directly in the calling activity!
# But wait, we want to hide it from the activity.
# Okay, we can add a LifecycleObserver to the activity's lifecycle to remove the receiver when destroyed.

lifecycle_code = """
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                LocalBroadcastManager.getInstance(activity).unregisterReceiver(this)
                val success = intent?.getBooleanExtra("success", false) == true
                if (success) onSuccess()
            }
        }
        LocalBroadcastManager.getInstance(activity).registerReceiver(receiver, IntentFilter(ACTION_FIREBASE_AUTH_SUCCESS))
        activity.lifecycle.addObserver(object : androidx.lifecycle.DefaultLifecycleObserver {
            override fun onDestroy(owner: androidx.lifecycle.LifecycleOwner) {
                LocalBroadcastManager.getInstance(activity).unregisterReceiver(receiver)
            }
        })
"""
helper = re.sub(r'        val receiver = object : BroadcastReceiver\(\) \{\n.*?\n.*?\n.*?\n        \}\n        LocalBroadcastManager\.getInstance\(activity\)\.registerReceiver\(receiver, IntentFilter\(ACTION_FIREBASE_AUTH_SUCCESS\)\)', lifecycle_code, helper, flags=re.DOTALL)

with open('app/src/main/java/com/neubofy/reality/google/GoogleSignInHelper.kt', 'w') as f:
    f.write(helper)
