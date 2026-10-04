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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rork.soundbar.data.AuthProvider
import com.rork.soundbar.data.AuthState
import com.rork.soundbar.ui.theme.LocalConcept

/**
 * The door, opened from the menu's invitation. The guest signs in or creates
 * an account — with Google, Apple, or email + password — so the house keeps
 * their shelf, recipes, and notes waiting; but they are free to browse, mix,
 * and pour without ever knocking.
 */
@Composable
fun SignInScreen(
    state: AuthState,
    onSignIn: (AuthProvider) -> Unit,
    onEmailSubmit: (isSignUp: Boolean, email: String, password: String) -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val concept = LocalConcept.current
    val inProgress = state as? AuthState.InProgress
    val failure = state as? AuthState.Failed
    val pending = state as? AuthState.EmailConfirmationPending

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
            pending?.let {
                Text(
                    text = concept.authCheckInboxMessage(it.email),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    text = concept.authDividerLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }
            EmailDoor(
                isLoading = inProgress != null && inProgress.provider == null,
                enabled = inProgress == null,
                onEmailSubmit = onEmailSubmit,
                modifier = Modifier.padding(top = 8.dp)
            )
            Spacer(modifier = Modifier.weight(1.4f))
        }
    }
}

/**
 * The email door: address and password fields with a sign in / create account
 * toggle underneath the social doors.
 */
@Composable
private fun EmailDoor(
    isLoading: Boolean,
    enabled: Boolean,
    onEmailSubmit: (isSignUp: Boolean, email: String, password: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val concept = LocalConcept.current
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var signUpMode by rememberSaveable { mutableStateOf(false) }
    val canSubmit = enabled && email.contains('@') && password.length >= 6

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = email,
            onValueChange = { value -> email = value.trim() },
            label = { Text(text = concept.authEmailLabel) },
            singleLine = true,
            enabled = enabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = password,
            onValueChange = { value -> password = value },
            label = { Text(text = concept.authPasswordLabel) },
            singleLine = true,
            enabled = enabled,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
        )
        Surface(
            onClick = { onEmailSubmit(signUpMode, email, password) },
            enabled = canSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp)
                .height(52.dp),
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
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
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Text(
                    text = if (signUpMode) concept.authSignUpButton else concept.authSignInButton,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        TextButton(
            onClick = { signUpMode = !signUpMode },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
        ) {
            Text(
                text = if (signUpMode) concept.authToggleSignInLabel else concept.authToggleSignUpLabel,
                fontSize = 13.sp
            )
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
