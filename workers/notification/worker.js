// ============================================================
// Reality Notification Bridge — Cloudflare Worker
// Routes:
//   POST /api/register-fcm-token    → App registers device FCM token
//   POST /api/unregister-fcm-token  → App unregisters device FCM token
//   POST /api/update-fcm-token      → App updates FCM token (after app update)
//   POST /webhook/calendar          → Google Calendar change webhook
//   POST /api/send-notification     → Send custom push notification (admin)
// ============================================================

const CORS_ORIGIN = "https://reality.neubofy.in";

export default {
  async fetch(request, env) {
    const url = new URL(request.url);

    if (request.method === "OPTIONS") {
      return new Response(null, {
        headers: {
          "Access-Control-Allow-Origin": CORS_ORIGIN,
          "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
          "Access-Control-Allow-Headers": "Content-Type",
        },
      });
    }

    // Root health check
    if (url.pathname === "/") {
      return new Response("Reality Notification Bridge is running.", { status: 200 });
    }

    // ============================================================
    // ROUTE: App registers FCM token for the authenticated user
    // POST /api/register-fcm-token
    // Body: { userId, backupPassword, fcmToken }
    // Auth: backupPassword = HMAC(userId, APP_SECRET_PEPPER) — static, never rotates
    // ============================================================
    if (url.pathname === "/api/register-fcm-token" && request.method === "POST") {
      let body = {};
      try {
        body = await request.json();
      } catch {
        return jsonError("Invalid JSON body", 400);
      }

      const { userId, backupPassword, fcmToken } = body;

      if (!userId || !backupPassword || !fcmToken) {
        return jsonError("Missing required fields: userId, backupPassword, fcmToken", 400);
      }

      // Verify credentials using static backupPassword (HMAC of userId)
      const verified = await verifyBackupPassword(userId, backupPassword, env);
      if (!verified) {
        return jsonError("Invalid credentials", 401);
      }

      if (!env.DB) return jsonError("DB not configured", 500);

      // Store FCM token
      try {
        await env.DB.prepare(
          'UPDATE "Reality Elite members management" SET fcmToken = ? WHERE userId = ?'
        ).bind(fcmToken, userId).run();
      } catch (e) {
        return jsonError("Failed to store FCM token: " + e.message, 500);
      }

      console.log(`FCM token registered for user: ${userId.substring(0, 8)}...`);
      return jsonResponse({ success: true, message: "FCM token registered successfully" });
    }

    // ============================================================
    // ROUTE: App unregisters FCM token (logout / account delete)
    // POST /api/unregister-fcm-token
    // Body: { userId, backupPassword }
    // ============================================================
    if (url.pathname === "/api/unregister-fcm-token" && request.method === "POST") {
      let body = {};
      try {
        body = await request.json();
      } catch {
        return jsonError("Invalid JSON body", 400);
      }

      const { userId, backupPassword } = body;

      if (!userId || !backupPassword) {
        return jsonError("Missing required fields: userId, backupPassword", 400);
      }

      const verified = await verifyBackupPassword(userId, backupPassword, env);
      if (!verified) {
        return jsonError("Invalid credentials", 401);
      }

      if (!env.DB) return jsonError("DB not configured", 500);

      try {
        await env.DB.prepare(
          'UPDATE "Reality Elite members management" SET fcmToken = NULL WHERE userId = ?'
        ).bind(userId).run();
      } catch (e) {
        return jsonError("Failed to clear FCM token: " + e.message, 500);
      }

      console.log(`FCM token unregistered for user: ${userId.substring(0, 8)}...`);
      return jsonResponse({ success: true, message: "FCM token unregistered successfully" });
    }

    // ============================================================
    // ROUTE: App updates FCM token (after app update / token rotation)
    // POST /api/update-fcm-token
    // Body: { userId, backupPassword, oldFcmToken, newFcmToken }
    // ============================================================
    if (url.pathname === "/api/update-fcm-token" && request.method === "POST") {
      let body = {};
      try {
        body = await request.json();
      } catch {
        return jsonError("Invalid JSON body", 400);
      }

      const { userId, backupPassword, oldFcmToken, newFcmToken } = body;

      if (!userId || !backupPassword || !oldFcmToken || !newFcmToken) {
        return jsonError("Missing required fields: userId, backupPassword, oldFcmToken, newFcmToken", 400);
      }

      const verified = await verifyBackupPassword(userId, backupPassword, env);
      if (!verified) {
        return jsonError("Invalid credentials", 401);
      }

      if (!env.DB) return jsonError("DB not configured", 500);

      try {
        // Safety: only update if the old token matches what's stored
        const result = await env.DB.prepare(
          'UPDATE "Reality Elite members management" SET fcmToken = ? WHERE userId = ? AND fcmToken = ?'
        ).bind(newFcmToken, userId, oldFcmToken).run();

        if (result.meta.changes === 0) {
          // Old token didn't match — maybe already updated or user re-registered
          // Fall back to unconditional update since the user is authenticated
          await env.DB.prepare(
            'UPDATE "Reality Elite members management" SET fcmToken = ? WHERE userId = ?'
          ).bind(newFcmToken, userId).run();
        }
      } catch (e) {
        return jsonError("Failed to update FCM token: " + e.message, 500);
      }

      console.log(`FCM token updated for user: ${userId.substring(0, 8)}...`);
      return jsonResponse({ success: true, message: "FCM token updated successfully" });
    }

    // ============================================================
    // ROUTE: Send Custom Notification (ADMIN ONLY — UNTOUCHED)
    // POST /api/send-notification
    // Body: { notificationSecret, userId, title, message }
    // ============================================================
    if (url.pathname === "/api/send-notification" && request.method === "POST") {
      let body = {};
      try {
        body = await request.json();
      } catch {
        return jsonError("Invalid JSON body", 400);
      }

      const { notificationSecret, userId, title, message } = body;

      if (!notificationSecret || !userId || !title || !message) {
        return jsonError("Missing required fields: notificationSecret, userId, title, message", 400);
      }

      if (!env.NOTIFICATION_SECRET || notificationSecret !== env.NOTIFICATION_SECRET) {
        return jsonError("Unauthorized: Invalid notificationSecret", 401);
      }

      if (!env.DB) return jsonError("DB not configured", 500);

      let userRow;
      try {
        userRow = await env.DB.prepare(
          'SELECT fcmToken FROM "Reality Elite members management" WHERE userId = ?'
        ).bind(userId).first();
      } catch (e) {
        return jsonError("DB lookup error: " + e.message, 500);
      }

      if (!userRow || !userRow.fcmToken) {
        return jsonError("No FCM token found for this user", 404);
      }

      try {
        await sendFcmPush(userRow.fcmToken, { title: String(title), message: String(message) }, env);
        console.log(`Custom notification sent for userId: ${userId.substring(0, 8)}...`);
      } catch (e) {
        return jsonError("FCM send failed: " + e.message, 500);
      }

      return jsonResponse({ success: true, message: "Custom notification sent successfully" });
    }

    // ============================================================
    // ROUTE: Google Calendar Webhook
    // POST /webhook/calendar
    // Google sends x-goog-channel-token = "reality-{userId}-{backupPassword}"
    // Secured via cryptographic verification of backupPassword
    // ============================================================
    if (url.pathname === "/webhook/calendar" && request.method === "POST") {
      const resourceState = request.headers.get("x-goog-resource-state");
      const channelToken = request.headers.get("x-goog-channel-token");
      const channelId = request.headers.get("x-goog-channel-id");

      console.log(`Calendar webhook received. State: ${resourceState}, Channel: ${channelId}`);

      // Google sends a "sync" ping when webhook is first registered — just acknowledge
      if (resourceState === "sync") {
        return new Response("Sync acknowledged", { status: 200 });
      }

      // Validate webhook token exists
      if (!channelToken) {
        return new Response("Missing channel token", { status: 400 });
      }

      // Channel token format: "reality-{userId}-{backupPassword}"
      if (!channelToken.startsWith("reality-")) {
        console.warn(`[SECURITY] Webhook rejected: Invalid token format. Returning 410 Gone to tell Google to stop sending.`);
        return new Response("Invalid token format", { status: 410 });
      }

      // Parse userId and backupPassword from token
      const tokenWithoutPrefix = channelToken.substring("reality-".length);
      const separatorIndex = tokenWithoutPrefix.indexOf("-");
      if (separatorIndex === -1) {
        return new Response("Invalid token format: missing separator", { status: 403 });
      }

      const userId = tokenWithoutPrefix.substring(0, separatorIndex);
      const receivedBackupPassword = tokenWithoutPrefix.substring(separatorIndex + 1);

      if (!userId || !receivedBackupPassword) {
        return new Response("Could not extract credentials from token", { status: 410 });
      }

      // Cryptographic verification: regenerate expected backupPassword and compare
      const verified = await verifyBackupPassword(userId, receivedBackupPassword, env);
      if (!verified) {
        console.warn(`[SECURITY] Webhook rejected: Invalid backupPassword for userId: ${userId.substring(0, 8)}... Returning 410 Gone to tell Google to stop sending.`);
        return new Response("Unauthorized: Invalid credentials", { status: 410 });
      }

      // Look up user's FCM token from D1
      if (!env.DB) {
        console.error("DB binding missing");
        return new Response("DB not configured", { status: 500 });
      }

      let userRow;
      try {
        userRow = await env.DB.prepare(
          'SELECT fcmToken FROM "Reality Elite members management" WHERE userId = ?'
        ).bind(userId).first();
      } catch (e) {
        console.error("DB lookup error:", e.message);
        return new Response("DB error", { status: 500 });
      }

      if (!userRow || !userRow.fcmToken) {
        console.log(`No FCM token found for userId: ${userId.substring(0, 8)}... — skipping`);
        return new Response("No FCM token for user", { status: 200 }); // 200 so Google doesn't retry
      }

      // Send silent FCM push notification
      try {
        await sendFcmPush(userRow.fcmToken, { action: "SYNC_CALENDAR" }, env);
        console.log(`FCM calendar sync push sent for userId: ${userId.substring(0, 8)}...`);
      } catch (e) {
        console.error("FCM send failed:", e.message);
        return new Response("FCM error: " + e.message, { status: 500 });
      }

      return new Response("Notification sent", { status: 200 });
    }

    return new Response("Not Found", { status: 404 });
  }
};

