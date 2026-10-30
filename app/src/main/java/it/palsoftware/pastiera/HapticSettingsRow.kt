package it.palsoftware.pastiera

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.palsoftware.pastiera.inputmethod.NotificationHelper

@Composable
fun HapticSettingsRow() {
    val context = LocalContext.current
    var tapHapticUseSystem by remember {
        mutableStateOf(SettingsManager.getTapHapticUseSystem(context))
    }
    var tapHapticDurationMs by remember {
        mutableStateOf(SettingsManager.getTapHapticDurationMs(context).toFloat())
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth().settingRow("haptics.system_haptics")
            .height(72.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Keyboard,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.tap_haptic_system_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Text(
                    text = stringResource(R.string.tap_haptic_system_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
            Switch(
                checked = tapHapticUseSystem,
                onCheckedChange = { enabled ->
                    tapHapticUseSystem = enabled
                    SettingsManager.setTapHapticUseSystem(context, enabled)
                }
            )
        }
    }

    if (!tapHapticUseSystem) {
        Surface(
            modifier = Modifier
                .fillMaxWidth().settingRow("haptics.haptic_duration")
                .height(104.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(R.string.tap_haptic_duration_title, tapHapticDurationMs.toInt()),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Text(
                    text = stringResource(R.string.tap_haptic_duration_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Slider(
                    value = tapHapticDurationMs,
                    onValueChange = { value ->
                        val rounded = (value / 5f).toInt() * 5L
                        if (rounded == tapHapticDurationMs.toLong()) {
                            return@Slider
                        }
                        tapHapticDurationMs = rounded.toFloat()
                        SettingsManager.setTapHapticDurationMs(context, rounded)
                        NotificationHelper.triggerFixedDurationHapticFeedback(context, rounded)
                    },
                    valueRange = SettingsManager.getMinTapHapticDurationMs().toFloat()..
                        SettingsManager.getMaxTapHapticDurationMs().toFloat(),
                    steps = 14
                )
            }
        }
    }
}
