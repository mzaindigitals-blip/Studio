package com.example.model

import android.util.Base64
import org.json.JSONObject

enum class ConnectionMode {
    DEMO,
    BUILT_IN_FIRESTORE,
    CUSTOM_RTDB,
    CUSTOM_FIRESTORE
}

enum class ConnectionStatus(val label: String) {
    FIREBASE_LIVE("FIREBASE LIVE"),
    DEMO_MODE("DEMO MODE"),
    CONNECTING("CONNECTING"),
    DISCONNECTED("DISCONNECTED")
}

data class CustomFirebaseConfig(
    val apiKey: String = "",
    val authDomain: String = "",
    val databaseURL: String = "",
    val projectId: String = "",
    val appId: String = ""
) {
    val hasDatabaseUrl: Boolean
        get() = databaseURL.trim().startsWith("http")

    val hasFirestoreConfig: Boolean
        get() = apiKey.isNotBlank() && projectId.isNotBlank() && appId.isNotBlank()

    val isValid: Boolean
        get() = hasDatabaseUrl || hasFirestoreConfig

    fun toPrettyJson(): String = JSONObject().apply {
        put("apiKey", apiKey)
        put("authDomain", authDomain)
        put("databaseURL", databaseURL)
        put("projectId", projectId)
        put("appId", appId)
    }.toString(2)

    fun toTeamJoinCode(): String {
        val compactJson = JSONObject().apply {
            if (databaseURL.isNotBlank()) put("databaseURL", databaseURL.trim().trimEnd('/'))
            if (projectId.isNotBlank()) put("projectId", projectId.trim())
            if (apiKey.isNotBlank()) put("apiKey", apiKey.trim())
            if (authDomain.isNotBlank()) put("authDomain", authDomain.trim())
            if (appId.isNotBlank()) put("appId", appId.trim())
        }.toString()
        val encoded = Base64.encodeToString(
            compactJson.toByteArray(Charsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
        return "DVX1:$encoded"
    }
}

/**
 * Parses:
 * 1) Team Join Code (`DVX1:...`)
 * 2) Direct Realtime Database URL (`https://...-rtdb.firebaseio.com`)
 * 3) Raw JSON or JavaScript `const firebaseConfig = { ... };` snippet
 */
fun parseFirebaseConfigInput(rawInput: String): Result<CustomFirebaseConfig> {
    val trimmed = rawInput.trim()
    if (trimmed.isEmpty()) {
        return Result.failure(IllegalArgumentException("Paste a Team Code, databaseURL, or firebaseConfig JSON."))
    }

    if (trimmed.startsWith("DVX1:", ignoreCase = true)) {
        return try {
            val payload = trimmed.substringAfter(":").trim()
            val decoded = String(
                Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING),
                Charsets.UTF_8
            )
            parseFirebaseConfigInput(decoded)
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Invalid Digital Vertex Team Code."))
        }
    }

    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        return Result.success(CustomFirebaseConfig(databaseURL = trimmed.trimEnd('/')))
    }

    return try {
        val normalized = extractObjectBody(trimmed)
        val json = JSONObject(normalized)
        val config = CustomFirebaseConfig(
            apiKey = json.optString("apiKey", "").trim(),
            authDomain = json.optString("authDomain", "").trim(),
            databaseURL = json.optString("databaseURL", "").trim().trimEnd('/'),
            projectId = json.optString("projectId", "").trim(),
            appId = json.optString("appId", "").trim()
        )
        if (!config.isValid) {
            Result.failure(
                IllegalArgumentException(
                    "Provide a databaseURL (for Realtime Database) or apiKey + projectId + appId (for Firestore)."
                )
            )
        } else {
            Result.success(config)
        }
    } catch (e: Exception) {
        Result.failure(
            IllegalArgumentException("Could not parse config: ${e.localizedMessage ?: "Invalid format"}")
        )
    }
}

private fun extractObjectBody(raw: String): String {
    val startIdx = raw.indexOf('{')
    val endIdx = raw.lastIndexOf('}')
    val slice = if (startIdx >= 0 && endIdx > startIdx) {
        raw.substring(startIdx, endIdx + 1)
    } else {
        raw
    }

    val quotedKeys = slice
        .replace(Regex("([\\{,]\\s*)([A-Za-z_][A-Za-z0-9_]*)\\s*:"), "$1\"$2\":")
        .replace('\'', '"')
        .replace(Regex(",\\s*\\}"), "}")
    return quotedKeys
}
