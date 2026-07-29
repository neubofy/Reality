import re

with open('app/src/main/java/com/neubofy/reality/google/GoogleSignInHelper.kt', 'r') as f:
    content = f.read()

old_fun = """
    private fun performFirebaseSignIn(activity: AppCompatActivity, fullScopes: Boolean, onSuccess: () -> Unit) {

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


        val intent = Intent(activity, FirebaseAuthProxyActivity::class.java)
        intent.putExtra("fullScopes", fullScopes)
        activity.startActivity(intent)
    }
"""

new_fun = """
    private fun performFirebaseSignIn(activity: AppCompatActivity, fullScopes: Boolean, onSuccess: () -> Unit) {
        val fragment = FirebaseAuthFragment().apply {
            arguments = android.os.Bundle().apply {
                putBoolean("fullScopes", fullScopes)
            }
            setCallback(onSuccess)
        }
        activity.supportFragmentManager.beginTransaction()
            .add(fragment, "FirebaseAuthFragment")
            .commit()
    }
"""

content = content.replace(old_fun, new_fun)

with open('app/src/main/java/com/neubofy/reality/google/GoogleSignInHelper.kt', 'w') as f:
    f.write(content)
