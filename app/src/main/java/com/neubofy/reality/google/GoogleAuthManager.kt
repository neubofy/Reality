package com.neubofy.reality.google

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.api.client.auth.oauth2.AuthorizationCodeFlow
import com.google.api.client.auth.oauth2.BearerToken
import com.google.api.client.auth.oauth2.ClientParametersAuthentication
import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.docs.v1.DocsScopes
import com.google.api.services.drive.DriveScopes
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.tasks.TasksScopes
import com.neubofy.reality.utils.TerminalLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Centralized Google Authentication Manager using BYOK (Bring Your Own Key) Desktop OAuth.
 */
object GoogleAuthManager {
    
    @Volatile
    private var activeServerSocket: java.net.ServerSocket? = null

    @Volatile
    var activeLocalPort: Int = 8080
        private set

    fun prepareLocalServer(): Int {
        try {
            activeServerSocket?.close()
        } catch (_: Exception) {}
        activeServerSocket = null

        // Try ports from 8080 to 8090
        for (port in 8080..8090) {
            try {
                val socket = java.net.ServerSocket(port, 50, java.net.InetAddress.getByName("127.0.0.1"))
                activeServerSocket = socket
                activeLocalPort = port
                return port
            } catch (e: Exception) {
                // port busy, continue
            }
        }

        // Fallback to random port
        try {
            val socket = java.net.ServerSocket(0, 50, java.net.InetAddress.getByName("127.0.0.1"))
            activeServerSocket = socket
            activeLocalPort = socket.localPort
            return activeLocalPort
        } catch (e: Exception) {
            activeLocalPort = 8080
            return 8080
        }
    }

    private const val PREF_NAME = "google_auth_prefs"
    private const val KEY_CLIENT_ID = "client_id"
    private const val KEY_CLIENT_SECRET = "client_secret"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_USER_NAME = "user_display_name"
    private const val KEY_USER_PHOTO_URL = "user_photo_url"
    private const val KEY_IS_SIGNED_IN = "is_signed_in"
    private const val KEY_ID_TOKEN = "id_token"
    private const val KEY_FIREBASE_SESSION = "firebase_session"
    
    val ALL_SCOPES = listOf(
        "openid",
        "https://www.googleapis.com/auth/calendar.events",
        TasksScopes.TASKS,
        DriveScopes.DRIVE_FILE,
        DocsScopes.DOCUMENTS,
        SheetsScopes.SPREADSHEETS,
        "email",
        "profile"
    )
    
    val BASIC_SCOPES = listOf("email")

    private fun getPrefs(context: Context) = 
        com.neubofy.reality.utils.SecurePreferences.get(context, PREF_NAME)

    private fun getConnectorPrefs(context: Context) =
        com.neubofy.reality.utils.SecurePreferences.get(context, "google_connector_prefs")

    fun saveCloudCredentials(context: Context, clientId: String, clientSecret: String) {
        getPrefs(context).edit().apply {
            putString(KEY_CLIENT_ID, clientId)
            putString(KEY_CLIENT_SECRET, clientSecret)
            apply()
        }
    }
    
    fun getCustomClientId(context: Context): String? {
        return getPrefs(context).getString(KEY_CLIENT_ID, null)
    }

    fun getCustomClientSecret(context: Context): String? {
        return getPrefs(context).getString(KEY_CLIENT_SECRET, null)
    }


