package com.rork.soundbar.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The two moods the app can be served in: a dark after-hours bar or a bright
 * daylight kitchen. Every piece of house vocabulary hangs off this enum so the
 * whole app — colours, words, and artwork — re-skins with a single switch.
 */
enum class Concept(val id: String) {
    BAR("bar"),
    KITCHEN("kitchen");

    val other: Concept get() = if (this == BAR) KITCHEN else BAR

    val trayLabel: String get() = if (this == BAR) "Back shelf" else "Spice rack"
    val topShelfTitle: String get() = if (this == BAR) "Top Shelf" else "Chef's Display"
    // Keys keep the legacy "cookbook" name; the product calls this tab the Recipe Book in both themes.
    val cookbookTab: String get() = "Recipe Book"
    val cookbookTitle: String get() = "The Recipe Book"
    val artistsLabel: String get() = "You'll hear"
    val popularTag: String get() = if (this == BAR) "Popular" else "Crowd favorite"
    val undergroundTag: String get() = if (this == BAR) "Deep cut" else "Secret ingredient"
    val notesLabel: String get() = if (this == BAR) "Margin notes" else "Recipe notes"
    val noteHint: String get() = if (this == BAR) "Jot a tasting note…" else "Write down your tweaks…"
    val cookbookEmptyTitle: String get() = "No recipes yet"
    val cookbookEmptyBody: String
        get() = if (this == BAR) {
            "Save a blend to the shelf and its recipe card is written up here."
        } else {
            "Save a dish to the pantry and its recipe card is written up here."
        }
    val traySearchHint: String get() = if (this == BAR) "Search the shelf" else "Search the rack"
    val surpriseLabel: String get() = if (this == BAR) "Bartender's pick" else "Chef's pick"
    val surpriseHint: String get() = if (this == BAR) "an odd pour, on the house" else "a wild pinch of something"
    val noResultsLine: String get() = if (this == BAR) "Nothing like that behind the bar." else "Nothing like that in the rack."
    val openLine: String get() = if (this == BAR) "the bar is open" else "the kitchen is open"
    val tonightLabel: String get() = if (this == BAR) "Tonight's menu" else "Today's menu"
    val classicsTitle: String get() = if (this == BAR) "House Classics" else "House Specials"
    val mixingTitle: String get() = if (this == BAR) "Mixing Station" else "The Kitchen"
    val baseLabel: String get() = if (this == BAR) "Your base spirit" else "Your base ingredient"
    val neatLine: String
        get() = if (this == BAR) {
            "Served neat. Add a pairing below to build it out."
        } else {
            "Served plain. Add a pairing below to build it out."
        }
    val shakeLabel: String get() = if (this == BAR) "Shake It Up" else "Stir the Pot"
    val shakingLabel: String get() = if (this == BAR) "Shaking…" else "Stirring…"
    val shelfTitle: String get() = if (this == BAR) "Your Shelf" else "Your Pantry"
    val regularsTitle: String get() = if (this == BAR) "Regulars" else "Favorites"
    val restTitle: String get() = if (this == BAR) "Rest of the shelf" else "More from the pantry"
    val streakLabel: String get() = if (this == BAR) "-day mixing streak" else "-day cooking streak"
    val genreDetailTitle: String get() = if (this == BAR) "The flavour board" else "The spice rack"
    val genreDetailEmptyBody: String
        get() = if (this == BAR) {
            "Pour something from the menu and the board fills in."
        } else {
            "Plate something from the menu and the rack fills in."
        }

    fun genreTallyLabel(name: String, count: Int): String = "$name × $count"
    val hoursDetailTitle: String get() = if (this == BAR) "Time at the bar" else "Time at the stove"

    fun hoursDetailBody(totalPours: Int, favourite: String?): String {
        val lead = if (this == BAR) "$totalPours pours so far" else "$totalPours plates served so far"
        return if (favourite == null) {
            "$lead — and this is just the warm-up."
        } else {
            "$lead — most often it's $favourite."
        }
    }

