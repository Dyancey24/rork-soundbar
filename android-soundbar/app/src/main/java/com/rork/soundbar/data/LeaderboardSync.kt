package com.rork.soundbar.data

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

/** One player on the friends leaderboard. */
@Serializable
data class LeaderboardPlayer(
    val id: String,
    val name: String,
    val points: Long = 0L
)

/** The signed-in player's own entry, including the code friends use to add them. */
@Serializable
data class LeaderboardProfile(
    val id: String,
    val name: String,
    val code: String,
    val points: Long = 0L
)

/** The whole board: the reader's own entry plus the friends they added. */
@Serializable
data class LeaderboardData(
    val me: LeaderboardProfile? = null,
    val friends: List<LeaderboardPlayer> = emptyList()
)

/** What came of offering a friend code. */
sealed interface AddFriendResult {
    data class Added(val player: LeaderboardPlayer) : AddFriendResult
    data object UnknownCode : AddFriendResult
    data object OwnCode : AddFriendResult
    data object Failed : AddFriendResult
}

/**
 * Carries scores and friendships between this device and the house's cloud
 * leaderboard. Requests ride the signed-in session's bearer token, refreshing
 * once on expiry — the same etiquette as the shelf sync.
 */
class LeaderboardSync(private val auth: AuthManager) {

    private val http = HttpClient(Android)
    private val json = Json { ignoreUnknownKeys = true }

    /** Registers (or renames) the signed-in player and returns their friend code. */
    suspend fun ensureProfile(name: String): LeaderboardProfile? = withContext(Dispatchers.IO) {
        try {
            val body = buildJsonObject { put("name", name) }.toString()
            val response = authorized { token ->
                http.post("${AuthConfig.FUNCTIONS_URL}/leaderboard/profile") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
            }
            if (!response.status.isSuccess()) return@withContext null
            json.decodeFromString<LeaderboardProfile>(response.bodyAsText())
        } catch (error: Exception) {
            Log.w(TAG, "Leaderboard profile failed")
            null
        }
    }

    /** Reads the board: the reader's entry plus the friends they added. */
    suspend fun fetch(): LeaderboardData? = withContext(Dispatchers.IO) {
        try {
            val response = authorized { token ->
                http.get("${AuthConfig.FUNCTIONS_URL}/leaderboard") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
            }
            if (!response.status.isSuccess()) return@withContext null
            json.decodeFromString<LeaderboardData>(response.bodyAsText())
        } catch (error: Exception) {
            Log.w(TAG, "Leaderboard fetch failed")
            null
        }
    }

    /** Offers the current point total; the cloud only ever ratchets it upward. */
    suspend fun pushScore(points: Long, name: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val body = buildJsonObject {
                put("points", points)
                put("name", name)
            }.toString()
            val response = authorized { token ->
                http.put("${AuthConfig.FUNCTIONS_URL}/leaderboard/score") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
            }
            response.status.isSuccess()
        } catch (error: Exception) {
            Log.w(TAG, "Leaderboard score push failed")
            false
        }
    }

    /** Adds the player behind a friend code to this reader's board. */
    suspend fun addFriend(code: String): AddFriendResult = withContext(Dispatchers.IO) {
        try {
            val body = buildJsonObject { put("code", code.trim().uppercase()) }.toString()
            val response = authorized { token ->
                http.post("${AuthConfig.FUNCTIONS_URL}/leaderboard/friends") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
            }
            when {
                response.status == HttpStatusCode.NotFound -> AddFriendResult.UnknownCode
                response.status == HttpStatusCode.BadRequest -> AddFriendResult.OwnCode
                !response.status.isSuccess() -> AddFriendResult.Failed
                else -> {
                    val payload = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                    val friend = payload["friend"]?.jsonObject
                        ?: return@withContext AddFriendResult.Failed
                    AddFriendResult.Added(
                        LeaderboardPlayer(
                            id = friend.str("id") ?: return@withContext AddFriendResult.Failed,
                            name = friend.str("name") ?: "Guest",
                            points = friend.str("points")?.toLongOrNull() ?: 0L
                        )
                    )
                }
            }
        } catch (error: Exception) {
            Log.w(TAG, "Add friend failed")
            AddFriendResult.Failed
        }
    }

    /** Takes a friend back off this reader's board. */
    suspend fun removeFriend(friendId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = authorized { token ->
                http.delete("${AuthConfig.FUNCTIONS_URL}/leaderboard/friends") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    parameter("id", friendId)
                }
            }
            response.status.isSuccess()
        } catch (error: Exception) {
            Log.w(TAG, "Remove friend failed")
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

    private fun kotlinx.serialization.json.JsonObject.str(key: String): String? =
        (this[key] as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull

    private companion object {
        const val TAG = "LeaderboardSync"
    }
}