    suspend fun saveFirebaseSession(context: Context, idToken: String, email: String?, name: String?, photoUrl: String? = null, accessTokenOrAuthCode: String? = null) {
        getPrefs(context).edit().apply {
            putBoolean(KEY_FIREBASE_SESSION, true)
            putString(KEY_ID_TOKEN, idToken)
            putBoolean(KEY_IS_SIGNED_IN, true)
            if (email != null) putString(KEY_USER_EMAIL, email)
            if (name != null) putString(KEY_USER_NAME, name)
            if (photoUrl != null) putString(KEY_USER_PHOTO_URL, photoUrl)
            
            // If the string starts with ya29 (Google access token prefix), save it directly
            if (accessTokenOrAuthCode != null && accessTokenOrAuthCode.startsWith("ya29")) {
                putString(KEY_ACCESS_TOKEN, accessTokenOrAuthCode)
            }
            apply()
        }
        
        if (accessTokenOrAuthCode != null && !accessTokenOrAuthCode.startsWith("ya29")) {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                exchangeCodeForTokens(context, accessTokenOrAuthCode)
            }
        }
        com.neubofy.reality.utils.IdentityManager.refreshIdentity(context.applicationContext)
    }

    fun isFirebaseSession(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_FIREBASE_SESSION, false)
    }

    fun getClientId(context: Context): String? {
        return getCustomClientId(context)
    }

    fun getClientSecret(context: Context): String? {
        return getCustomClientSecret(context)
    }

    fun hasCloudCredentials(context: Context): Boolean {
        val customHasCredentials = !getClientId(context).isNullOrBlank() && !getClientSecret(context).isNullOrBlank()
        return customHasCredentials
    }
    



    fun getAuthUrl(context: Context, basicOnly: Boolean = false): String? {
        val scopes = if (basicOnly) BASIC_SCOPES else ALL_SCOPES
        val clientId = getClientId(context)
        val workerUrl = com.neubofy.reality.BuildConfig.WORKER_URL

        val port = prepareLocalServer()
        val redirectUri = "http://127.0.0.1:$port/Callback"

        if (!clientId.isNullOrBlank()) {
            TerminalLogger.log("GOOGLE AUTH: Using developer/user cloud credential through no-knowledge architecture secure Google login.")
            return GoogleAuthorizationCodeRequestUrl(
                clientId,
                redirectUri,
                scopes
            )
            .setAccessType("offline")
            .build()
        }
        return null
    }
    

    suspend fun startLocalServerAndGetCode(): String? {
        return withContext(Dispatchers.IO) {
            var server = activeServerSocket
            if (server == null || server.isClosed) {
                try {
                    server = java.net.ServerSocket(activeLocalPort, 50, java.net.InetAddress.getByName("127.0.0.1"))
                    activeServerSocket = server
                } catch (e: Exception) {
                    TerminalLogger.log("GOOGLE AUTH: Failed to start server socket - ${e.message}")
                    return@withContext null
                }
            }
            try {
                // Set a timeout so we don't block forever if the user closes the browser
                server.soTimeout = 120000 // 2 minutes timeout

                val socket = server.accept()
                val reader = java.io.BufferedReader(java.io.InputStreamReader(socket.inputStream))
                var line = reader.readLine()
                var code: String? = null

                while (line != null && line.isNotEmpty()) {
                    if (line.startsWith("GET ")) {
                        // Example: GET /Callback?code=4/0AeaYSHD8...&scope=... HTTP/1.1
                        val parts = line.split(" ")
                        if (parts.size > 1) {
                            val pathAndQuery = parts[1]
                            val match = Regex("[?&]code=([^&]+)").find(pathAndQuery)
                            if (match != null) {
                                code = match.groupValues[1]
                                try {
                                    code = java.net.URLDecoder.decode(code, "UTF-8")
                                } catch (e: Exception) {}
                            }
                        }
                    }
                    line = reader.readLine()
                }

                val output = "HTTP/1.1 200 OK\r\nContent-Type: text/html\r\n\r\n<!DOCTYPE html><html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"><style>body{background-color:#05050A;color:#FFFFFF;font-family:monospace;display:flex;flex-direction:column;align-items:center;justify-content:center;height:100vh;margin:0;text-align:center;}h1{color:#00E5FF;text-shadow:0 0 10px rgba(0,229,255,0.5);}p{color:#7B61FF;margin-top:10px;}#countdown{font-size:2rem;font-weight:bold;color:#00E5FF;margin-top:20px;}</style></head><body><h1>Reality Authorization</h1><p>Authentication captured successfully.</p><p>Please wait while we process the token...</p><div id=\"countdown\">10</div><script>var timeLeft = 10;var el = document.getElementById('countdown');var timerId = setInterval(function() {if (timeLeft <= 0) {clearInterval(timerId);el.innerHTML = 'Process Complete. You may close this window and return to Reality.';} else {el.innerHTML = timeLeft;timeLeft -= 1;}}, 1000);</script></body></html>"
                socket.outputStream.write(output.toByteArray())
                socket.outputStream.flush()
                socket.close()
                return@withContext code
            } catch (e: Exception) {
                TerminalLogger.log("GOOGLE AUTH: Server error or timeout - ${e.message}")
                com.neubofy.reality.utils.TerminalLogger.log("ERROR: ${e.message}")
                null
            } finally {
                try {
                    server.close()
                } catch (_: Exception) {}
                activeServerSocket = null
            }
        }
    }

    suspend fun refreshTokenIfNeeded(context: Context, force: Boolean = false): Boolean {
        return withContext(Dispatchers.IO) {
            try {

            if (isFirebaseSession(context)) {
                val userEmail = getPrefs(context).getString(KEY_USER_EMAIL, null)
                val oldAccessToken = getPrefs(context).getString(KEY_ACCESS_TOKEN, null)
                
                if (userEmail != null) {
                    try {
                        val isExpired = oldAccessToken != null && isJwtExpired(oldAccessToken)
                        if (force || isExpired || oldAccessToken == null) {
                            if (force && oldAccessToken != null) {
                                com.google.android.gms.auth.GoogleAuthUtil.clearToken(context, oldAccessToken)
                            }
                            val account = android.accounts.Account(userEmail, "com.google")
                            
                            val webClientId = context.getString(context.resources.getIdentifier("default_web_client_id", "string", context.packageName))
                            val scopes = "oauth2:" + ALL_SCOPES.joinToString(" ")
                            val newAccessToken = com.google.android.gms.auth.GoogleAuthUtil.getToken(context, account, scopes)
                            
                            val idTokenScopes = "audience:server:client_id:$webClientId"
                            val newIdToken = com.google.android.gms.auth.GoogleAuthUtil.getToken(context, account, idTokenScopes)
                            
                            getPrefs(context).edit().apply {
                                putString(KEY_ACCESS_TOKEN, newAccessToken)
                                putString(KEY_ID_TOKEN, newIdToken)
                                apply()
                            }
                            TerminalLogger.log("GOOGLE AUTH: Firebase (Google) tokens refreshed successfully via GoogleAuthUtil")
                            return@withContext true
                        } else {
                            TerminalLogger.log("GOOGLE AUTH: Firebase (Google) tokens still valid, skipping refresh.")
                            return@withContext true
                        }
                    } catch (e: Exception) {
                        TerminalLogger.log("GOOGLE AUTH: Failed to refresh GoogleAuthUtil token - ${e.message}")
                        return@withContext false
                    }
                }
                return@withContext false
            }

                // To properly refresh the id_token (required for Identity Worker), we MUST use the manual 
                // direct exchange because GoogleCredential.refreshToken() only returns an access_token.
                val refreshToken = getPrefs(context).getString(KEY_REFRESH_TOKEN, null)
                val clientId = getClientId(context)
                val clientSecret = getClientSecret(context)
                val workerUrl = com.neubofy.reality.BuildConfig.WORKER_URL

                if (refreshToken != null) {
                    val accessToken: String?
                    val idToken: String?
                    
                    if (!clientId.isNullOrBlank() && !clientSecret.isNullOrBlank()) {
                        val tokenResponse = com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest(
                            getHttpTransport(),
                            getJsonFactory(),
                            refreshToken,
                            clientId,
                            clientSecret
                        ).execute()
                        accessToken = tokenResponse.accessToken
                        idToken = tokenResponse.idToken
                    } else {
                         val cleanWorkerUrl = workerUrl.removeSuffix("/")
                         val url = URL("$cleanWorkerUrl/oauth/token")
                         val conn = url.openConnection() as HttpURLConnection
                         conn.requestMethod = "POST"
                         conn.setRequestProperty("Content-Type", "application/json")
                         conn.doOutput = true

                         val jsonBody = JSONObject()
                         jsonBody.put("refresh_token", refreshToken)
                         jsonBody.put("grant_type", "refresh_token")

                         java.io.OutputStreamWriter(conn.outputStream).use { writer ->
                             writer.write(jsonBody.toString())
                         }

                         if (conn.responseCode == 200) {
                             val response = conn.inputStream.bufferedReader().use { it.readText() }
                             val json = JSONObject(response)
                             accessToken = json.optString("access_token", null)
                             idToken = json.optString("id_token", null)
                         } else {
                             accessToken = null
                             idToken = null
                         }
                    }
                    
                    if (accessToken != null) {
                        getPrefs(context).edit().apply {
                            putString(KEY_ACCESS_TOKEN, accessToken)
                            if (idToken != null) {
                                putString(KEY_ID_TOKEN, idToken)
                            }
                            apply()
                        }
                        TerminalLogger.log("GOOGLE AUTH: Token refreshed successfully via direct exchange")
                        return@withContext true
                    }
                }
            } catch (e: Exception) {
                TerminalLogger.log("GOOGLE AUTH: Token refresh failed - ${e.message}")
            }
            return@withContext false
        }
    }

    suspend fun exchangeCodeForTokens(context: Context, code: String): Boolean {
        return withContext(Dispatchers.IO) {
            val clientId = getClientId(context)
            val clientSecret = getClientSecret(context)
            val workerUrl = com.neubofy.reality.BuildConfig.WORKER_URL

            if (clientId.isNullOrBlank() || clientSecret.isNullOrBlank()) {
                if (workerUrl.isBlank()) {
                    return@withContext false
                }
            }

            try {
                val accessToken: String?
                val refreshToken: String?
                val idToken: String?

                if (!clientId.isNullOrBlank() && !clientSecret.isNullOrBlank()) {
                    val tokenResponse = GoogleAuthorizationCodeTokenRequest(
                        getHttpTransport(),
                        getJsonFactory(),
                        clientId,
                        clientSecret,
                        code,
                        "http://127.0.0.1:$activeLocalPort/Callback"
                      ).execute()
                    accessToken = tokenResponse.accessToken
                    refreshToken = tokenResponse.refreshToken
                    idToken = tokenResponse.idToken
                } else {
                     accessToken = null
                     refreshToken = null
                     idToken = null
                }

                if (accessToken != null) {
                    getPrefs(context).edit().apply {
                        putString(KEY_ACCESS_TOKEN, accessToken)
                        if (refreshToken != null) {
                            putString(KEY_REFRESH_TOKEN, refreshToken)
                        }
                        if (idToken != null) {
                            putString(KEY_ID_TOKEN, idToken)
                        }
                        putBoolean(KEY_IS_SIGNED_IN, true)
                        apply()
                    }
                    
                    // Fetch user info
                    fetchAndSaveUserInfo(context, accessToken)
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch { com.neubofy.reality.utils.IdentityManager.refreshIdentity(context.applicationContext) }

                    return@withContext true
                }
            } catch (e: Exception) {
                TerminalLogger.log("GOOGLE AUTH: Token exchange failed - ${e.message}")
                com.neubofy.reality.utils.TerminalLogger.log("ERROR: ${e.message}")
            }
            return@withContext false
        }
    }

    private suspend fun fetchAndSaveUserInfo(context: Context, accessToken: String) {
        try {
            val url = URL("https://www.googleapis.com/oauth2/v2/userinfo")
            val connection = url.openConnection() as HttpURLConnection
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            connection.requestMethod = "GET"

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)

                val email = json.optString("email", "")
                val name = json.optString("name", context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE).getString("user_name", "User"))
                val picture = json.optString("picture", "")

                getPrefs(context).edit().apply {
                    putString(KEY_USER_EMAIL, email)
                    putString(KEY_USER_NAME, name)
                    putString(KEY_USER_PHOTO_URL, picture)
                    apply()
                }
                TerminalLogger.log("GOOGLE AUTH: Saved user info for $email")
            }
        } catch (e: Exception) {
            TerminalLogger.log("GOOGLE AUTH: Failed to fetch user info - ${e.message}")
        }
    }

    fun isSignedIn(context: Context): Boolean {
        val prefs = getPrefs(context)
        if (!prefs.getBoolean(KEY_IS_SIGNED_IN, false)) return false
        if (isFirebaseSession(context)) {
            val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (user != null) return true
            return !prefs.getString(KEY_ID_TOKEN, null).isNullOrBlank()
        }
        return !prefs.getString(KEY_ACCESS_TOKEN, null).isNullOrBlank()
    }

    fun getUserEmail(context: Context): String? = getPrefs(context).getString(KEY_USER_EMAIL, null)
    fun getUserName(context: Context): String? = getPrefs(context).getString(KEY_USER_NAME, null)
    fun getUserPhotoUrl(context: Context): String? = getPrefs(context).getString(KEY_USER_PHOTO_URL, null)
    
    fun getIdToken(context: Context): String? {
        val prefs = getPrefs(context)
        val cached = prefs.getString(KEY_ID_TOKEN, null)
        if (!cached.isNullOrBlank()) return cached
        
        if (isFirebaseSession(context)) {
            val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (user != null) {
                var token: String? = null
                try {
                    val task = user.getIdToken(false)
                    val latch = java.util.concurrent.CountDownLatch(1)
                    task.addOnCompleteListener { res ->
                        if (res.isSuccessful) token = res.result?.token
                        latch.countDown()
                    }
                    latch.await(3, java.util.concurrent.TimeUnit.SECONDS)
                } catch (_: Exception) {}
                if (token != null) {
                    prefs.edit().putString(KEY_ID_TOKEN, token).apply()
                    return token
                }
            }
        }
        return null
    }
    
    fun signOut(context: Context) {
        val clientId = getClientId(context)
        val clientSecret = getClientSecret(context)

        if (isFirebaseSession(context)) {
            try {
                com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(
                    context,
                    com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
                ).signOut()
            } catch (e: Exception) {
                TerminalLogger.log("GOOGLE AUTH: Error during Firebase signout - ${e.message}")
            }
        }
        val featureManager = com.neubofy.reality.utils.FeatureManager(context)
        featureManager.setRealityProVerified(false)
        featureManager.setRealityProEnabled(false)

        getPrefs(context).edit().clear().apply()
        // Keep credentials
        if (clientId != null && clientSecret != null) {
            saveCloudCredentials(context, clientId, clientSecret)
        }

        com.neubofy.reality.utils.SecurePreferences.get(context, "google_connector_prefs").edit().clear().apply()
        com.neubofy.reality.utils.IdentityManager.clearIdentity(context)
        TerminalLogger.log("GOOGLE AUTH: Explicit sign out completed")
    }

    private var lastAuthFailureTime = 0L

    fun handleAuthFailure(context: Context) {
        if (!isSignedIn(context)) return
        
        val now = System.currentTimeMillis()
        if (now - lastAuthFailureTime < 5000) return
        lastAuthFailureTime = now

        TerminalLogger.log("GOOGLE AUTH: Token issue detected - attempting background refresh")
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            refreshTokenIfNeeded(context, force = true)
        }
    }

    fun isFullWorkspaceConnected(context: Context): Boolean {
        if (!isSignedIn(context)) return false
        val isBasicSignIn = com.neubofy.reality.utils.SecurePreferences.get(context, "reality_features")
            .getBoolean("reality_pro_basic_sign_in", false)
        return !isBasicSignIn
    }

    fun getGoogleCredential(context: Context): Credential? {
        val accessToken = getPrefs(context).getString(KEY_ACCESS_TOKEN, null)
        val refreshToken = getPrefs(context).getString(KEY_REFRESH_TOKEN, null)
        val clientId = getClientId(context)
        val clientSecret = getClientSecret(context)
        val workerUrl = com.neubofy.reality.BuildConfig.WORKER_URL
        
        val isBasicSignIn = com.neubofy.reality.utils.SecurePreferences.get(context, "reality_features")
            .getBoolean("reality_pro_basic_sign_in", false)
        if (isBasicSignIn) {
            TerminalLogger.log("GOOGLE AUTH: Workspace connection denied - basic scope only.")
            return null
        }


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
                        
                    val credential = object : Credential(gBuilder) {
                        private var retryCount = 0
                        
                        override fun handleResponse(
                            request: com.google.api.client.http.HttpRequest,
                            response: com.google.api.client.http.HttpResponse,
                            supportsRetry: Boolean
                        ): Boolean {
                            if (response.statusCode == 401 && retryCount < 2) {
                                retryCount++
                                try {
                                    com.google.android.gms.auth.GoogleAuthUtil.clearToken(context, this.accessToken)
                                    val newToken = com.google.android.gms.auth.GoogleAuthUtil.getToken(context, android.accounts.Account(email, "com.google"), scopeString)
                                    this.setAccessToken(newToken)
                                    getPrefs(context).edit().putString(KEY_ACCESS_TOKEN, newToken).apply()
                                    TerminalLogger.log("GOOGLE AUTH: Token auto-refreshed seamlessly after 401 (Attempt $retryCount)")
                                    return true // Instructs Java Client to retry the failed request
                                } catch (e: Exception) {
                                    TerminalLogger.log("GOOGLE AUTH: Auto-refresh failed: ${e.message}")
                                }
                            }
                            return super.handleResponse(request, response, supportsRetry)
                        }
                    }
                    credential.setAccessToken(accToken)
                    return credential
                } catch(e: Exception) {
                    TerminalLogger.log("GOOGLE AUTH: Failed to fetch token via GoogleAuthUtil - ${e.message}")
                }
            }
        }
        val hasCustomAuth = !clientId.isNullOrBlank() && !clientSecret.isNullOrBlank()

        if (!hasCustomAuth && workerUrl.isBlank()) {
            return null
        }
        
        val builder = Credential.Builder(BearerToken.authorizationHeaderAccessMethod())
            .setTransport(getHttpTransport())
            .setJsonFactory(getJsonFactory())

        if (hasCustomAuth) {
            builder.setTokenServerUrl(com.google.api.client.http.GenericUrl("https://oauth2.googleapis.com/token"))
            builder.setClientAuthentication(ClientParametersAuthentication(clientId, clientSecret))
        } else {
            val cleanWorkerUrl = workerUrl.removeSuffix("/")
            builder.setTokenServerUrl(com.google.api.client.http.GenericUrl("$cleanWorkerUrl/oauth/token"))
            // Provide a dummy client authentication to satisfy the Java client requirement. The worker doesn't need them.
            builder.setClientAuthentication(ClientParametersAuthentication("dummy_id", "dummy_secret"))
        }

        builder.addRefreshListener(object : com.google.api.client.auth.oauth2.CredentialRefreshListener {
            override fun onTokenResponse(
                credential: Credential,
                tokenResponse: com.google.api.client.auth.oauth2.TokenResponse
            ) {
                getPrefs(context).edit().apply {
                    putString(KEY_ACCESS_TOKEN, credential.accessToken)
                    if (credential.refreshToken != null) {
                        putString(KEY_REFRESH_TOKEN, credential.refreshToken)
                    }
                    apply()
                }
                TerminalLogger.log("GOOGLE AUTH: Access token refreshed automatically and saved")
            }

            override fun onTokenErrorResponse(
                credential: Credential,
                tokenErrorResponse: com.google.api.client.auth.oauth2.TokenErrorResponse
            ) {
                TerminalLogger.log("GOOGLE AUTH: Token error response received: ${tokenErrorResponse.error}")
            }
        })

        return builder.build()
            .setAccessToken(accessToken)
            .setRefreshToken(refreshToken)
    }

    // Dummy sign in returning boolean instead of GoogleIdTokenCredential
    suspend fun signIn(activity: Activity): Boolean {
        // This is now manual.
        return isSignedIn(activity)
    }
    
    fun getHttpTransport() = com.google.api.client.extensions.android.http.AndroidHttp.newCompatibleTransport()
    fun getJsonFactory() = com.google.api.client.json.gson.GsonFactory.getDefaultInstance()
    
    fun hasRequiredPermissions(context: Context): Boolean {
        return isSignedIn(context)
    }

    suspend fun <T> runWithAutoTokenRefresh(context: Context, block: suspend () -> T): T {
        return try {
            block()
        } catch (e: com.google.api.client.googleapis.json.GoogleJsonResponseException) {
            if (e.statusCode == 401 || e.statusCode == 403) {
                TerminalLogger.log("GOOGLE AUTH: API returned ${e.statusCode}, attempting force token refresh...")
                val refreshed = refreshTokenIfNeeded(context, force = true)
                if (refreshed) {
                    return block()
                }
            }
            throw e
        } catch (e: Exception) {
            val msg = e.message?.lowercase() ?: ""
            if (msg.contains("401") || msg.contains("invalid_credentials") || msg.contains("token expired")) {
                TerminalLogger.log("GOOGLE AUTH: Token expired in request, attempting force refresh...")
                val refreshed = refreshTokenIfNeeded(context, force = true)
                if (refreshed) {
                    return block()
                }
            }
            throw e
        }
    }

    private fun isJwtExpired(token: String): Boolean {
        try {
            val parts = token.split(".")
            if (parts.size != 3) return false
            val payload = String(android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE))
            val json = JSONObject(payload)
            val exp = json.optLong("exp", 0)
            if (exp == 0L) return false
            // Refresh if within 5 minutes of expiration
            return (exp * 1000) - System.currentTimeMillis() < 300000
        } catch (e: Exception) {
            return false
        }
    }
}
