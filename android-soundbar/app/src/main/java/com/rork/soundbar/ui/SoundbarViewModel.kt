package com.rork.soundbar.ui

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rork.soundbar.data.Blend
import com.rork.soundbar.data.BlendBar
import com.rork.soundbar.data.Garnish
import com.rork.soundbar.data.GarnishBar
import com.rork.soundbar.data.GuestCard
import com.rork.soundbar.data.GuestExchange
import com.rork.soundbar.data.Ingredient
import com.rork.soundbar.data.ShelfRepository
import com.rork.soundbar.data.ShelfState
import com.rork.soundbar.data.SignatureCraft
import com.rork.soundbar.data.StreamingPlatform
import com.rork.soundbar.ui.theme.Concept
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.TimeZone

/** What the simulated bar player is currently pouring through the speakers. */
data class Playback(
    val blendId: String,
    val trackIndex: Int,
    val positionSeconds: Int,
    val isPlaying: Boolean
)

/**
 * Shelf-wide taste numbers. The detail fields back the tappable stat cards on
 * the shelf: the genre tally counts every genre appearance across saved and
 * served blends (garnishes included), totalPours sums every play, and
 * mixDaysCount is how many distinct days something was mixed.
 */
data class TasteStats(
    val blendsMixed: Int,
    val genresExplored: Int,
    val hoursListened: Int,
    val streakDays: Int,
    val genreTally: List<Pair<String, Int>> = emptyList(),
    val totalPours: Int = 0,
    val mixDaysCount: Int = 0,
    /** Last seven day keys ending today, oldest first — true where something was mixed. */
    val recentMixDays: List<Boolean> = emptyList()
)

data class SoundbarUiState(
    val baseGenreId: String = "jazz",
    val concept: Concept = Concept.BAR,
    val mix: List<Ingredient> = emptyList(),
    val shelf: List<Blend> = emptyList(),
    val playCounts: Map<String, Int> = emptyMap(),
    val notes: Map<String, String> = emptyMap(),
    val lastPlayed: Map<String, Long> = emptyMap(),
    val signatureId: String? = null,
    val signatureBlend: Blend? = null,
    val listenedSeconds: Long = 0L,
    val servedBlends: List<Blend> = emptyList(),
    val guests: List<GuestCard> = emptyList(),
    val isSharing: Boolean = false,
    val isSharingAvailable: Boolean = true,
    val playback: Playback? = null,
    val isShaking: Boolean = false,
    val selectedPlatform: StreamingPlatform = StreamingPlatform.SPOTIFY,
    val stats: TasteStats = TasteStats(0, 0, 0, 0)
)

class SoundbarViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ShelfRepository(application)
    private val _uiState = MutableStateFlow(SoundbarUiState())
    val uiState: StateFlow<SoundbarUiState> = _uiState.asStateFlow()

    private var mixDays: List<Long> = emptyList()
    private var tickerJob: Job? = null
    private var noteSaveJob: Job? = null
    private var exchange: GuestExchange? = null
    private var sharingEnabled: Boolean = false

    init {
        val stored = restore()
        _uiState.update {
            it.copy(
                shelf = stored.blends,
                playCounts = stored.playCounts,
                listenedSeconds = stored.listenedSeconds,
                concept = Concept.fromId(stored.concept),
                notes = stored.notes,
                lastPlayed = stored.lastPlayed,
                signatureId = stored.signatureId,
                signatureBlend = stored.signatureBlend,
                guests = stored.guestRecipes,
                selectedPlatform = StreamingPlatform.fromId(stored.platform),
                mix = listOf(Ingredient("dreampop", 1), Ingredient("soul", 2))
            )
        }
        mixDays = stored.mixDays
        sharingEnabled = stored.sharingEnabled
        recomputeStats()
        refreshSignature()
        startSignature()
        if (sharingEnabled) setSharing(true)
    }

    /**
     * On open, the house automatically pours the user's signature playlist —
     * no play-count bump, it is simply what greets them.
     */
    private fun startSignature() {
        val state = _uiState.value
        val blend = state.signatureId?.let { findBlend(it) } ?: state.signatureBlend ?: return
        if (blend.tracks.isEmpty()) return
        _uiState.update { it.copy(playback = Playback(blend.id, 0, 0, true)) }
        startTicker()
    }

    /**
     * Recrafts the taste-built signature from the shelf and play history. A
     * manually starred blend always wins; unstar it and the house pour returns.
     */
    private fun refreshSignature() {
        val state = _uiState.value
        val crafted = if (state.signatureId == null) {
            SignatureCraft.craft(state.shelf, state.playCounts, state.concept)
        } else {
            null
        }
        if (crafted == state.signatureBlend) return
        _uiState.update { it.copy(signatureBlend = crafted) }
        persist()
        exchange?.updateSignature(currentSignature())
    }

    /** First launch stocks the shelf so the cabinet never opens empty. */
    private fun restore(): ShelfState {
        val stored = repository.load()
        if (stored.seeded) return stored
        val seeded = ShelfState(
            blends = BlendBar.classics(Concept.BAR) + BlendBar.tonight(Concept.BAR).drop(1),
            playCounts = mapOf(
                "house-midnight-espresso" to 21,
                "house-velvet-tiramisu" to 14,
                "house-bitter-sunset" to 9,
                "house-gin-tonicic" to 5,
                "house-caramel-apple" to 3
            ),
            lastPlayed = mapOf(
                "house-midnight-espresso" to System.currentTimeMillis() - 3L * 60 * 60 * 1000,
                "house-velvet-tiramisu" to System.currentTimeMillis() - 26L * 60 * 60 * 1000,
                "house-bitter-sunset" to System.currentTimeMillis() - 3L * 24 * 60 * 60 * 1000,
                "house-gin-tonicic" to System.currentTimeMillis() - 6L * 24 * 60 * 60 * 1000,
                "house-caramel-apple" to System.currentTimeMillis() - 9L * 24 * 60 * 60 * 1000
            ),
            listenedSeconds = 86L * 3600L,
            mixDays = listOf(today()),
            seeded = true
        )
        repository.save(seeded)
        return seeded
    }

    // region mixing

    fun selectBase(genreId: String) {
        _uiState.update { it.copy(baseGenreId = genreId, mix = emptyList()) }
    }

    fun addIngredient(genreId: String) {
        _uiState.update { state ->
            if (state.mix.any { it.genreId == genreId } || state.mix.size >= MAX_INGREDIENTS) {
                state
            } else {
                state.copy(mix = state.mix + Ingredient(genreId, 1))
            }
        }
    }

    fun removeIngredient(genreId: String) {
        _uiState.update { state -> state.copy(mix = state.mix.filterNot { it.genreId == genreId }) }
    }

    fun adjustDose(genreId: String, delta: Int) {
        _uiState.update { state ->
            val updated = state.mix.mapNotNull { ingredient ->
                if (ingredient.genreId != genreId) {
                    ingredient
                } else {
                    val parts = ingredient.parts + delta
                    if (parts <= 0) null else ingredient.copy(parts = parts.coerceAtMost(MAX_PARTS))
                }
            }
            state.copy(mix = updated)
        }
    }

    fun clearMix() {
        _uiState.update { it.copy(mix = emptyList()) }
    }

    /**
     * Shakes the current recipe into a served blend. Runs a short shake beat so
     * the button press has weight, then returns the new blend id to navigate to.
     */
    fun shake(onServed: (String) -> Unit) {
        if (_uiState.value.isShaking) return
        _uiState.update { it.copy(isShaking = true) }
        viewModelScope.launch {
            delay(SHAKE_MILLIS)
            val state = _uiState.value
            val blend = BlendBar.mix(state.baseGenreId, state.mix, concept = state.concept)
            mixDays = (mixDays + today()).distinct().takeLast(120)
            _uiState.update {
                it.copy(servedBlends = it.servedBlends + blend, isShaking = false)
            }
            persist()
            recomputeStats()
            refreshSignature()
            onServed(blend.id)
        }
    }

    // endregion

    // region concept

    /** Swaps the house between the dark bar and the bright kitchen. */
    fun toggleConcept() {
        _uiState.update { it.copy(concept = it.concept.other) }
        persist()
        refreshSignature()
    }

    /** Chooses the streaming house that play presses open. */
    fun setPlatform(platform: StreamingPlatform) {
        if (_uiState.value.selectedPlatform == platform) return
        _uiState.update { it.copy(selectedPlatform = platform) }
        persist()
    }

    // endregion

    // region shelf

    fun findBlend(id: String): Blend? {
        val concept = _uiState.value.concept
        val stored = _uiState.value.servedBlends.firstOrNull { it.id == id }
            ?: _uiState.value.shelf.firstOrNull { it.id == id }
            ?: _uiState.value.guests.firstOrNull { it.blend.id == id }?.blend
        return if (stored != null) BlendBar.adapt(stored, concept) else BlendBar.findHouseBlend(id, concept)
    }

    fun isOnShelf(id: String): Boolean = _uiState.value.shelf.any { it.id == id }

    fun toggleShelf(blend: Blend) {
        _uiState.update { state ->
            val onShelf = state.shelf.any { it.id == blend.id }
            if (onShelf) {
                state.copy(shelf = state.shelf.filterNot { it.id == blend.id })
            } else {
                // Saving a guest recipe adopts it into the personal book; the
                // guest- prefix stays on the id, so the badge follows it
                // everywhere and across restarts.
                state.copy(
                    shelf = state.shelf + blend,
                    guests = state.guests.filterNot { it.blend.id == blend.id }
                )
            }
        }
        persist()
        recomputeStats()
        refreshSignature()
    }

    /** Stars or unstars a blend as the user's signature playlist. */
    fun toggleSignature(blend: Blend) {
        _uiState.update { state ->
            state.copy(signatureId = if (state.signatureId == blend.id) null else blend.id)
        }
        persist()
        refreshSignature()
        exchange?.updateSignature(currentSignature())
    }

    /**
     * Tops a served blend with a garnish — a splash of a genre, a twist of an
     * artist, or one more song. The updated blend replaces itself everywhere it
     * lives: tonight's serves and the shelf, if it was saved.
     */
    fun applyGarnish(blend: Blend, garnish: Garnish) {
        val updated = GarnishBar.apply(blend, garnish)
        if (updated == blend) return
        _uiState.update { state ->
            state.copy(
                servedBlends = state.servedBlends.map { if (it.id == updated.id) updated else it },
                shelf = state.shelf.map { if (it.id == updated.id) updated else it }
            )
        }
        persist()
        recomputeStats()
    }

    /** Saves a margin note written on a recipe card; blank notes are erased. */
    fun updateNote(blendId: String, note: String) {
        _uiState.update { state ->
            val cleaned = note.trim()
            if (cleaned.isEmpty()) {
                if (state.notes.containsKey(blendId)) state.copy(notes = state.notes - blendId) else state
            } else {
                state.copy(notes = state.notes + (blendId to cleaned))
            }
        }
        scheduleNoteSave()
    }

    private fun scheduleNoteSave() {
        noteSaveJob?.cancel()
        noteSaveJob = viewModelScope.launch {
            delay(NOTE_SAVE_MILLIS)
            persist()
        }
    }

    // endregion

    // region guests

    /** The signature playlist that travels over the air — the manual star, or the crafted one. */
    private fun currentSignature(): Blend? {
        val state = _uiState.value
        return state.signatureId?.let { findBlend(it) } ?: state.signatureBlend
    }

    /**
     * Opts in or out of the nearby guest exchange. While on, the signature
     * playlist is advertised automatically and guest recipes are collected
     * without any prompt — only playlist data ever crosses the air.
     */
    fun setSharing(enabled: Boolean) {
        sharingEnabled = enabled
        if (enabled) {
            val client = exchange ?: GuestExchange(
                getApplication(),
                onGuestBlend = ::addGuestBlend,
                onStateChanged = ::onExchangeState
            ).also { exchange = it }
            client.start(currentSignature())
        } else {
            exchange?.stop()
        }
        persist()
    }

    private fun onExchangeState(state: GuestExchange.State) {
        _uiState.update {
            it.copy(
                isSharing = state == GuestExchange.State.RUNNING,
                isSharingAvailable = if (state == GuestExchange.State.UNAVAILABLE) false else it.isSharingAvailable
            )
        }
    }

    /**
     * Removes a guest recipe from the collection. If it happens to be pouring,
     * the needle is lifted first so nothing plays from a cleared card.
     */
    fun removeGuestRecipe(blendId: String) {
        val stopPlayback = _uiState.value.playback?.blendId == blendId
        if (stopPlayback) {
            tickerJob?.cancel()
            tickerJob = null
        }
        _uiState.update { state ->
            state.copy(
                guests = state.guests.filterNot { it.blend.id == blendId },
                playback = if (stopPlayback) null else state.playback
            )
        }
        persist()
    }

    /** Files a recipe that drifted in from a guest; duplicates are ignored. */
    private fun addGuestBlend(blend: Blend) {
        val card = GuestCard(blend.copy(id = "guest-${blend.id}"), System.currentTimeMillis())
        _uiState.update { state ->
            if (state.guests.any { it.blend.id == card.blend.id } ||
                state.shelf.any { it.id == card.blend.id }
            ) {
                // Already collected, or already adopted onto the shelf.
                state
            } else {
                state.copy(guests = (state.guests + card).takeLast(MAX_GUESTS))
            }
        }
        persist()
    }

    // endregion

    // region playback

    fun playBlend(blend: Blend, trackIndex: Int = 0) {
        if (blend.tracks.isEmpty()) return
        _uiState.update { state ->
            state.copy(
                playback = Playback(blend.id, trackIndex.coerceIn(0, blend.tracks.lastIndex), 0, true),
                playCounts = state.playCounts + (blend.id to (state.playCounts[blend.id] ?: 0) + 1),
                servedBlends = if (state.servedBlends.any { it.id == blend.id } ||
                    BlendBar.findHouseBlend(blend.id, state.concept) != null ||
                    state.shelf.any { it.id == blend.id } ||
                    state.guests.any { it.blend.id == blend.id }
                ) state.servedBlends else state.servedBlends + blend
            )
        }
        persist()
        startTicker()
        refreshSignature()
        launchExternal(blend, trackIndex)
    }

    fun togglePlayPause() {
        val playback = _uiState.value.playback ?: return
        _uiState.update { it.copy(playback = playback.copy(isPlaying = !playback.isPlaying)) }
        if (!playback.isPlaying) startTicker() else tickerJob?.cancel()
    }

    fun skipToNext() {
        val playback = _uiState.value.playback ?: return
        val blend = findBlend(playback.blendId) ?: return
        val next = (playback.trackIndex + 1) % blend.tracks.size
        _uiState.update { it.copy(playback = playback.copy(trackIndex = next, positionSeconds = 0, isPlaying = true)) }
        startTicker()
    }

    fun skipToPrevious() {
        val playback = _uiState.value.playback ?: return
        val blend = findBlend(playback.blendId) ?: return
        if (playback.positionSeconds > 4) {
            _uiState.update { it.copy(playback = playback.copy(positionSeconds = 0)) }
            return
        }
        val previous = if (playback.trackIndex == 0) blend.tracks.lastIndex else playback.trackIndex - 1
        _uiState.update { it.copy(playback = playback.copy(trackIndex = previous, positionSeconds = 0, isPlaying = true)) }
        startTicker()
    }

    /**
     * Hands the blend to the user's chosen streaming house — once per play,
     * never on skips or pauses, so the platform's own queue carries the music
     * on without the reader being pulled back and forth. Says where it went.
     */
    private fun launchExternal(blend: Blend, trackIndex: Int) {
        val platform = _uiState.value.selectedPlatform
        platform.launch(getApplication(), blend, trackIndex)
        val verb = if (_uiState.value.concept == Concept.BAR) "Now pouring in" else "Now playing in"
        Toast.makeText(getApplication(), "$verb ${platform.displayName}", Toast.LENGTH_SHORT).show()
    }

    fun stopPlayback() {
        tickerJob?.cancel()
        tickerJob = null
        _uiState.update { it.copy(playback = null) }
        persist()
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val state = _uiState.value
                val playback = state.playback ?: break
                if (!playback.isPlaying) break
                val blend = findBlend(playback.blendId) ?: break
                val track = blend.tracks.getOrNull(playback.trackIndex) ?: break
                val position = playback.positionSeconds + 1
                if (position >= track.seconds) {
                    val next = (playback.trackIndex + 1) % blend.tracks.size
                    _uiState.update {
                        it.copy(
                            playback = playback.copy(trackIndex = next, positionSeconds = 0),
                            listenedSeconds = it.listenedSeconds + 1
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            playback = playback.copy(positionSeconds = position),
                            listenedSeconds = it.listenedSeconds + 1
                        )
                    }
                }
                if (position % 30 == 0) {
                    persist()
                    recomputeStats()
                }
            }
        }
    }

    // endregion

    private fun recomputeStats() {
        val state = _uiState.value
        val tally = mutableMapOf<String, Int>()
        fun count(genres: List<String>) {
            genres.forEach { genre -> tally[genre] = (tally[genre] ?: 0) + 1 }
        }
        val tasted = buildSet {
            state.shelf.forEach {
                addAll(it.genreIds); count(it.genreIds)
                it.garnishes.forEach { g -> add(g.genreId); count(listOf(g.genreId)) }
            }
            state.servedBlends.forEach {
                addAll(it.genreIds); count(it.genreIds)
                it.garnishes.forEach { g -> add(g.genreId); count(listOf(g.genreId)) }
            }
            state.playCounts.keys.forEach { id ->
                BlendBar.findHouseBlend(id, state.concept)?.let { addAll(it.genreIds); count(it.genreIds) }
            }
        }
        _uiState.update {
            it.copy(
                stats = TasteStats(
                    blendsMixed = state.shelf.size + state.servedBlends.size,
                    genresExplored = tasted.size,
                    hoursListened = (state.listenedSeconds / 3600L).toInt(),
                    streakDays = streakFrom(mixDays),
                    genreTally = tally.entries.sortedByDescending { it.value }.map { it.key to it.value },
                    totalPours = state.playCounts.values.sum(),
                    mixDaysCount = mixDays.size,
                    recentMixDays = recentMixFlags(mixDays)
                )
            )
        }
    }

    /** True-flags for the last seven days, oldest first — feeds the ledger's streak strip. */
    private fun recentMixFlags(days: List<Long>): List<Boolean> {
        val mixed = days.toHashSet()
        val anchor = today()
        return (6 downTo 0).map { offset -> mixed.contains(anchor - offset) }
    }

    private fun persist() {
        val state = _uiState.value
        repository.save(
            ShelfState(
                blends = state.shelf,
                playCounts = state.playCounts,
                listenedSeconds = state.listenedSeconds,
                mixDays = mixDays,
                concept = state.concept.id,
                notes = state.notes,
                lastPlayed = state.lastPlayed,
                signatureId = state.signatureId,
                signatureBlend = state.signatureBlend,
                guestRecipes = state.guests,
                sharingEnabled = sharingEnabled,
                platform = state.selectedPlatform.id,
                seeded = true
            )
        )
    }

    override fun onCleared() {
        super.onCleared()
        tickerJob?.cancel()
        noteSaveJob?.cancel()
        exchange?.stop()
        persist()
    }

    private companion object {
        const val MAX_INGREDIENTS = 4
        const val MAX_PARTS = 4
        const val SHAKE_MILLIS = 850L
        const val NOTE_SAVE_MILLIS = 400L
        const val MAX_GUESTS = 60

        /** Local calendar day index, kept free of java.time so minSdk 24 stays happy. */
        fun today(): Long {
            val now = System.currentTimeMillis()
            val offset = TimeZone.getDefault().getOffset(now)
            return (now + offset) / 86_400_000L
        }

        /** Counts back from today over the days the user actually mixed something. */
        fun streakFrom(days: List<Long>): Int {
            if (days.isEmpty()) return 0
            val mixed = days.toHashSet()
            var cursor = today()
            if (!mixed.contains(cursor)) cursor -= 1
            var streak = 0
            while (mixed.contains(cursor)) {
                streak++
                cursor -= 1
            }
            return streak
        }
    }
}