    val streakDetailTitle: String get() = if (this == BAR) "The nightly run" else "The daily run"

    fun streakDetailBody(streak: Int, totalDays: Int): String {
        if (streak == 0) {
            return if (this == BAR) {
                "No run going yet — shake something tonight to start one."
            } else {
                "No run going yet — cook up something new today to start one."
            }
        }
        val run = if (this == BAR) {
            "$streak night${if (streak == 1) "" else "s"} in a row. Keep it alive tonight."
        } else {
            "$streak day${if (streak == 1) "" else "s"} in a row. Keep it alive today."
        }
        val total = if (this == BAR) {
            " You've fired up the still on $totalDays day${if (totalDays == 1) "" else "s"} in total."
        } else {
            " You've cooked something new on $totalDays day${if (totalDays == 1) "" else "s"} in total."
        }
        return "$run$total"
    }

    val tasteHubTitle: String get() = if (this == BAR) "The house ledger" else "The recipe ledger"
    val tasteHubHint: String get() = "Tap a number for its story — or flip it into a chart."

    fun chartToggleLabel(showChart: Boolean): String =
        if (showChart) {
            if (this == BAR) "Read it as a story" else "Read it as a recipe"
        } else {
            if (this == BAR) "Show it as a chart" else "Plate it as a chart"
        }

    fun poursChartCaption(count: Int): String =
        if (this == BAR) "$count pours" else "$count plates served"

    val emptyTitle: String get() = if (this == BAR) "The shelf is bare" else "The pantry is bare"
    val emptyBody: String
        get() = if (this == BAR) {
            "Mix a blend at the station and save it here to start your collection."
        } else {
            "Cook up a dish in the kitchen and save it here to start your collection."
        }
    val emptyCta: String get() = if (this == BAR) "Open the Mixing Station →" else "Open the Kitchen →"
    val menuFooter: String
        get() = if (this == BAR) {
            "Pick a base spirit in the Mixing Station to pour something of your own."
        } else {
            "Pick a base ingredient in the Kitchen to cook something of your own."
        }
    val servedLine: String get() = if (this == BAR) "Served tonight" else "Fresh from the kitchen"
    val streamingLabel: String get() = if (this == BAR) "Pouring through" else "Playing through"
    val authHeadline: String get() = if (this == BAR) "Pull up a chair" else "Tie on the apron"
    val syncUpdatedMessage: String
        get() = if (this == BAR) "Poured in from the cloud — shelf refreshed." else "Fresh from the cloud — pantry restocked."
    val syncCurrentMessage: String
        get() = if (this == BAR) "Your shelf already matches the cloud." else "Your pantry already matches the cloud."
    val syncFailedMessage: String
        get() = "Couldn't reach the cloud — try again shortly."
    val authBody: String
        get() = if (this == BAR) {
            "Create an account or sign in — your shelf, your recipes, and your notes are kept waiting for you."
        } else {
            "Create an account or sign in — your pantry, your recipes, and your notes are kept waiting for you."
        }
    val playLabel: String get() = if (this == BAR) "Play the blend" else "Play the dish"
    val pauseLabel: String get() = if (this == BAR) "Pause the blend" else "Pause the dish"
    val savedLabel: String get() = if (this == BAR) "On your shelf" else "In your pantry"
    val saveLabel: String get() = if (this == BAR) "Save to shelf" else "Save to pantry"
    val removeLabel: String get() = if (this == BAR) "Remove from shelf" else "Remove from pantry"
    val switchLabel: String
        get() = if (this == BAR) "Switch to the kitchen theme" else "Switch to the bar theme"
    val menuTab: String get() = "Menu"
    val mixTab: String get() = if (this == BAR) "Mix" else "Cook"
    val shelfTab: String get() = if (this == BAR) "Shelf" else "Pantry"

