1. **Fix Loading UI:**
    - Modify `fragment_auth_loading.xml` to remove `ProgressBar` and adjust constraints so `tv_loading_message` centers properly below `iv_logo`.
    - Create `res/anim/pulse_zoom.xml` and `res/anim/pulse_alpha.xml` containing zoom (scale) and pulse (alpha) animations.
    - Update `BaseActivity.showLoading()` to start these animations on `iv_logo` and `tv_loading_message` respectively.

2. **Fix delayed Identity UI reflection during Google/Firebase Login:**
    - The issue is that `saveFirebaseSession` is called in `GoogleSignInHelper.kt` asynchronously. The loading screen finishes and the UI updates BEFORE `IdentityManager.refreshIdentity` completes because `IdentityManager.refreshIdentity` takes time and the UI update isn't waiting for it.
    - `GoogleSignInHelper` has `GoogleAuthManager.saveFirebaseSession` call. Since `saveFirebaseSession` is a `suspend` function, calling it inside `lifecycleScope.launch` correctly suspends, but there was an issue where `IdentityManager.refreshIdentity` might not have been awaited properly inside `saveFirebaseSession`.
    - Make sure `exchangeCodeForTokens` inside `saveFirebaseSession` is awaited (I already updated this).
    - `saveFirebaseSession` awaits `com.neubofy.reality.utils.IdentityManager.refreshIdentity(context.applicationContext)`.
    - This ensures that by the time `saveFirebaseSession` returns, the identity is fully cached locally, the broadcast `com.neubofy.reality.IDENTITY_UPDATED` is sent, and `updateStateUI()` will fetch the correct values.

3. **Performance and Battery Efficiency Audit:**
    - I reviewed `AppBlockerService.kt`, particularly the `refreshReceiver` and `timeTickReceiver`.
    - `refreshReceiver` properly uses `Dispatchers.IO` for reloading strict mode data, avoiding main thread blockage.
    - `timeTickReceiver` runs every minute via `ACTION_TIME_TICK`, which is highly efficient.
    - The memory noted that avoiding heavy I/O operations inside `ACTION_USER_PRESENT` (screen unlock) is important. Looking at `refreshReceiver`, `ACTION_USER_PRESENT` only sets `isScreenOn = true` and conditionally calls `browserWatchdog.startBrowserCheckTimer()`. This is very efficient and complies with the memory rule.

4. **Verify functionality:**
    - I will make sure the project compiles.
    - Run pre-commit checks.
