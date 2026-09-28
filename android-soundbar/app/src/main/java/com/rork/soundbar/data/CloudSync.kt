package com.rork.soundbar.data

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

/** What the cloud holds for one account — a null state means the shelf isn't there yet. */
data class CloudSnapshot(
    val state: ShelfState?,
    val updatedAt: Long
)

/**
 * Moves the shelf between this device and the account's cloud copy. Every
 * write carries a millisecond stamp and the newest one wins, so a phone that
 * was offline for a week hands its story over cleanly. Requests ride the
 * signed-in session's bearer token; an expired one refreshes once and retries.
 */
class CloudSync(private val auth: AuthManager) {

    private val http = HttpClient(Android)
    private val json = Json { ignoreUnknownKeys = true }

    /** Reads the account's snapshot, or one with a null state when the cloud is empty. */
    suspend fun pull(): CloudSnapshot? = withContext(Dispatchers.IO) {
        try {
            val response = authorized { token ->
                http.get("${AuthConfig.FUNCTIONS_URL}/shelf") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
            }
            if (!response.status.isSuccess()) return@withContext null
            val payload = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            if (payload["found"]?.jsonPrimitive?.booleanOrNull != true) {
                return@withContext CloudSnapshot(state = null, updatedAt = 0L)
            }
            val raw = payload["state"]?.jsonPrimitive?.contentOrNull
                ?: return@withContext null
            CloudSnapshot(
                state = json.decodeFromString<ShelfState>(raw),
                updatedAt = payload["updatedAt"]?.jsonPrimitive?.longOrNull ?: 0L
            )
        } catch (error: Exception) {
            Log.w(TAG, "Cloud pull failed")
            null
        }
    }

    /** Offers the local shelf to the cloud; the backend keeps whichever write is newest. */
    suspend fun push(state: ShelfState): Boolean = withContext(Dispatchers.IO) {
        try {
            val body = buildJsonObject {
                put("state", json.encodeToString(state))
                put("updatedAt", state.updatedAt)
            }.toString()
            val response = authorized { token ->
                http.put("${AuthConfig.FUNCTIONS_URL}/shelf") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
            }
            response.status.isSuccess()
        } catch (error: Exception) {
            Log.w(TAG, "Cloud push failed")
            false
        }
    }

    /** Runs the request with the session token, refreshing once if it expired. */
    private suspend fun authorized(block: suspend (String) -> HttpResponse): HttpResponse {
        var token = auth.accessToken() ?: throw IllegalStateException("signed out")
        var response = block(token)
        if (response.status == HttpStatusCode.Unauthorized && auth.refreshAccessToken()) {
            token = auth.accessToken() ?: token
            response = block(token)
        }
        return response
    }

    private companion object {
        const val TAG = "CloudSync"
    }
}
