with open('app/src/main/java/com/neubofy/reality/google/FirebaseAuthProxyActivity.kt', 'r') as f:
    content = f.read()

old_finish = """
    private fun finishAuth(success: Boolean) {
        val intent = Intent(GoogleSignInHelper.ACTION_FIREBASE_AUTH_SUCCESS)
        intent.putExtra("success", success)
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        finish()
        overridePendingTransition(0, 0)
    }
"""

new_finish = """
    private fun finishAuth(success: Boolean) {
        val intent = Intent(GoogleSignInHelper.ACTION_FIREBASE_AUTH_SUCCESS).apply {
            setPackage(packageName)
        }
        intent.putExtra("success", success)
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        finish()
        overridePendingTransition(0, 0)
    }
"""

content = content.replace(old_finish, new_finish)

with open('app/src/main/java/com/neubofy/reality/google/FirebaseAuthProxyActivity.kt', 'w') as f:
    f.write(content)
