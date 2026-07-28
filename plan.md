1.  **Update `GoogleSignInHelper.kt`:** Modify `startSignInFlow` to check `GoogleAuthManager.hasCloudCredentials(activity)`.
    -   If the user has custom cloud credentials, continue using the existing desktop auth flow (via `performSignIn` with local server callback).
    -   If not, use Google Sign-In with Firebase Auth as a fallback.
    -   We need to ensure that for the Reality Elite page (`forceBasicScope = true`), we only request basic scopes (`email`), and for the Profile page (`forceBasicScope = false`), we request all needed scopes (Tasks, Drive, Calendar, Docs, Sheets, etc.).
    -   We will create a separate hidden Activity or Fragment, or pass a lambda that triggers an intent in the given Activity since `AppCompatActivity` doesn't have an easy way to register for result outside `onCreate`. A cleaner approach is to use a hidden, transparent Activity (`FirebaseSignInActivity`) specifically for handling the `GoogleSignInClient` intent result and Firebase Auth sign-in, which will return the result to `GoogleSignInHelper`.
    -   Wait, we don't need a hidden activity, we can use `com.google.firebase.auth.FirebaseAuth.getInstance().signInWithCredential` and standard `GoogleSignInOptions`, but we still need `startActivityForResult`. Let's create `FirebaseSignInActivity` in `ui/activity` and add it to `AndroidManifest.xml`.
    -   **Action:** Add `FirebaseSignInActivity.kt`. Add it to `AndroidManifest.xml`. Modify `GoogleSignInHelper.kt` to call this new Activity.
    -   **Verify:** Verify changes using `cat` for `AndroidManifest.xml`, `GoogleSignInHelper.kt`, and the new `FirebaseSignInActivity.kt`.

2.  **Update `GoogleAuthManager.kt`:**
    -   Add `saveFirebaseAuthResult(context: Context, idToken: String, email: String, name: String, photoUrl: String)` to save the user info to `SecurePreferences` to mimic what the current desktop flow does.
    -   Ensure `isSignedIn` returns true if Firebase is signed in (which is currently checked by presence of `ACCESS_TOKEN`, we might need to modify `isSignedIn` to also accept Firebase sign in state, e.g. if `KEY_IS_SIGNED_IN` is true and either `ACCESS_TOKEN` is present or we are using Firebase Auth). We can store a boolean `KEY_IS_FIREBASE_AUTH` to distinguish.
    -   **Action:** Modify `GoogleAuthManager.kt` using `sed` or Python script.
    -   **Verify:** Verify changes using `cat` for `GoogleAuthManager.kt`.

3.  **Update `workers/identity/worker.js`:**
    -   In `/api/generate-identity`, if Google's `tokeninfo` verification fails on the `idToken`, implement a fallback to decode the JWT manually (parse the base64-encoded payload) to extract the `email` directly. This fulfills the requirement: "have no error smart fallback that detect token and use rejected way to get email if possible or a smooth fallback in identity worker".
    -   **Action:** Modify `workers/identity/worker.js` using `sed` or Python script.
    -   **Verify:** Verify changes using `cat` for `workers/identity/worker.js`.

4.  **Verify and Compile:**
    -   After modifying the worker and the Kotlin files, compile the app using `./gradlew assembleDebug` to verify no compilation errors.

5.  **Run Tests:**
    -   Run `./gradlew test` to ensure we haven't broken any existing unit tests.

6.  **Pre-commit Step:** Complete pre-commit steps to ensure proper testing, verification, review, and reflection are done.
