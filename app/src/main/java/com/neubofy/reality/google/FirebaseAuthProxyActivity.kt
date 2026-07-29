package com.neubofy.reality.google

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
import com.neubofy.reality.R
import com.neubofy.reality.utils.SecurePreferences
import com.neubofy.reality.utils.TerminalLogger
import androidx.activity.result.contract.ActivityResultContracts

class FirebaseAuthProxyActivity : AppCompatActivity() {

    private var fullScopes = false

    private val firebaseAuthLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                val idToken = account.idToken
                if (idToken != null) {
                    firebaseAuthWithGoogle(idToken, account.serverAuthCode)
                } else {
                    Toast.makeText(this, "Sign-in failed. No ID Token.", Toast.LENGTH_SHORT).show()
                    finishAuth(false)
                }
            } catch (e: ApiException) {
                TerminalLogger.log("Firebase Google Sign In failed: ${e.message}")
                Toast.makeText(this, "Sign-in failed.", Toast.LENGTH_SHORT).show()
                finishAuth(false)
            }
        } else {
            finishAuth(false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // No layout, transparent activity

        fullScopes = intent.getBooleanExtra("fullScopes", false)



        val defaultWebClientIdRes = resources.getIdentifier("default_web_client_id", "string", packageName)
        if (defaultWebClientIdRes == 0) {
            Toast.makeText(this, "Firebase configuration missing (default_web_client_id)", Toast.LENGTH_LONG).show()
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


        val googleSignInClient = GoogleSignIn.getClient(this, gsoBuilder.build())
        firebaseAuthLauncher.launch(googleSignInClient.signInIntent)
    }

    private fun firebaseAuthWithGoogle(idToken: String, serverAuthCode: String?) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = FirebaseAuth.getInstance().currentUser
                    if (user != null) {
                        GoogleAuthManager.saveFirebaseSession(this, idToken, user.email, user.displayName, serverAuthCode)
                        SecurePreferences.get(this, "reality_features").edit()
                            .putBoolean("reality_pro_basic_sign_in", !fullScopes).apply()
                        Toast.makeText(this, "Sign-in successful!", Toast.LENGTH_SHORT).show()
                        finishAuth(true)
                    } else {
                        finishAuth(false)
                    }
                } else {
                    TerminalLogger.log("Firebase Auth failed: ${task.exception?.message}")
                    Toast.makeText(this, "Authentication Failed.", Toast.LENGTH_SHORT).show()
                    finishAuth(false)
                }
            }
    }

    private fun finishAuth(success: Boolean) {
        val intent = Intent(GoogleSignInHelper.ACTION_FIREBASE_AUTH_SUCCESS)
        intent.putExtra("success", success)
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        finish()
    }
}
