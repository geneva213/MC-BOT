package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.AppLanguage
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
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.McViewModel

@Composable
fun SettingsScreen(
    viewModel: McViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val netConnected by viewModel.isNetworkAvailable.collectAsStateWithLifecycle()
    val isOnline by viewModel.isEffectiveOnline.collectAsStateWithLifecycle()
    val strings = remember(settings.language) { McLocalization.get(settings.language) }

    var githubDraft by remember(settings.githubUsername) {
        mutableStateOf(settings.githubUsername)
    }

    val wakePermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.setScreenOffFastWake(true)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val camOk = grants[Manifest.permission.CAMERA] == true
        if (camOk) {
            viewModel.setBackgroundVideoTalk(true)
        }
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
                text = strings.settingsTitle,
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary
            )
        }

        // Language Option: English & Bangla
        item {
            SectionTerminalCard(borderColor = NeonViolet.copy(alpha = 0.6f)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Language, contentDescription = null, tint = NeonViolet)
                        Column {
                            Text(
                                text = strings.languageSettingTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = strings.languageSettingDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AppLanguage.entries.forEach { lang ->
                            val selected = settings.language == lang
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) NeonViolet else ObsidianElevated,
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) NeonViolet else ObsidianBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.setLanguage(lang) }
                                    .testTag("lang_option_${lang.code}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = lang.nativeLabel,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = lang.displayName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (selected) TextPrimary else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Online / Offline Mode Selector
        item {
            SectionTerminalCard(borderColor = if (isOnline) NeonEmerald.copy(alpha = 0.55f) else NeonAmber.copy(alpha = 0.55f)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isOnline) Icons.Filled.Cloud else Icons.Filled.CloudOff,
                                contentDescription = null,
                                tint = if (isOnline) NeonEmerald else NeonAmber
                            )
                            Column {
                                Text(
                                    text = strings.offlineModeTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = strings.offlineModeDesc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                        StatusPill(
                            label = if (netConnected) "NET OK" else "NO NET",
                            accentColor = if (netConnected) NeonEmerald else NeonAmber
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (!settings.offlineMode) NeonEmerald.copy(alpha = 0.2f) else ObsidianElevated,
                            border = BorderStroke(1.dp, if (!settings.offlineMode) NeonEmerald else ObsidianBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.setOfflineMode(false) }
                                .testTag("mode_online_option")
                        ) {
                            Text(
                                text = strings.onlineLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (!settings.offlineMode) NeonEmerald else TextSecondary,
                                fontWeight = FontWeight.Bold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(12.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (settings.offlineMode) NeonAmber.copy(alpha = 0.2f) else ObsidianElevated,
                            border = BorderStroke(1.dp, if (settings.offlineMode) NeonAmber else ObsidianBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.setOfflineMode(true) }
                                .testTag("mode_offline_option")
                        ) {
                            Text(
                                text = strings.offlineLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (settings.offlineMode) NeonAmber else TextSecondary,
                                fontWeight = FontWeight.Bold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }

        // Background Video Talk Setting (Off / On)
        item {
            SettingToggleCard(
                title = strings.backgroundVideoTalkTitle,
                description = strings.backgroundVideoTalkDesc,
                icon = Icons.Filled.Videocam,
                checked = settings.backgroundVideoTalk,
                testTag = "setting_video_talk_switch",
                onCheckedChange = { enable ->
                    if (enable) {
                        val hasCam = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                        val hasMic = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasCam && hasMic) {
                            viewModel.setBackgroundVideoTalk(true)
                        } else {
                            cameraPermissionLauncher.launch(
                                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                            )
                        }
                    } else {
                        viewModel.setBackgroundVideoTalk(false)
                    }
                }
            )
        }

        // Screen-Off Fast Wake Setting ("MC")
        item {
            SectionTerminalCard(borderColor = if (settings.screenOffFastWake) NeonCyan else ObsidianBorder) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.RecordVoiceOver, contentDescription = null, tint = NeonCyan)
                            Column {
                                Text(
                                    text = strings.screenOffWakeTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = strings.screenOffWakeDesc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                        Switch(
                            checked = settings.screenOffFastWake,
                            onCheckedChange = { enable ->
                                if (enable) {
                                    val perms = buildList {
                                        add(Manifest.permission.RECORD_AUDIO)
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            add(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    }.toTypedArray()
                                    wakePermissionsLauncher.launch(perms)
                                } else {
                                    viewModel.setScreenOffFastWake(false)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ObsidianVoid,
                                checkedTrackColor = NeonCyan
                            ),
                            modifier = Modifier.testTag("setting_screen_off_wake_switch")
                        )
                    }

                    Button(
                        onClick = { viewModel.triggerMcWakeResponse() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            contentColor = ObsidianVoid
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Test Fast Wake: \"MC\" → \"${strings.mcGreetingWake}\"",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Auto-Speak TTS Toggle
        item {
            SettingToggleCard(
                title = strings.autoSpeakTitle,
                description = strings.autoSpeakDesc,
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                checked = settings.autoSpeakReplies,
                testTag = "setting_tts_switch",
                onCheckedChange = { viewModel.setAutoSpeakReplies(it) }
            )
        }

        // GitHub Profile Handle
        item {
            SectionTerminalCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = strings.githubUsernameLabel,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = githubDraft,
                            onValueChange = { githubDraft = it },
                            placeholder = { Text("e.g. your-github-handle") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = ObsidianBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = { viewModel.setGithubUsername(githubDraft) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }

        // System Permissions & Bridges Status
        item {
            SectionTerminalCard {
                val notifGranted = McNotificationListenerService.isNotificationAccessEnabled(context)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.LockOpen, contentDescription = null, tint = NeonEmerald)
                        Text(
                            text = "Device Permissions & Bridges",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
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
                            border = BorderStroke(1.dp, if (notifGranted) NeonEmerald else NeonAmber),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (notifGranted) "Notifications: ON" else "Enable Notifications",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (notifGranted) NeonEmerald else NeonAmber
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                viewModel.deviceControl.openSystemPanel(SystemPanelType.ACCESSIBILITY)
                            },
                            border = BorderStroke(1.dp, NeonCyan),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Accessibility Control",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonCyan
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingToggleCard(
    title: String,
    description: String,
    icon: ImageVector,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    SectionTerminalCard(borderColor = if (checked) NeonEmerald.copy(alpha = 0.6f) else ObsidianBorder) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(icon, contentDescription = null, tint = if (checked) NeonEmerald else TextSecondary)
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ObsidianVoid,
                    checkedTrackColor = NeonEmerald
                ),
                modifier = Modifier.testTag(testTag)
            )
        }
    }
}