// ============================================================
// HELPER: Verify backupPassword (static HMAC of userId)
// Same algorithm as identity worker's generateBackupPassword()
// ============================================================
async function generateBackupPassword(userId, secretPepper) {
  if (!userId || !secretPepper) return "";
  const encoder = new TextEncoder();
  const secretKeyData = encoder.encode(secretPepper);
  const cryptoKey = await crypto.subtle.importKey(
    "raw", secretKeyData, { name: "HMAC", hash: "SHA-256" }, false, ["sign"]
  );
  const msg = `${userId}`;
  const signature = await crypto.subtle.sign("HMAC", cryptoKey, encoder.encode(msg));
  return Array.from(new Uint8Array(signature))
    .map(b => b.toString(16).padStart(2, '0'))
    .join('').substring(0, 32);
}

async function verifyBackupPassword(userId, backupPassword, env) {
  if (!userId || !backupPassword || !env.APP_SECRET_PEPPER) return false;
  const expected = await generateBackupPassword(userId, env.APP_SECRET_PEPPER);
  const encoder = new TextEncoder();
  const a = encoder.encode(backupPassword);
  const b = encoder.encode(expected);

  let matched = false;
  if (a.byteLength === b.byteLength) {
    matched = crypto.subtle.timingSafeEqual(a, b);
  }

  if (!matched) {
    console.warn(`[SECURITY] Unauthorized access detected: backupPassword mismatch for userId: ${userId}. Attempts to bypass verification may result in account termination and legal action.`);
  }
  return matched;
}