    /** The account desk: tab, title, and the rewards ledger's vocabulary. */
    val accountTab: String get() = if (this == BAR) "Members" else "Household"
    val accountTitle: String get() = if (this == BAR) "The Members' Desk" else "The Household Ledger"
    val rewardsTitle: String get() = if (this == BAR) "House Rewards" else "Kitchen Merits"
    val rewardsTagline: String
        get() = if (this == BAR) {
            "Listen around the house — every badge sweetens the pour."
        } else {
            "Taste around the kitchen — every badge sweetens the pot."
        }
    val badgesTitle: String get() = if (this == BAR) "Genre badges" else "Flavor badges"
    val badgeLocked: String get() = "Locked"
    val settingsTitle: String get() = "Settings"
    val themeSettingLabel: String get() = if (this == BAR) "Bar or kitchen" else "Kitchen or bar"
    val rewardsExplainer: String
        get() = "10 points a song · 25 points an album · ×1.5 base · each badge up to ×10"
    val eventBadgesTitle: String get() = "Event badges"
    val founderTitle: String get() = "Founder badge"
    val founderDescription: String
        get() = if (this == BAR) {
            "For the first guests behind the bar — earned in the opening three months after launch."
        } else {
            "For the first guests in the kitchen — earned in the opening three months after launch."
        }
    val founderEarnedLabel: String get() = "Yours forever"
    val founderBonusLabel: String get() = "×50 boost"
    val founderEarnedMessage: String
        get() = if (this == BAR) {
            "Founder badge earned — the bar will remember you."
        } else {
            "Founder badge earned — the kitchen will remember you."
        }

    /** The friends leaderboard: entry card, the board, and the friend-code exchange. */
    val leaderboardTitle: String get() = "Leaderboard"
    val leaderboardTagline: String
        get() = if (this == BAR) {
            "Pour more points than your friends."
        } else {
            "Plate more points than your friends."
        }
    val friendsTitle: String get() = "Friends"
    val yourCodeLabel: String get() = "Your friend code"
    val addFriendLabel: String get() = "Add a friend"
    val friendCodeHint: String get() = "Enter a friend's code"
    val friendAddButton: String get() = "Add"
    val youLabel: String get() = "You"
    val leaderboardEmpty: String
        get() = if (this == BAR) {
            "Share your code or add a friend's — then compare pours."
        } else {
            "Share your code or add a friend's — then compare dishes."
        }
    fun friendInvitedMessage(name: String): String =
        if (this == BAR) {
            "Raised a glass to $name — you'll share a board when they raise one back."
        } else {
            "Offered $name a taste — you'll share a board when they taste back."
        }
    fun friendClinkedMessage(name: String): String =
        if (this == BAR) {
            "Clink — you and $name now share a leaderboard."
        } else {
            "Taste traded — you and $name now share a leaderboard."
        }
    val friendNotFoundMessage: String get() = "No player with that code — check it and try again."
    val friendOwnCodeMessage: String get() = "That's your own code — add a friend's instead."
    val friendRemovedMessage: String get() = "Removed from the leaderboard."
    val friendPassedMessage: String
        get() = if (this == BAR) {
            "Passed — the glass stays unraised."
        } else {
            "Passed — the taste stays untasted."
        }
    val leaderboardFailedMessage: String get() = "Couldn't reach the leaderboard — try again shortly."
    val codeCopiedMessage: String get() = "Friend code copied."

    /** The mutual-confirmation flow: it takes two to share a board. */
    val inviteHint: String
        get() = if (this == BAR) {
            "It takes two to clink — you'll share a board once they add your code too."
        } else {
            "It takes two to trade tastes — you'll share a board once they add your code too."
        }
    val invitesTitle: String
        get() = if (this == BAR) "Glasses raised to you" else "Tastes offered to you"
    val inviteAcceptButton: String
        get() = if (this == BAR) "Clink back" else "Taste back"
    val invitePassButton: String get() = "Pass"
    val inviteWaitingLabel: String
        get() = if (this == BAR) "Waiting on their clink" else "Waiting on their taste"

