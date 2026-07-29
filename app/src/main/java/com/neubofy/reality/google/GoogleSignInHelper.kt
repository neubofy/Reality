package com.neubofy.reality.google

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.neubofy.reality.R
import com.neubofy.reality.utils.SecurePreferences
import com.neubofy.reality.utils.TerminalLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLDecoder

object GoogleSignInHelper {

    const val ACTION_FIREBASE_AUTH_SUCCESS = "com.neubofy.reality.FIREBASE_AUTH_SUCCESS"

    fun startSignInFlow(activity: AppCompatActivity, isAllConnected: Boolean = false, forceBasicScope: Boolean? = null, skipDialog: Boolean = false, onSuccess: () -> Unit) {
        val fullScopes = (forceBasicScope != true)

        if (GoogleAuthManager.hasCloudCredentials(activity)) {
            performSignIn(activity, fullScopes, onSuccess)
        } else {
            performFirebaseSignIn(activity, fullScopes, onSuccess)
        }
    }

    class AuthFragment : androidx.fragment.app.Fragment() {
        var fullScopes: Boolean = false
        var onSuccess: (() -> Unit)? = null

        override fun onCreateView(
            inflater: android.view.LayoutInflater,
            container: android.view.ViewGroup?,
            savedInstanceState: android.os.Bundle?
        ): android.view.View? {
            return inflater.inflate(com.neubofy.reality.R.layout.fragment_auth_loading, container, false)
        }

