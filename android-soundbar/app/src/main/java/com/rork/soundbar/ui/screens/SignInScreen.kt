package com.rork.soundbar.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rork.soundbar.data.AuthProvider
import com.rork.soundbar.data.AuthState
import com.rork.soundbar.ui.theme.LocalConcept

/**
 * The door, opened from the menu's invitation. The guest signs in or creates
 * an account so the house keeps their shelf, recipes, and notes waiting —
 * but they are free to browse, mix, and pour without ever knocking.
 */
@Composable
fun SignInScreen(
    state: AuthState,
    onSignIn: (AuthProvider) -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val concept = LocalConcept.current
    val inProgress = state as? AuthState.InProgress
    val failure = state as? AuthState.Failed

    Box(modifier = modifier.fillMaxSize()) {
        BarGlow()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (onBack != null) {
                TextButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Text(text = "Back", fontSize = 14.sp)
                }
            }
            Spacer(modifier = Modifier.weight(1.1f))
            Text(
                text = "Soundbar",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = concept.authHeadline,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 10.dp)
            )
            Text(
                text = concept.authBody,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            failure?.let {
                Text(
                    text = it.message,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 14.dp)
                )
            }
            AuthProvider.entries.forEach { provider ->
                AuthDoorButton(
                    provider = provider,
                    isLoading = inProgress?.provider == provider,
                    enabled = inProgress == null,
                    onClick = { onSignIn(provider) },
                    emphasized = provider == AuthProvider.GOOGLE,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
            Spacer(modifier = Modifier.weight(1.4f))
        }
    }
}

@Composable
private fun AuthDoorButton(
    provider: AuthProvider,
    isLoading: Boolean,
    enabled: Boolean,
    emphasized: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(50),
        color = if (emphasized) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surface
        },
        contentColor = if (emphasized) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        border = if (emphasized) {
            null
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.size(10.dp))
            }
            Text(
                text = if (isLoading) {
                    "Opening ${provider.displayName}…"
                } else {
                    "Continue with ${provider.displayName}"
                },
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
