package com.neubofy.reality.google

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.docs.v1.DocsScopes
import com.google.api.services.drive.DriveScopes
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.tasks.TasksScopes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.neubofy.reality.utils.SecurePreferences
import com.neubofy.reality.utils.TerminalLogger

class FirebaseAuthFragment : Fragment() {

    private var fullScopes = false
    private var onSuccess: (() -> Unit)? = null

    private val firebaseAuthLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    val idToken = account.idToken
                    if (idToken != null) {
                        firebaseAuthWithGoogle(idToken, account.serverAuthCode)
                    } else {
                        Toast.makeText(requireContext(), "Sign-in failed. No ID Token.", Toast.LENGTH_SHORT).show()
                        finishAuth(false)
                    }
                } else {
                    finishAuth(false)
                }
            } catch (e: ApiException) {
                TerminalLogger.log("Firebase Google Sign In failed: ${e.message}")
                Toast.makeText(requireContext(), "Sign-in failed.", Toast.LENGTH_SHORT).show()
                finishAuth(false)
            }
        } else {
            finishAuth(false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fullScopes = arguments?.getBoolean("fullScopes") ?: false

        val defaultWebClientIdRes = resources.getIdentifier("default_web_client_id", "string", requireContext().packageName)
        if (defaultWebClientIdRes == 0) {
            Toast.makeText(requireContext(), "Firebase configuration missing", Toast.LENGTH_LONG).show()
            finishAuth(false)
            return
        }
        val defaultWebClientId = getString(defaultWebClientIdRes)

        val gsoBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(defaultWebClientId)
            .requestEmail()

        if (fullScopes) {
            gsoBuilder.requestProfile()
            gsoBuilder.requestServerAuthCode(defaultWebClientId, true)
            gsoBuilder.requestScopes(Scope(CalendarScopes.CALENDAR))
            gsoBuilder.requestScopes(Scope(TasksScopes.TASKS))
            gsoBuilder.requestScopes(Scope(DriveScopes.DRIVE_FILE))
            gsoBuilder.requestScopes(Scope(DocsScopes.DOCUMENTS))
            gsoBuilder.requestScopes(Scope(SheetsScopes.SPREADSHEETS))
        }

        val googleSignInClient = GoogleSignIn.getClient(requireContext(), gsoBuilder.build())
        firebaseAuthLauncher.launch(googleSignInClient.signInIntent)
    }

    private fun firebaseAuthWithGoogle(idToken: String, serverAuthCode: String?) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    val user = FirebaseAuth.getInstance().currentUser
                    if (user != null) {
                        GoogleAuthManager.saveFirebaseSession(requireContext(), idToken, user.email, user.displayName, serverAuthCode)
                        SecurePreferences.get(requireContext(), "reality_features").edit()
                            .putBoolean("reality_pro_basic_sign_in", !fullScopes).apply()
                        Toast.makeText(requireContext(), "Sign-in successful!", Toast.LENGTH_SHORT).show()
                        finishAuth(true)
                    } else {
                        finishAuth(false)
                    }
                } else {
                    TerminalLogger.log("Firebase Auth failed: ${task.exception?.message}")
                    Toast.makeText(requireContext(), "Authentication Failed.", Toast.LENGTH_SHORT).show()
                    finishAuth(false)
                }
            }
    }

    private fun finishAuth(success: Boolean) {
        if (success) {
            onSuccess?.invoke()
        }
        val intent = Intent(GoogleSignInHelper.ACTION_FIREBASE_AUTH_SUCCESS).apply {
            setPackage(requireContext().packageName)
        }
        intent.putExtra("success", success)
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent)
        parentFragmentManager.beginTransaction().remove(this).commitAllowingStateLoss()
    }

    fun setCallback(callback: () -> Unit) {
        this.onSuccess = callback
    }
}
