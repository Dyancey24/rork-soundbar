package com.rork.soundbar.ui.navigation

import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Blender
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Liquor
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SoupKitchen
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Blender
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.Liquor
import androidx.compose.material.icons.outlined.LocalBar
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.SoupKitchen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rork.soundbar.data.AuthManager
import com.rork.soundbar.data.AuthProvider
import com.rork.soundbar.data.AuthState
import com.rork.soundbar.data.AuthUser
import com.rork.soundbar.ui.SoundbarViewModel
import com.rork.soundbar.ui.components.NowPouringBar
import com.rork.soundbar.ui.components.NowPouringBarHost
import com.rork.soundbar.ui.screens.AccountScreen
import com.rork.soundbar.ui.screens.CookbookScreen
import com.rork.soundbar.ui.screens.DishScreen
import com.rork.soundbar.ui.screens.LeaderboardScreen
import com.rork.soundbar.ui.screens.MenuScreen
import com.rork.soundbar.ui.screens.MixScreen
import com.rork.soundbar.ui.screens.ProfileScreen
import com.rork.soundbar.ui.screens.ShelfScreen
import com.rork.soundbar.ui.screens.SignInScreen
import com.rork.soundbar.ui.theme.AppTheme
import com.rork.soundbar.ui.theme.Concept
import com.rork.soundbar.ui.theme.LocalConcept

