package com.rork.soundbar

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.rork.soundbar.data.AuthConfig
import com.rork.soundbar.data.AuthManager
import com.rork.soundbar.data.SpotifyConfig
import com.rork.soundbar.data.SpotifyManager
import com.rork.soundbar.ui.navigation.AppNavigation

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            AppNavigation()
        }
        handleAuthIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleAuthIntent(intent)
    }

    /** The sign-in browser hops back into the house through the deep link. */
    private fun handleAuthIntent(intent: Intent?) {
        val data: Uri = intent?.data ?: return
        if (data.scheme == AuthConfig.CALLBACK_SCHEME &&
            data.host == AuthConfig.CALLBACK_HOST &&
            data.path == AuthConfig.CALLBACK_PATH
        ) {
            AuthManager.get(applicationContext).handleCallback(data)
        }
        if (data.scheme == SpotifyConfig.REDIRECT_SCHEME &&
            data.host == SpotifyConfig.REDIRECT_HOST
        ) {
            SpotifyManager.get(applicationContext).handleCallback(data)
        }
    }
}
