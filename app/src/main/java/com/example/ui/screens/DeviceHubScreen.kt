package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.McLocalization
import com.example.core.McNotificationListenerService
import com.example.core.SystemPanelType
import com.example.ui.components.SectionTerminalCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.McViewModel

@Composable
fun DeviceHubScreen(
    viewModel: McViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isFlashOn by viewModel.isFlashlightOn.collectAsStateWithLifecycle()
    val notifications by viewModel.deviceNotifications.collectAsStateWithLifecycle()
    val strings = remember(settings.language) { McLocalization.get(settings.language) }

    var phoneInput by rememberSaveable { mutableStateOf("") }
    var smsNumber by rememberSaveable { mutableStateOf("") }
    var smsBody by rememberSaveable { mutableStateOf("") }

    val notifAccessGranted = remember(notifications.size) {
        McNotificationListenerService.isNotificationAccessEnabled(context)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianVoid),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = strings.deviceControlTitle,
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary
            )
        }

        // Quick Voice Call Card
        item {
            SectionTerminalCard(borderColor = NeonCyan.copy(alpha = 0.5f)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Call, contentDescription = null, tint = NeonCyan)
                        Text(
                            text = strings.quickCallTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { phoneInput = it },
                            placeholder = { Text("+880 / +1 Phone number") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = ObsidianBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("call_number_input")
                        )
                        Button(
                            onClick = { viewModel.deviceControl.dialNumber(phoneInput) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonCyan,
                                contentColor = ObsidianVoid
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("dial_button")
                        ) {
                            Text(strings.dialButton, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Quick SMS / Message Card
        item {
            SectionTerminalCard(borderColor = NeonEmerald.copy(alpha = 0.5f)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Message,
                            contentDescription = null,
                            tint = NeonEmerald
                        )
                        Text(
                            text = strings.quickMessageTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }
                    OutlinedTextField(
                        value = smsNumber,
                        onValueChange = { smsNumber = it },
                        placeholder = { Text("Recipient phone number") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonEmerald,
                            unfocusedBorderColor = ObsidianBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sms_number_input")
                    )
                    OutlinedTextField(
                        value = smsBody,
                        onValueChange = { smsBody = it },
                        placeholder = { Text("Message text...") },
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonEmerald,
                            unfocusedBorderColor = ObsidianBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sms_body_input")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.deviceControl.sendSms(smsNumber, smsBody) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonEmerald,
                                contentColor = ObsidianVoid
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("send_sms_button")
                        ) {
                            Text(strings.sendMessageButton, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = {
                                viewModel.deviceControl.shareText("Message from MC", smsBody)
                            },
                            border = BorderStroke(1.dp, NeonCyan),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                Icons.Filled.Share,
                                contentDescription = "Share via any app",
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Share App", color = NeonCyan)
                        }
                    }
                }
            }
        }

        // Screen, Flashlight & System Controls Grid
        item {
            SectionTerminalCard(borderColor = NeonViolet.copy(alpha = 0.5f)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = strings.screenControlTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ControlActionTile(
                            label = strings.flashlightLabel,
                            icon = if (isFlashOn) Icons.Filled.FlashlightOn else Icons.Filled.FlashlightOff,
                            active = isFlashOn,
                            onClick = { viewModel.toggleFlashlight() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("flashlight_tile")
                        )
                        ControlActionTile(
                            label = "Wi-Fi",
                            icon = Icons.Filled.Wifi,
                            active = false,
                            onClick = { viewModel.deviceControl.openSystemPanel(SystemPanelType.WIFI) },
                            modifier = Modifier.weight(1f)
                        )
                        ControlActionTile(
                            label = "Bluetooth",
                            icon = Icons.Filled.Bluetooth,
                            active = false,
                            onClick = { viewModel.deviceControl.openSystemPanel(SystemPanelType.BLUETOOTH) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ControlActionTile(
                            label = "Volume",
                            icon = Icons.AutoMirrored.Filled.VolumeUp,
                            active = false,
                            onClick = { viewModel.deviceControl.openSystemPanel(SystemPanelType.VOLUME) },
                            modifier = Modifier.weight(1f)
                        )
                        ControlActionTile(
                            label = "Display",
                            icon = Icons.Filled.Brightness6,
                            active = false,
                            onClick = { viewModel.deviceControl.openSystemPanel(SystemPanelType.DISPLAY) },
                            modifier = Modifier.weight(1f)
                        )
                        ControlActionTile(
                            label = "Screen Ctrl",
                            icon = Icons.Filled.AccessibilityNew,
                            active = false,
                            onClick = { viewModel.deviceControl.openSystemPanel(SystemPanelType.ACCESSIBILITY) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Live Notifications & Share Bridge
        item {
            SectionTerminalCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.NotificationsActive,
                                contentDescription = null,
                                tint = NeonAmber
                            )
                            Text(
                                text = strings.notificationsTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                        }
                        StatusPill(
                            label = if (notifAccessGranted) "LINKED" else "OFF",
                            accentColor = if (notifAccessGranted) NeonEmerald else NeonAmber
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.deviceControl.openSystemPanel(SystemPanelType.NOTIFICATION_ACCESS)
                            },
                            border = BorderStroke(1.dp, NeonCyan),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = strings.enableNotificationAccess,
                                style = MaterialTheme.typography.labelMedium,
                                color = NeonCyan
                            )
                        }

                        Button(
                            onClick = { viewModel.shareNotificationsSummary() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonViolet,
                                contentColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                Icons.Filled.Share,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = strings.shareAllNotifications,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    if (notifications.isEmpty()) {
                        Text(
                            text = if (notifAccessGranted) {
                                "No active notifications right now."
                            } else {
                                "Grant Notification Access so MC can read aloud and share incoming notifications with you."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        items(notifications, key = { it.key }) { notif ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = ObsidianCard,
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${notif.appPackage.uppercase()} • ${notif.title}",
                            style = MaterialTheme.typography.labelMedium,
                            color = NeonCyan
                        )
                        if (notif.text.isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = notif.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                        }
                    }
                    Row {
                        IconButton(
                            onClick = {
                                viewModel.voiceEngine.speak("${notif.title}. ${notif.text}")
                            }
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Read notification",
                                tint = NeonEmerald
                            )
                        }
                        IconButton(
                            onClick = {
                                viewModel.deviceControl.shareText(
                                    notif.title,
                                    "[${notif.appPackage}] ${notif.title}: ${notif.text}"
                                )
                            }
                        ) {
                            Icon(
                                Icons.Filled.Share,
                                contentDescription = "Share notification",
                                tint = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlActionTile(
    label: String,
    icon: ImageVector,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (active) NeonCyan.copy(alpha = 0.2f) else ObsidianElevated,
        border = BorderStroke(1.dp, if (active) NeonCyan else ObsidianBorder),
        modifier = modifier.height(74.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) NeonCyan else TextPrimary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (active) NeonCyan else TextSecondary,
                maxLines = 1
            )
        }
    }
}
