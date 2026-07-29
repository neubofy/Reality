import re

with open('website/src/app/tapashya/page.tsx', 'r') as f:
    content = f.read()

content = content.replace("getRedirectResult,", "")

old_auth_effect = """
      // Check for redirect result from Firebase Auth
      getRedirectResult(auth).then(async (result) => {
          if (result) {
              const credential = GoogleAuthProvider.credentialFromResult(result);
              if (credential && credential.accessToken) {
                  // Send the Google Access Token to our backend to be stored securely in an HTTP-only cookie
                  await fetch('/api/auth/session', {
                      method: 'POST',
                      headers: { 'Content-Type': 'application/json' },
                      body: JSON.stringify({ googleAccessToken: credential.accessToken })
                  });
              }
          }
          // After handling redirect (or if no redirect), verify the session cookie
          await fetchTodayEvents();
      }).catch((error) => {
          console.error("Google Sign-In redirect failed", error);
          fetchTodayEvents();
      });
"""

new_auth_effect = """
      fetchTodayEvents();
"""

content = content.replace(old_auth_effect, new_auth_effect)

old_connect = """
  const handleGoogleConnect = async () => {
      try {
          await signInWithPopup(auth, googleProvider);
      } catch (error) {
          console.error("Google Sign-In redirect initiation failed", error);
      }
  };
"""

new_connect = """
  const handleGoogleConnect = async () => {
      try {
          const result = await signInWithPopup(auth, googleProvider);
          const credential = GoogleAuthProvider.credentialFromResult(result);
          if (credential && credential.accessToken) {
              await fetch('/api/auth/session', {
                  method: 'POST',
                  headers: { 'Content-Type': 'application/json' },
                  body: JSON.stringify({ googleAccessToken: credential.accessToken })
              });
          }
          await fetchTodayEvents();
      } catch (error) {
          console.error("Google Sign-In initiation failed", error);
      }
  };
"""

content = content.replace(old_connect, new_connect)

with open('website/src/app/tapashya/page.tsx', 'w') as f:
    f.write(content)