private data class TabSpec(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

/** The tab bar re-labels and re-icons itself with the active concept. */
private fun tabsFor(concept: Concept): List<TabSpec> = if (concept == Concept.KITCHEN) {
    listOf(
        TabSpec("menu", concept.menuTab, Icons.Outlined.Restaurant, Icons.Filled.Restaurant),
        TabSpec("mix", concept.mixTab, Icons.Outlined.SoupKitchen, Icons.Filled.SoupKitchen),
        TabSpec("shelf", concept.shelfTab, Icons.Outlined.Kitchen, Icons.Filled.Kitchen),
        TabSpec("cookbook", concept.cookbookTab, Icons.Outlined.MenuBook, Icons.Filled.MenuBook),
        TabSpec("account", concept.accountTab, Icons.Outlined.Person, Icons.Filled.Person)
    )
} else {
    listOf(
        TabSpec("menu", concept.menuTab, Icons.Outlined.LocalBar, Icons.Filled.LocalBar),
        TabSpec("mix", concept.mixTab, Icons.Outlined.Blender, Icons.Filled.Blender),
        TabSpec("shelf", concept.shelfTab, Icons.Outlined.Liquor, Icons.Filled.Liquor),
        TabSpec("cookbook", concept.cookbookTab, Icons.Outlined.AutoStories, Icons.Filled.AutoStories),
        TabSpec("account", concept.accountTab, Icons.Outlined.Person, Icons.Filled.Person)
    )
}

private const val DISH_ROUTE = "dish/{blendId}"

@Composable
fun AppNavigation() {
    val appContext = LocalContext.current
    val auth = remember { AuthManager.get(appContext) }
    val authState by auth.state.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val viewModel: SoundbarViewModel = viewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val concept = state.concept

    // The bar serves after dark with light status icons; the kitchen flips them.
    val activity = LocalContext.current as? ComponentActivity
    LaunchedEffect(concept) {
        activity?.enableEdgeToEdge(
            statusBarStyle = if (concept == Concept.KITCHEN) {
                SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
            } else {
                SystemBarStyle.dark(Color.TRANSPARENT)
            },
            navigationBarStyle = if (concept == Concept.KITCHEN) {
                SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
            } else {
                SystemBarStyle.dark(Color.TRANSPARENT)
            }
        )
    }

    AppTheme(concept = concept) {
        CompositionLocalProvider(LocalConcept provides concept) {
            // The house serves every guest right away — signing in is an
            // invitation from the menu, not a door at the entrance.
            AppShell(
                navController = navController,
                viewModel = viewModel,
                authState = authState,
                account = (authState as? AuthState.SignedIn)?.user,
                onSignIn = auth::signIn,
                onSignOut = auth::signOut
            )
        }
    }
}

@Composable
private fun AppShell(
    navController: NavHostController,
    viewModel: SoundbarViewModel,
    authState: AuthState,
    account: AuthUser?,
    onSignIn: (AuthProvider) -> Unit,
    onSignOut: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val concept = state.concept

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val onTab = tabsFor(concept).any { it.route == currentRoute }

    val playback = state.playback
    val playingBlend = playback?.let { viewModel.findBlend(it.blendId) }
    val playingTrack = playingBlend?.tracks?.getOrNull(playback.trackIndex)

    fun openDish(blendId: String) {
        navController.navigate("dish/$blendId")
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (onTab) {
                Column {
                    if (currentRoute == "mix") {
                        ShakeBar(
                            enabled = !state.isShaking,
                            isShaking = state.isShaking,
                            onShake = {
                                viewModel.shake { blendId ->
                                    navController.navigate("dish/$blendId")
                                }
                            }
                        )
                    } else {
                        NowPouringBarHost(visible = playingBlend != null && playingTrack != null && playback != null) {
                            if (playingBlend != null && playingTrack != null && playback != null) {
                                NowPouringBar(
                                    blend = playingBlend,
                                    track = playingTrack,
                                    platformName = state.selectedPlatform.displayName,
                                    isPlaying = playback.isPlaying,
                                    progress = playback.positionSeconds.toFloat() /
                                        playingTrack.seconds.coerceAtLeast(1).toFloat(),
                                    onOpen = { openDish(playingBlend.id) },
                                    onTogglePlay = viewModel::togglePlayPause,
                                    onSkip = viewModel::skipToNext
                                )
                            }
                        }
                    }
                    BarTabs(
                        navController = navController,
                        currentRoute = currentRoute,
                        concept = concept
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "menu"
        ) {
            composable("menu") {
                MenuScreen(
                    playingBlendId = playback?.blendId,
                    isPlaying = playback?.isPlaying == true,
                    onOpenBlend = ::openDish,
                    onPlayBlend = { viewModel.playBlend(it) },
                    onTogglePlay = viewModel::togglePlayPause,
                    selectedPlatform = state.selectedPlatform,
                    onSelectPlatform = viewModel::setPlatform,
                    onToggleConcept = viewModel::toggleConcept,
                    accountName = account?.name,
                    onOpenAccount = { navController.navigate("account") },
                    contentPadding = padding
                )
            }
            composable("signin") {
                // Back to the menu the moment the account is through the door.
                LaunchedEffect(authState) {
                    if (authState is AuthState.SignedIn) navController.popBackStack()
                }
                SignInScreen(
                    state = authState,
                    onSignIn = onSignIn,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("mix") {
                MixScreen(
                    baseGenreId = state.baseGenreId,
                    mix = state.mix,
                    isShaking = state.isShaking,
                    onSelectBase = viewModel::selectBase,
                    onAddIngredient = viewModel::addIngredient,
                    onRemoveIngredient = viewModel::removeIngredient,
                    onAdjustDose = viewModel::adjustDose,
                    onToggleConcept = viewModel::toggleConcept,
                    contentPadding = padding
                )
            }
            composable("shelf") {
                ShelfScreen(
                    shelf = state.shelf,
                    playCounts = state.playCounts,
                    stats = state.stats,
                    onOpenBlend = ::openDish,
                    onStartMixing = { navController.navigate("mix") },
                    onToggleConcept = viewModel::toggleConcept,
                    isSyncing = state.isSyncing,
                    onRefresh = viewModel::refreshFromCloud,
                    contentPadding = padding
                )
            }
            composable("cookbook") {
                CookbookScreen(
                    shelf = state.shelf,
                    guests = state.guests,
                    notes = state.notes,
                    lastPlayed = state.lastPlayed,
                    signatureId = state.signatureId ?: state.signatureBlend?.id,
                    playingBlendId = playback?.blendId,
                    isPlaying = playback?.isPlaying == true,
                    isSharing = state.isSharing,
                    isSharingAvailable = state.isSharingAvailable,
                    onSetSharing = viewModel::setSharing,
                    onRemoveGuest = viewModel::removeGuestRecipe,
                    onOpenBlend = ::openDish,
                    onPlayBlend = { viewModel.playBlend(it) },
                    onUpdateNote = viewModel::updateNote,
                    onStartMixing = { navController.navigate("mix") },
                    onToggleConcept = viewModel::toggleConcept,
                    contentPadding = padding
                )
            }
            composable("account") {
                AccountScreen(
                    signedIn = account != null,
                    accountName = account?.name,
                    accountEmail = account?.email,
                    points = state.points,
                    genreBadges = state.genreBadges,
                    genreSongs = state.genreSongs,
                    eventBadges = state.eventBadges,
                    songsHeard = state.listenedTracks.size,
                    albumsCompleted = state.completedAlbums.size,
                    profile = state.profile,
                    selectedPlatform = state.selectedPlatform,
                    onSelectPlatform = viewModel::setPlatform,
                    onToggleConcept = viewModel::toggleConcept,
                    isSharing = state.isSharing,
                    isSharingAvailable = state.isSharingAvailable,
                    onSetSharing = viewModel::setSharing,
                    onSignIn = { navController.navigate("signin") },
                    onSignOut = onSignOut,
                    onOpenProfile = { navController.navigate("profile") },
                    onOpenLeaderboard = { navController.navigate("leaderboard") },
                    contentPadding = padding
                )
            }
            composable("leaderboard") {
                LeaderboardScreen(
                    signedIn = account != null,
                    points = state.points,
                    leaderboard = state.leaderboard,
                    isLoading = state.isLeaderboardLoading,
                    onRefresh = viewModel::loadLeaderboard,
                    onAddFriend = viewModel::addFriend,
                    onRemoveFriend = viewModel::removeFriend,
                    onBack = { navController.popBackStack() },
                    onSignIn = { navController.navigate("signin") },
                    contentPadding = padding
                )
            }
            composable("profile") {
                ProfileScreen(
                    profile = state.profile,
                    onUpdate = viewModel::updateProfile,
                    onBack = { navController.popBackStack() },
                    contentPadding = padding
                )
            }
            composable(DISH_ROUTE) { entry ->
                val blendId = entry.arguments?.getString("blendId").orEmpty()
                val blend = viewModel.findBlend(blendId)
                if (blend == null) {
                    MissingBlend(onBack = { navController.popBackStack() })
                } else {
                    val isSaved = state.shelf.any { it.id == blend.id }
                    DishScreen(
                        blend = blend,
                        playingTrackIndex = if (playback?.blendId == blend.id) playback.trackIndex else null,
                        isPlaying = playback?.blendId == blend.id && playback.isPlaying,
                        isSaved = isSaved,
                        isSignature = blend.id == (state.signatureId ?: state.signatureBlend?.id),
                        isGuest = blendId.startsWith("guest-"),
                        canRemove = blendId.startsWith("guest-") && !isSaved,
                        onRemove = {
                            viewModel.removeGuestRecipe(blendId)
                            navController.popBackStack()
                        },
                        onToggleSignature = { viewModel.toggleSignature(blend) },
                        canGarnish = blendId.startsWith("mix-"),
                        onApplyGarnish = { garnish -> viewModel.applyGarnish(blend, garnish) },
                        note = state.notes[blendId].orEmpty(),
                        onNoteChange = { viewModel.updateNote(blendId, it) },
                        onBack = { navController.popBackStack() },
                        onPlayTrack = { index -> viewModel.playBlend(blend, index) },
                        onTogglePlay = viewModel::togglePlayPause,
                        onToggleSave = { viewModel.toggleShelf(blend) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BarTabs(
    navController: NavHostController,
    currentRoute: String?,
    concept: Concept
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        tonalElevation = 0.dp
    ) {
        val backStackEntry by navController.currentBackStackEntryAsState()
        tabsFor(concept).forEach { tab ->
            val selected = backStackEntry?.destination?.hierarchy?.any { it.route == tab.route } == true ||
                currentRoute == tab.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(tab.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (selected) tab.selectedIcon else tab.icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = tab.label,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

/** The Mix tab's primary action, seated directly above the tab bar. */
@Composable
private fun ShakeBar(
    enabled: Boolean,
    isShaking: Boolean,
    onShake: () -> Unit,
    modifier: Modifier = Modifier
) {
    val concept = LocalConcept.current
    val accent = MaterialTheme.colorScheme.primary
    val scale by animateFloatAsState(
        targetValue = if (isShaking) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 600f),
        label = "shakeScale"
    )
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
            Button(
                onClick = onShake,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .height(54.dp)
                    .scale(scale),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = accent.copy(alpha = 0.7f),
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = if (isShaking) concept.shakingLabel else concept.shakeLabel,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun MissingBlend(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(24.dp)) {
        Text(
            text = "That one has been cleared away.",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Button(
            onClick = onBack,
            modifier = Modifier.padding(top = 16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text("Back to the menu")
        }
    }
}
