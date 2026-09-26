package com.rork.soundbar.data

import android.content.Context
import android.util.Log
import kotlinx.serialization.json.Json

/** Persists the user's shelf to SharedPreferences as JSON. */
class ShelfRepository(context: Context) {

    private val prefs = context.getSharedPreferences("soundbar_shelf", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun load(): ShelfState {
        val raw = prefs.getString(KEY_STATE, null) ?: return ShelfState()
        return try {
            json.decodeFromString<ShelfState>(raw)
        } catch (error: Exception) {
            Log.w(TAG, "Shelf state could not be read, starting a fresh shelf")
            ShelfState()
        }
    }

    fun save(state: ShelfState) {
        try {
            prefs.edit().putString(KEY_STATE, json.encodeToString(state)).apply()
        } catch (error: Exception) {
            Log.w(TAG, "Shelf state could not be saved")
        }
    }

    private companion object {
        const val TAG = "ShelfRepository"
        const val KEY_STATE = "state_v1"
    }
}
