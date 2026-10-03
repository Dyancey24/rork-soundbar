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
    val points: Long = 0L,
    /** The player's avatar mark as a "glyph:palette" code, when they opted in. */
    val avatar: String? = null
)

/** The signed-in player's own entry, including the code friends use to add them. */
@Serializable
data class LeaderboardProfile(
    val id: String,
    val name: String,
    val code: String,
    val points: Long = 0L,
    val avatar: String? = null
)

/**
 * The whole board: the reader's own entry, confirmed friends (both sides have
 * added each other), invites waiting on the reader's nod, and invites the
 * reader has sent that are still waiting on the other side.
 */
@Serializable
data class LeaderboardData(
    val me: LeaderboardProfile? = null,
    val friends: List<LeaderboardPlayer> = emptyList(),
    val incoming: List<LeaderboardPlayer> = emptyList(),
    val outgoing: List<LeaderboardPlayer> = emptyList()
)

/** What came of offering a friend code. */
sealed interface AddFriendResult {
    /** The other side had already offered — glasses clinked, friends now. */
    data class Confirmed(val player: LeaderboardPlayer) : AddFriendResult
    /** The invite is with them; the board is shared once they add back. */
    data class Invited(val player: LeaderboardPlayer) : AddFriendResult
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
    suspend fun ensureProfile(name: String, avatar: Avatar? = null): LeaderboardProfile? = withContext(Dispatchers.IO) {
        try {
            val body = buildJsonObject {
                put("name", name)
                put("avatar", avatar?.code)
            }.toString()
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
    suspend fun pushScore(points: Long, name: String, avatar: Avatar? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            val body = buildJsonObject {
                put("points", points)
                put("name", name)
                put("avatar", avatar?.code)
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

    /**
     * Offers the player behind a friend code an invite. Friendship only
     * exists once they offer one back — the cloud does the clinking.
     */
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
                    val player = LeaderboardPlayer(
                        id = friend.str("id") ?: return@withContext AddFriendResult.Failed,
                        name = friend.str("name") ?: "Guest",
                        points = friend.str("points")?.toLongOrNull() ?: 0L,
                        avatar = friend.str("avatar")
                    )
                    if (payload.str("status") == "confirmed") {
                        AddFriendResult.Confirmed(player)
                    } else {
                        AddFriendResult.Invited(player)
                    }
                }
            }
        } catch (error: Exception) {
            Log.w(TAG, "Add friend failed")
            AddFriendResult.Failed
        }
    }

    /**
     * Clinks back: accepts the invite from this player, confirming the
     * friendship for both sides. Returns the newly confirmed friend.
     */
    suspend fun acceptInvite(playerId: String): LeaderboardPlayer? = withContext(Dispatchers.IO) {
        try {
            val body = buildJsonObject { put("id", playerId) }.toString()
            val response = authorized { token ->
                http.post("${AuthConfig.FUNCTIONS_URL}/leaderboard/friends/accept") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
            }
            if (!response.status.isSuccess()) return@withContext null
            val payload = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            val friend = payload["friend"]?.jsonObject ?: return@withContext null
            LeaderboardPlayer(
                id = friend.str("id") ?: return@withContext null,
                name = friend.str("name") ?: "Guest",
                points = friend.str("points")?.toLongOrNull() ?: 0L,
                avatar = friend.str("avatar")
            )
        } catch (error: Exception) {
            Log.w(TAG, "Accept invite failed")
            null
        }
    }

    /** Passes on an incoming invite — or takes back one this reader sent. */
    suspend fun declineInvite(playerId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = authorized { token ->
                http.delete("${AuthConfig.FUNCTIONS_URL}/leaderboard/friends/requests") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    parameter("id", playerId)
                }
            }
            response.status.isSuccess()
        } catch (error: Exception) {
            Log.w(TAG, "Decline invite failed")
            false
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
