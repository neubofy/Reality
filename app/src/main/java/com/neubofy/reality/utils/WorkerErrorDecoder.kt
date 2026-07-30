package com.neubofy.reality.utils

import org.json.JSONObject
import java.io.InputStream
import java.net.HttpURLConnection

/**
 * Universal error decoder for Cloudflare Worker & Google Auth HTTP responses.
 * Classifies errors to prevent user from ever getting stuck on expired tokens or silent network failures.
 */
object WorkerErrorDecoder {

    enum class AuthErrorType {
        EXPIRED_ID_TOKEN,
        EXPIRED_ACCESS_TOKEN,
        EXPIRED_REFRESH_TOKEN,
        UNAUTHORIZED,
        SUBSCRIPTION_EXPIRED,
        NETWORK_ERROR,
        UNKNOWN_ERROR
    }

    data class DecodedError(
        val responseCode: Int,
        val errorMsg: String,
        val details: String,
        val type: AuthErrorType
    )

    /**
     * Reads error stream or input stream from a HttpURLConnection and classifies the error.
     */
    fun decodeConnectionError(conn: HttpURLConnection): DecodedError {
        val code = try { conn.responseCode } catch (e: Exception) { -1 }
        val stream: InputStream? = try {
            conn.errorStream ?: conn.inputStream
        } catch (e: Exception) {
            null
        }

        val rawText = stream?.bufferedReader()?.use { it.readText() } ?: ""
        var errorMsg = ""
        var details = ""

        if (rawText.isNotEmpty()) {
            try {
                val json = JSONObject(rawText)
                errorMsg = json.optString("error").ifEmpty {
                    json.optString("message").ifEmpty {
                        json.optString("status")
                    }
                }
                details = json.optString("details").ifEmpty {
                    json.optString("error_description")
                }
            } catch (_: Exception) {
                errorMsg = rawText
            }
        }

        if (errorMsg.isEmpty()) {
            errorMsg = "HTTP $code Error"
        }

        val combined = "$errorMsg $details $rawText".lowercase()

        val type = when {
            code == 401 || (code == 400 && (combined.contains("idtoken") || combined.contains("invalid idtoken") || combined.contains("token expired") || combined.contains("failed to verify idtoken"))) -> {
                AuthErrorType.EXPIRED_ID_TOKEN
            }
            code == 401 && (combined.contains("access_token") || combined.contains("bearer") || combined.contains("invalid_credentials") || combined.contains("invalid credentials")) -> {
                AuthErrorType.EXPIRED_ACCESS_TOKEN
            }
            code == 401 && combined.contains("invalid_grant") -> {
                AuthErrorType.EXPIRED_REFRESH_TOKEN
            }
            code == 403 && (combined.contains("expired") || combined.contains("subscription") || combined.contains("inactive")) -> {
                AuthErrorType.SUBSCRIPTION_EXPIRED
            }
            code == 401 -> {
                AuthErrorType.UNAUTHORIZED
            }
            code == -1 || combined.contains("unable to resolve host") || combined.contains("timeout") -> {
                AuthErrorType.NETWORK_ERROR
            }
            else -> AuthErrorType.UNKNOWN_ERROR
        }

        return DecodedError(
            responseCode = code,
            errorMsg = errorMsg,
            details = details,
            type = type
        )
    }
}
