package com.rork.soundbar.data

import android.content.Context
import android.os.Handler
import android.os.Looper
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
import kotlinx.serialization.json.Json
import java.security.SecureRandom

/**
 * The guest exchange. While it runs, the app quietly advertises the user's
 * signature playlist over Nearby Connections and collects the same from anyone
 * nearby. Only the playlist bytes cross the air — the on-air name is a random
 * anonymous token, connections are auto-accepted, and no account, id, or other
 * identifying information is ever transmitted.
 */
class GuestExchange(
    context: Context,
    private val onGuestBlend: (Blend) -> Unit,
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
        if (!isAvailable) {
            onStateChanged(State.UNAVAILABLE)
            return
        }
        signature = initial
        try {
            connections.startAdvertising(localName, SERVICE_ID, connectionLifecycle, adOptions)
            connections.startDiscovery(SERVICE_ID, discoveryCallback, discoveryOptions)
            onStateChanged(State.RUNNING)
        } catch (e: SecurityException) {
            onStateChanged(State.UNAVAILABLE)
        }
    }

    /** Swaps the playlist being shared and pushes it to every connected guest. */
    fun updateSignature(blend: Blend?) {
        signature = blend
        if (blend != null) connected.forEach { sendTo(it, blend) }
    }

    fun stop() {
        try {
            connections.stopAdvertising()
            connections.stopDiscovery()
            connections.stopAllEndpoints()
        } catch (e: SecurityException) {
            // Permissions were revoked mid-flight; the endpoints are gone regardless.
        }
        connected.clear()
        onStateChanged(State.STOPPED)
    }

    private fun sendTo(endpointId: String, blend: Blend) {
        try {
            val bytes = json.encodeToString(Blend.serializer(), blend).toByteArray(Charsets.UTF_8)
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
                } catch (e: SecurityException) {
                    // Missing runtime permissions; sharing is effectively down.
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
            val blend = try {
                val decoded = json.decodeFromString(Blend.serializer(), String(bytes, Charsets.UTF_8))
                if (decoded.tracks.isEmpty()) null else decoded
            } catch (e: Exception) {
                null
            }
            blend?.let { main.post { onGuestBlend(it) } }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) = Unit
    }

    private companion object {
        const val SERVICE_ID = "com.rork.soundbar"

        private val random = SecureRandom()

        fun randomToken(length: Int): String {
            val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
            return buildString {
                repeat(length) { append(chars[random.nextInt(chars.length)]) }
            }
        }
    }
}
