package com.rork.soundbar.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.SecureRandom

/**
 * What travels over the air on the pass: the playlist itself, plus the
 * sender's name and mark — but only when they've opted into a public profile.
 * An opted-out pour sends the playlist with both identity fields null.
 */
@Serializable
private data class SharedPour(
    val blend: Blend,
    val senderName: String? = null,
    val senderAvatar: Avatar? = null
)

/**
 * The guest exchange. While it runs, the app quietly advertises the user's
 * signature playlist over Nearby Connections and collects the same from anyone
 * nearby. The on-air name is a random anonymous token and connections are
 * auto-accepted; only the playlist crosses the air unless the sender has
 * opted into a public profile, in which case their chosen username and avatar
 * mark ride along too — never an account, id, or anything else.
 */
class GuestExchange(
    context: Context,
    private val onGuestBlend: (Blend, String?, Avatar?) -> Unit,
    private val onStateChanged: (State) -> Unit
) {

    enum class State { RUNNING, STOPPED, UNAVAILABLE }

    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val connections = Nearby.getConnectionsClient(appContext)
    private val json = Json { ignoreUnknownKeys = true }

    /** Anonymous on-air name, regenerated every session. No user identity. */
    private val localName = "guest-" + randomToken(4)

    private var signature: Blend? = null

    /** The public identity riding the pass; null unless the reader opted in. */
    private var senderName: String? = null
    private var senderAvatar: Avatar? = null
    private val connected = mutableSetOf<String>()

    private val adOptions = AdvertisingOptions.Builder()
        .setStrategy(Strategy.P2P_CLUSTER)
        .build()
    private val discoveryOptions = DiscoveryOptions.Builder()
        .setStrategy(Strategy.P2P_CLUSTER)
        .build()

    val isAvailable: Boolean
        get() = GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(appContext) == ConnectionResult.SUCCESS

    /** Starts advertising and listening. Reports UNAVAILABLE when the device can't. */
    fun start(initial: Blend?) {
        if (!isAvailable || missingPermissions(appContext).isNotEmpty()) {
            onStateChanged(State.UNAVAILABLE)
            return
        }
        signature = initial
        try {
            connections.startAdvertising(localName, SERVICE_ID, connectionLifecycle, adOptions)
            connections.startDiscovery(SERVICE_ID, discoveryCallback, discoveryOptions)
            onStateChanged(State.RUNNING)
        } catch (e: Exception) {
            // No usable radio (Bluetooth/Wi-Fi off or missing) — sharing can't run.
            onStateChanged(State.UNAVAILABLE)
        }
    }

    /** Swaps the playlist being shared and pushes it to every connected guest. */
    fun updateSignature(blend: Blend?) {
        signature = blend
        if (blend != null) connected.forEach { sendTo(it, blend) }
    }

    /**
     * Sets the identity that rides the pass — the public username and mark
     * when the reader has opted in, or null to travel anonymous. Connected
     * guests are refreshed so a mid-session opt-in updates what they see.
     */
    fun setSender(name: String?, avatar: Avatar?) {
        senderName = name?.takeIf { it.isNotBlank() }
        senderAvatar = if (senderName != null) avatar else null
        signature?.let { blend -> connected.forEach { sendTo(it, blend) } }
    }

    fun stop() {
        try {
            connections.stopAdvertising()
            connections.stopDiscovery()
            connections.stopAllEndpoints()
        } catch (e: Exception) {
            // Permissions were revoked mid-flight; the endpoints are gone regardless.
        }
        connected.clear()
        onStateChanged(State.STOPPED)
    }

    private fun sendTo(endpointId: String, blend: Blend) {
        try {
            val pour = SharedPour(
                blend = blend,
                senderName = senderName,
                senderAvatar = senderAvatar
            )
            val bytes = json.encodeToString(SharedPour.serializer(), pour).toByteArray(Charsets.UTF_8)
            connections.sendPayload(endpointId, Payload.fromBytes(bytes))
        } catch (e: Exception) {
            connected.remove(endpointId)
        }
    }

    private val connectionLifecycle = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            main.post { connections.acceptConnection(endpointId, payloadCallback) }
        }

        override fun onConnectionResult(endpointId: String, connectionResolution: ConnectionResolution) {
            main.post {
                if (connectionResolution.status.isSuccess) {
                    connected += endpointId
                    // Greet the new guest with our signature right away.
                    signature?.let { sendTo(endpointId, it) }
                } else {
                    connected -= endpointId
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            main.post { connected -= endpointId }
        }
    }

    private val discoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            main.post {
                try {
                    connections.requestConnection(localName, endpointId, connectionLifecycle)
                } catch (e: Exception) {
                    // Permissions revoked or radio gone mid-flight; sharing is effectively down.
                }
            }
        }

        override fun onEndpointLost(endpointId: String) {
            main.post { connected -= endpointId }
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type != Payload.Type.BYTES) return
            val bytes = payload.asBytes() ?: return
            val text = String(bytes, Charsets.UTF_8)
            val received = try {
                // The current wire format: a pour with an optional sender.
                val pour = json.decodeFromString(SharedPour.serializer(), text)
                if (pour.blend.tracks.isEmpty()) {
                    null
                } else {
                    Triple(pour.blend, pour.senderName, pour.senderAvatar)
                }
            } catch (e: Exception) {
                try {
                    // An older pour: a bare playlist from an anonymous guest.
                    val blend = json.decodeFromString(Blend.serializer(), text)
                    if (blend.tracks.isEmpty()) null else Triple(blend, null, null)
                } catch (e2: Exception) {
                    null
                }
            }
            received?.let { (blend, name, avatar) -> main.post { onGuestBlend(blend, name, avatar) } }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) = Unit
    }

    companion object {
        private const val SERVICE_ID = "com.rork.soundbar"

        private val random = SecureRandom()

        /** The radio permissions the exchange needs on this OS version. */
        fun requiredPermissions(): List<String> = if (Build.VERSION.SDK_INT >= 31) {
            buildList {
                add(Manifest.permission.BLUETOOTH_CONNECT)
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_ADVERTISE)
                if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
        } else if (Build.VERSION.SDK_INT >= 29) {
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            listOf(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        /** Which of the required permissions the app is still missing. */
        fun missingPermissions(context: Context): List<String> = requiredPermissions().filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }

        fun randomToken(length: Int): String {
            val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
            return buildString {
                repeat(length) { append(chars[random.nextInt(chars.length)]) }
            }
        }
    }
}