// ============================================================
// HELPER: Send silent FCM push using Firebase HTTP v1 API
// ============================================================
async function sendFcmPush(fcmToken, dataPayload, env) {
  if (!env.FIREBASE_SERVICE_ACCOUNT) {
    throw new Error("FIREBASE_SERVICE_ACCOUNT secret is not configured in the Cloudflare Dashboard settings for this Worker.");
  }
  const accessToken = await getFirebaseAccessToken(env.FIREBASE_SERVICE_ACCOUNT);
  const serviceAccount = JSON.parse(env.FIREBASE_SERVICE_ACCOUNT);
  const projectId = serviceAccount.project_id;

  const fcmUrl = `https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`;

  const payload = {
    message: {
      token: fcmToken,
      data: dataPayload,
      android: {
        priority: "high"
      }
    }
  };

  const response = await fetch(fcmUrl, {
    method: "POST",
    headers: {
      "Authorization": `Bearer ${accessToken}`,
      "Content-Type": "application/json"
    },
    body: JSON.stringify(payload)
  });

  const resJson = await response.json();
  if (!response.ok) {
    throw new Error(`FCM error: ${JSON.stringify(resJson)}`);
  }
}

// ============================================================
// HELPER: Get Firebase OAuth2 access token from service account
// Uses Web Crypto RS256 JWT — no external libraries needed
// ============================================================
async function getFirebaseAccessToken(serviceAccountJson) {
  const serviceAccount = JSON.parse(serviceAccountJson);
  const privateKeyPem = serviceAccount.private_key;

  const pemHeader = "-----BEGIN PRIVATE KEY-----";
  const pemFooter = "-----END PRIVATE KEY-----";
  const pemContents = privateKeyPem
    .replace(pemHeader, "")
    .replace(pemFooter, "")
    .replace(/\s/g, "");

  const binaryDer = base64ToArrayBuffer(pemContents);

  const cryptoKey = await crypto.subtle.importKey(
    "pkcs8",
    binaryDer,
    { name: "RSASSA-PKCS1-v1_5", hash: { name: "SHA-256" } },
    false,
    ["sign"]
  );

  const now = Math.floor(Date.now() / 1000);
  const header = { alg: "RS256", typ: "JWT" };
  const claim = {
    iss: serviceAccount.client_email,
    scope: "https://www.googleapis.com/auth/firebase.messaging",
    aud: serviceAccount.token_uri,
    exp: now + 3600,
    iat: now
  };

  const encodedHeader = base64UrlEncode(JSON.stringify(header));
  const encodedClaim = base64UrlEncode(JSON.stringify(claim));
  const tokenInput = `${encodedHeader}.${encodedClaim}`;

  const encoder = new TextEncoder();
  const signatureBuffer = await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5",
    cryptoKey,
    encoder.encode(tokenInput)
  );

  const jwt = `${tokenInput}.${base64UrlEncodeFromBuffer(signatureBuffer)}`;

  const tokenResponse = await fetch(serviceAccount.token_uri, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: `grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=${jwt}`
  });

  const tokenData = await tokenResponse.json();
  if (tokenData.error) {
    throw new Error(`Token exchange failed: ${tokenData.error_description || tokenData.error}`);
  }
  return tokenData.access_token;
}

// ============================================================
// HELPERS: Base64 utilities
// ============================================================
function base64UrlEncode(str) {
  return base64UrlEncodeFromBuffer(new TextEncoder().encode(str));
}

function base64UrlEncodeFromBuffer(buffer) {
  const bytes = new Uint8Array(buffer);
  let binary = "";
  for (let i = 0; i < bytes.byteLength; i++) {
    binary += String.fromCharCode(bytes[i]);
  }
  return btoa(binary).replace(/=/g, "").replace(/\+/g, "-").replace(/\//g, "_");
}

function base64ToArrayBuffer(b64) {
  const binaryString = atob(b64);
  const bytes = new Uint8Array(binaryString.length);
  for (let i = 0; i < binaryString.length; i++) {
    bytes[i] = binaryString.charCodeAt(i);
  }
  return bytes.buffer;
}

// ============================================================
// HELPERS: Response builders
// ============================================================
function jsonResponse(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": CORS_ORIGIN }
  });
}

function jsonError(message, status = 400) {
  return new Response(JSON.stringify({ error: message }), {
    status,
    headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": CORS_ORIGIN }
  });
}
