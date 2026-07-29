with open('app/src/main/java/com/neubofy/reality/google/GoogleAuthManager.kt', 'r') as f:
    content = f.read()

old_isSignedIn = """
    fun isSignedIn(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_SIGNED_IN, false) &&
               !getPrefs(context).getString(KEY_ACCESS_TOKEN, null).isNullOrBlank()
    }
"""

new_isSignedIn = """
    fun isSignedIn(context: Context): Boolean {
        if (isFirebaseSession(context)) {
             return getPrefs(context).getBoolean(KEY_IS_SIGNED_IN, false)
        }
        return getPrefs(context).getBoolean(KEY_IS_SIGNED_IN, false) &&
               !getPrefs(context).getString(KEY_ACCESS_TOKEN, null).isNullOrBlank()
    }
"""

content = content.replace(old_isSignedIn, new_isSignedIn)

with open('app/src/main/java/com/neubofy/reality/google/GoogleAuthManager.kt', 'w') as f:
    f.write(content)
