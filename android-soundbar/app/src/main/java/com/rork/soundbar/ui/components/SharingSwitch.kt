package com.rork.soundbar.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.rork.soundbar.data.GuestExchange

/**
 * The nearby-sharing switch: before turning the exchange on, it asks for the
 * radio permissions Nearby Connections needs on this OS version and only
 * flips on once every one is granted. Turning off passes straight through.
 */
@Composable
fun SharingSwitch(
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.all { it }) onChecked(true)
    }
    Switch(
        checked = checked,
        onCheckedChange = { turnOn ->
            if (!turnOn) {
                onChecked(false)
            } else {
                val missing = GuestExchange.missingPermissions(context)
                if (missing.isEmpty()) {
                    onChecked(true)
                } else {
                    permissionLauncher.launch(missing.toTypedArray())
                }
            }
        },
        enabled = enabled,
        modifier = modifier
    )
}
