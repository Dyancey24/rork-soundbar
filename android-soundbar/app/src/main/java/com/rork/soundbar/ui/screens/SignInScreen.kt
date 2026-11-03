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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import com.rork.soundbar.data.AuthState
import com.rork.soundbar.ui.theme.LocalConcept

/**
 * The door, opened from the menu's invitation. The guest signs in or creates
 * an account with email + password so the house keeps their shelf, recipes,
 * and notes waiting; but they are free to browse, mix, and pour without ever
 * knocking.
 */
@Composable
fun SignInScreen(
    state: AuthState,
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
            if (state is AuthState.EmailConfirmed) {
                // The link came home: the address checks out, only the sign-in remains.
                Text(
                    text = concept.emailConfirmedBanner,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 14.dp)
                )
            }
            failure?.let {
                Text(
                    text = it.message,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 14.dp)
                )
            }
            if (pending != null) {
                // The inbox holds the last step: the door gives way to the wait.
                ConfirmationPanel(email = pending.email)
            } else {
                EmailDoor(
                    isLoading = inProgress != null && inProgress.provider == null,
                    enabled = inProgress == null,
                    onEmailSubmit = onEmailSubmit,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
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

/** The wait for the confirmation link: what was sent, and where to look. */
@Composable
private fun ConfirmationPanel(
    email: String,
    modifier: Modifier = Modifier
) {
    val concept = LocalConcept.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 14.dp, bottom = 8.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Outlined.MarkEmailRead,
                    contentDescription = concept.emailPendingTitle,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
        Text(
            text = concept.emailPendingTitle,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 14.dp)
        )
        Text(
            text = concept.emailPendingBody(email),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = concept.emailPendingHint,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 10.dp)
        )
    }
}
