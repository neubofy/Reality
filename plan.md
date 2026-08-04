1. **Update `saveFirebaseSession` inside `GoogleAuthManager`:**
    - Revert the earlier change that made `saveFirebaseSession` await `exchangeCodeForTokens` and `refreshIdentity` directly, blocking the UI.
    - Change the coroutine back to a fire-and-forget background job (using `CoroutineScope(Dispatchers.IO).launch`) so that it doesn't block `finishAuth`.
    - This will allow the basic user data (email, name, photoUrl) saved in SharedPreferences to reflect immediately upon Google Sign-In completion, while `IdentityManager.refreshIdentity` syncs in the background.

2. **Run all relevant tests:**
    - Run `./gradlew test` with necessary configuration to ensure no regressions were introduced.

3. **Run Pre Commit Steps:**
    - Ensure proper testing, verifications, reviews, and reflections are done.

4. **Submit changes**