    /** The profile: the reader's chosen name, mark, and the public opt-in. */
    val profileTitle: String get() = "Your profile"
    val profileTagline: String
        get() = if (this == BAR) "How the house knows you." else "How the kitchen knows you."
    val profileNameLabel: String get() = "Username"
    val profileNameHint: String
        get() = if (this == BAR) "Name on your house card" else "Name on your kitchen card"
    val profileMarkLabel: String get() = "Pick your mark"
    val profileColorLabel: String get() = "Pick your colours"
    val profilePublicTitle: String get() = "Public profile"
    val profilePublicOnHint: String
        get() = if (this == BAR) {
            "Your name and mark ride along when your house pour passes a guest, and they show on the friends leaderboard."
        } else {
            "Your name and mark ride along when your house special passes a guest, and they show on the friends leaderboard."
        }
    val profilePublicOffHint: String
        get() = "Private — only the playlist travels. No name, no mark, nothing about you."
    val profilePublicOnMessage: String
        get() = if (this == BAR) {
            "Public — your name and mark now ride the pass."
        } else {
            "Public — your name and mark now ride the counter."
        }
    val profilePublicOffMessage: String get() = "Private again — the playlist travels alone."
    val profileFootnote: String
        get() = "Your profile lives on this device. Turning it off pulls your name and mark back in everywhere."
    val profilePublicOnTag: String
        get() = if (this == BAR) "Public — your name rides the pass" else "Public — your name rides the counter"
    val profilePublicOffTag: String get() = "Private — only the playlist travels"

    /** The anonymous-pass option: shown once the profile is public. */
    val profilePassTitle: String get() = "Anonymous passes"
    val profilePassOnHint: String
        get() = if (this == BAR) {
            "Your pours still travel, but without your name or mark attached."
        } else {
            "Your specials still travel, but without your name or mark attached."
        }
    val profilePassOffHint: String
        get() = if (this == BAR) {
            "Guests who catch your pour see your name and mark."
        } else {
            "Guests who catch your special see your name and mark."
        }

    /** The house-dealt board name, shown until the reader sets a username. */
    val profileAliasLabel: String get() = "On the leaderboard"
    val profileAliasHint: String
        get() = "The house dealt you a name — you'll play under it until you choose your own."
    val profileAliasShuffle: String get() = "Deal a new name"

    fun guestFromLabel(name: String): String = "From $name"

    fun badgeEarnedMessage(genre: String): String =
        if (this == BAR) {
            "New badge: $genre — every pour now earns more."
        } else {
            "New badge: $genre — every dish now earns more."
        }

    fun shelfCount(count: Int): String =
        if (this == BAR) "$count blends on the shelf" else "$count dishes in the pantry"

    fun cookbookCount(count: Int): String =
        if (this == BAR) "$count recipes on file" else "$count recipe cards"

    // The recipe card's flip: the photograph faces front, the namesake build faces back.
    val recipeBackTitle: String get() = if (this == BAR) "The build" else "The recipe"
    val recipeBackHint: String
        get() = if (this == BAR) "Tap to flip back to the photograph" else "Tap to flip back to the photo"
    val recipeBaseWord: String get() = if (this == BAR) "the pour" else "the base"

    fun doseLabel(parts: Int): String = when (parts) {
        1 -> if (this == BAR) "a dash" else "a pinch"
        2 -> if (this == BAR) "a splash" else "a spoonful"
        3 -> if (this == BAR) "a generous pour" else "a generous scoop"
        else -> "$parts parts"
    }

    /** When this recipe was last played, phrased per concept; epoch math only, so minSdk 24 stays happy. */
    fun servedAgo(playedAtMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val verb = if (this == BAR) "poured" else "cooked"
        val minutes = ((nowMillis - playedAtMillis) / 60_000L).coerceAtLeast(0)
        val span = when {
            minutes < 1L -> "just now"
            minutes < 60L -> "${minutes}m ago"
            minutes < 1_440L -> "${minutes / 60L}h ago"
            else -> "${minutes / 1_440L}d ago"
        }
        return "Last $verb $span"
    }