        private val launcher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                    val idToken = account?.idToken
                    if (idToken != null) {
                        firebaseAuthWithGoogle(idToken, account.serverAuthCode, account.account)
                    } else {
                        Toast.makeText(requireContext(), "Sign-in failed. No ID Token.", Toast.LENGTH_SHORT).show()
                        finishAuth(false)
                    }
                } catch (e: Exception) {
                    TerminalLogger.log("GOOGLE AUTH: Sign-in failed or cancelled - ${e.message}")
                    finishAuth(false)
                }
            } else {
                finishAuth(false)
            }
        }

        private fun firebaseAuthWithGoogle(idToken: String, serverAuthCode: String?, googleAccount: android.accounts.Account?) {
            val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
            com.google.firebase.auth.FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnCompleteListener(requireActivity()) { task ->
                    if (task.isSuccessful) {
                        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                        if (user != null) {
                            lifecycleScope.launch {
                                var accessToken: String? = null
                                
                                if (fullScopes && googleAccount != null) {
                                    val scopes = "oauth2:https://www.googleapis.com/auth/calendar https://www.googleapis.com/auth/tasks https://www.googleapis.com/auth/drive.file https://www.googleapis.com/auth/documents"
                                    try {
                                        accessToken = withContext(Dispatchers.IO) {
                                            com.google.android.gms.auth.GoogleAuthUtil.getToken(requireContext(), googleAccount, scopes)
                                        }
                                    } catch (e: Exception) {
                                        TerminalLogger.log("GOOGLE AUTH: Failed to get GoogleAuthUtil token - ${e.message}")
                                    }
                                }

                                GoogleAuthManager.saveFirebaseSession(
                                    requireContext(), idToken,
                                    user.email, user.displayName, accessToken ?: serverAuthCode
                                )
                                SecurePreferences.get(requireContext(), "reality_features").edit()
                                    .putBoolean("reality_pro_basic_sign_in", !fullScopes).apply()
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(requireContext(), "Sign-in successful!", Toast.LENGTH_SHORT).show()
                                    finishAuth(true)
                                }
                            }
                        } else {
                            finishAuth(false)
                        }
                    } else {
                        TerminalLogger.log("GOOGLE AUTH: Firebase Auth Failed - ${task.exception?.message}")
                        Toast.makeText(requireContext(), "Firebase Auth Failed.", Toast.LENGTH_SHORT).show()
                        finishAuth(false)
                    }
                }
        }

        private fun finishAuth(success: Boolean) {
            if (success) {
                onSuccess?.invoke()
            }
            parentFragmentManager.beginTransaction().remove(this).commitAllowingStateLoss()
        }

        override fun onCreate(savedInstanceState: android.os.Bundle?) {
            super.onCreate(savedInstanceState)
            val defaultWebClientIdRes = resources.getIdentifier("default_web_client_id", "string", requireContext().packageName)
            if (defaultWebClientIdRes == 0) {
                Toast.makeText(requireContext(), "Firebase configuration missing (default_web_client_id)", Toast.LENGTH_LONG).show()
                finishAuth(false)
                return
            }
            
            val defaultWebClientId = getString(defaultWebClientIdRes)
            
            val gsoBuilder = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(defaultWebClientId)
                .requestEmail()

            if (fullScopes) {
                gsoBuilder.requestServerAuthCode(defaultWebClientId)
                gsoBuilder.requestScopes(
                    com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/calendar"),
                    com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/tasks"),
                    com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/drive.file"),
                    com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/documents")
                )
            }

            val googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(requireContext(), gsoBuilder.build())
            googleSignInClient.signOut().addOnCompleteListener {
                launcher.launch(googleSignInClient.signInIntent)
            }
        }
    }

    private fun performFirebaseSignIn(activity: AppCompatActivity, fullScopes: Boolean, onSuccess: () -> Unit) {
        val fragment = AuthFragment().apply {
            this.fullScopes = fullScopes
            this.onSuccess = onSuccess
        }
        activity.supportFragmentManager.beginTransaction()
            .add(fragment, "AuthFragment")
            .commitAllowingStateLoss()
    }

    fun showCloudKeySettings(activity: AppCompatActivity) {
        showCustomKeyDialog(activity, null) {}
    }

    private fun showCustomKeyDialog(activity: AppCompatActivity, forceBasicScope: Boolean?, onSuccess: () -> Unit) {
        val dialogView = activity.layoutInflater.inflate(R.layout.dialog_cloud_settings, null)
        val etClientId = dialogView.findViewById<EditText>(R.id.et_client_id)
        val etClientSecret = dialogView.findViewById<EditText>(R.id.et_client_secret)

        val customId = GoogleAuthManager.getCustomClientId(activity)
        val customSecret = GoogleAuthManager.getCustomClientSecret(activity)

        if (!customId.isNullOrBlank()) {
            etClientId.setText(customId)
        } else {
            etClientId.setText("")
        }

        if (!customSecret.isNullOrBlank()) {
            etClientSecret.setText(customSecret)
        } else {
            etClientSecret.setText("")
        }

        MaterialAlertDialogBuilder(activity)
            .setTitle("Google Cloud Setup")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val clientId = etClientId.text.toString().trim()
                val clientSecret = etClientSecret.text.toString().trim()
                GoogleAuthManager.saveCloudCredentials(activity, clientId, clientSecret)
                Toast.makeText(activity, "Credentials saved", Toast.LENGTH_SHORT).show()
                onSuccess()
            }
            .setNeutralButton("Clear") { _, _ ->
                GoogleAuthManager.saveCloudCredentials(activity, "", "")
                Toast.makeText(activity, "Credentials cleared", Toast.LENGTH_SHORT).show()
                onSuccess()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performSignIn(activity: AppCompatActivity, fullScopes: Boolean, onSuccess: () -> Unit) {
        val url = GoogleAuthManager.getAuthUrl(activity, basicOnly = !fullScopes)
        if (url != null) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            activity.startActivity(intent)

            // Start local server to listen for redirect (Auto-catching)
            activity.lifecycleScope.launch {
                val autoCode = GoogleAuthManager.startLocalServerAndGetCode()

                var success = false
                if (autoCode != null) {
                    success = GoogleAuthManager.exchangeCodeForTokens(activity, autoCode)
                    if (success) {
                        withContext(Dispatchers.Main) {
                            SecurePreferences.get(activity, "reality_features").edit()
                                .putBoolean("reality_pro_basic_sign_in", !fullScopes).apply()
                            TerminalLogger.log("GOOGLE AUTH: Auto-Signed in successfully")
                            Toast.makeText(activity, "Sign-in successful!", Toast.LENGTH_SHORT).show()
                            onSuccess()
                        }
                    } else {
                        TerminalLogger.log("GOOGLE AUTH: Auto-Sign-in token exchange failed. Falling back to manual entry.")
                    }
                }

                if (!success) {
                    // Fallback to manual entry if server timed out or failed to catch/exchange
                    withContext(Dispatchers.Main) {
                        showManualCodeDialog(activity, fullScopes, onSuccess)
                    }
                }
            }
        } else {
            Toast.makeText(activity, "Error generating Auth URL. Check Keys.", Toast.LENGTH_LONG).show()
        }
    }

    private fun showManualCodeDialog(activity: AppCompatActivity, fullScopes: Boolean, onSuccess: () -> Unit) {
        val input = EditText(activity)
        input.hint = "Paste URL or Code here"

        MaterialAlertDialogBuilder(activity)
            .setTitle("Enter Auth Code or URL")
            .setMessage("Auto-catch timed out or failed. If the browser shows 'Site can't be reached', copy the entire URL from the address bar and paste it here.")
            .setView(input)
            .setPositiveButton("Submit") { _, _ ->
                var code = input.text.toString().trim()
                if (code.contains("code=")) {
                    try {
                        val uri = Uri.parse(code)
                        code = uri.getQueryParameter("code") ?: code
                    } catch (e: Exception) {
                        val match = Regex("code=([^&]+)").find(code)
                        if (match != null) {
                            code = match.groupValues[1]
                            try {
                                code = URLDecoder.decode(code, "UTF-8")
                            } catch (e2: Exception) {}
                        }
                    }
                }
                if (code.isNotEmpty()) {
                    activity.lifecycleScope.launch {
                        val manualSuccess = GoogleAuthManager.exchangeCodeForTokens(activity, code)
                        withContext(Dispatchers.Main) {
                            if (manualSuccess) {
                                SecurePreferences.get(activity, "reality_features").edit()
                                    .putBoolean("reality_pro_basic_sign_in", !fullScopes).apply()
                                Toast.makeText(activity, "Sign-in successful!", Toast.LENGTH_SHORT).show()
                                onSuccess()
                            } else {
                                Toast.makeText(activity, "Sign-in failed. Check credentials.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