    /** The user's starred default playlist, named per concept. */
    val signatureTag: String get() = if (this == BAR) "House pour" else "House special"
    val setSignatureLabel: String
        get() = if (this == BAR) "Set as the house pour" else "Set as the house special"
    val clearSignatureLabel: String
        get() = if (this == BAR) "No longer the house pour" else "No longer the house special"

    /** Garnish vocabulary: the finishing touches offered after a pour. */
    val garnishTitle: String get() = "Add a garnish"
    val garnishHint: String
        get() = if (this == BAR) {
            "The pour is done. Drop something extra on top."
        } else {
            "The dish is plated. Finish it with a flourish."
        }
    val garnishAppliedLabel: String get() = if (this == BAR) "Garnished with" else "Finished with"

    fun garnishGenreLabel(genre: String): String =
        if (this == BAR) "a splash of $genre" else "a pinch of $genre"

    fun garnishArtistLabel(artist: String): String =
        if (this == BAR) "a twist of $artist" else "a sprig of $artist"

    fun garnishSongLabel(title: String): String =
        if (this == BAR) "“$title” on the rocks" else "a side of $title"

    /** Guest exchange vocabulary: nearby sharing and the collected recipes. */
    val guestTab: String get() = "Guest recipes"
    val myRecipesTab: String get() = "My recipes"
    val guestTag: String get() = "From a guest"
    val guestEmptyTitle: String get() = "No guest recipes yet"
    val guestEmptyBody: String
        get() = if (this == BAR) {
            "Turn on nearby sharing and any house pour that drifts past is written up here."
        } else {
            "Turn on nearby sharing and any house special that drifts past is written up here."
        }
    val sharingTitle: String get() = if (this == BAR) "Passing drinks" else "Passing plates"
    val sharingLabel: String
        get() = if (this == BAR) {
            "Share my house pour with guests nearby"
        } else {
            "Share my house special with guests nearby"
        }
    val sharingActive: String
        get() = if (this == BAR) {
            "Listening for guests nearby — your house pour is on the pass."
        } else {
            "Listening for guests nearby — your house special is on the pass."
        }
    val sharingHint: String
        get() = "Only the playlist travels. No names, no accounts, nothing about you."
    val sharingUnavailable: String get() = "Nearby sharing isn't available on this device."
    val guestRemoveLabel: String
        get() = if (this == BAR) "Send it back" else "Clear the plate"

    fun guestCount(count: Int): String =
        if (this == BAR) "$count guest recipes collected" else "$count guest recipe cards"

    /** When a guest recipe drifted in, phrased per concept; epoch math only. */
    fun guestReceivedAgo(receivedAtMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val minutes = ((nowMillis - receivedAtMillis) / 60_000L).coerceAtLeast(0)
        val span = when {
            minutes < 1L -> "just now"
            minutes < 60L -> "${minutes}m ago"
            minutes < 1_440L -> "${minutes / 60L}h ago"
            else -> "${minutes / 1_440L}d ago"
        }
        return "Received $span"
    }

    companion object {
        fun fromId(id: String?): Concept = if (id == KITCHEN.id) KITCHEN else BAR
    }

    /** Wood for the shelf planks the bottles stand on. */
    val plankColor: Color get() = if (this == BAR) Color(0xFF4A3423) else Color(0xFFD9B98C)
    val plankEdge: Color get() = if (this == BAR) Color(0xFF2E2013) else Color(0xFFB79463)

    /** The cream paper label on every bottle. */
    val bottleLabel: Color get() = if (this == BAR) Color(0xFFF3E9DC) else Color(0xFFFFFAF0)
    val bottleLabelInk: Color get() = Color(0xFF3B2A1E)
}

val LocalConcept = staticCompositionLocalOf { Concept.BAR }
