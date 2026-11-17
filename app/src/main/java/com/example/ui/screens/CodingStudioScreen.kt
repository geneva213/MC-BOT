package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.McLocalization
import com.example.ui.components.SectionTerminalCard
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeonCoral
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
fun CodingStudioScreen(
    viewModel: McViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val codeText by viewModel.generatedCode.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isCodeGenerating.collectAsStateWithLifecycle()
    val codeError by viewModel.codeError.collectAsStateWithLifecycle()
    val snippets by viewModel.savedSnippets.collectAsStateWithLifecycle()
    val strings = remember(settings.language) { McLocalization.get(settings.language) }

    val stacks = listOf("Kotlin Compose", "Android APK Project", "Python", "JavaScript", "GitHub CI YAML")
    var selectedStack by rememberSaveable { mutableStateOf(stacks.first()) }
    var promptInput by rememberSaveable { mutableStateOf("") }
    var projectTitle by rememberSaveable { mutableStateOf("McApp") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianVoid),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = strings.codingStudioTitle,
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary
            )
        }

        // Stack Chips + Code Generator Prompt
        item {
            SectionTerminalCard(borderColor = NeonViolet.copy(alpha = 0.55f)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        stacks.forEach { stack ->
                            val selected = stack == selectedStack
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (selected) NeonViolet else ObsidianElevated,
                                border = BorderStroke(1.dp, if (selected) NeonViolet else ObsidianBorder),
                                modifier = Modifier.clickable { selectedStack = stack }
                            ) {
                                Text(
                                    text = stack,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) TextPrimary else TextSecondary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = projectTitle,
                        onValueChange = { projectTitle = it },
                        label = { Text("Project / File Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = ObsidianBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = promptInput,
                        onValueChange = { promptInput = it },
                        placeholder = { Text("Describe the Android screen, app feature, or script to code...") },
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonViolet,
                            unfocusedBorderColor = ObsidianBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("code_prompt_input")
                    )

                    Button(
                        onClick = { viewModel.generateCode(promptInput, selectedStack) },
                        enabled = !isGenerating && promptInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonViolet,
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("generate_code_button")
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = TextPrimary
                            )
                        } else {
                            Icon(Icons.Filled.Code, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(strings.generateCodeButton, fontWeight = FontWeight.Bold)
                    }

                    if (codeError != null) {
                        Text(
                            text = codeError.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = NeonCoral
                        )
                    }
                }
            }
        }

        // Editable Code Terminal + GitHub & APK Export Bar
        item {
            SectionTerminalCard(borderColor = NeonCyan.copy(alpha = 0.5f)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CODE WORKBENCH",
                            style = MaterialTheme.typography.labelLarge,
                            color = NeonCyan
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = {
                                    viewModel.deviceControl.copyToClipboard("MC Code", codeText)
                                }
                            ) {
                                Icon(
                                    Icons.Filled.ContentCopy,
                                    contentDescription = strings.copyCodeButton,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    viewModel.shareSingleCodeFile("${projectTitle}.kt")
                                }
                            ) {
                                Icon(
                                    Icons.Filled.Share,
                                    contentDescription = "Share file",
                                    tint = NeonEmerald,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = codeText,
                        onValueChange = { viewModel.updateCodeText(it) },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            color = NeonEmerald
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ObsidianVoid,
                            unfocusedContainerColor = ObsidianVoid,
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = ObsidianBorder
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp)
                            .testTag("code_editor_field")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.shareCodeToGithub() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonCyan,
                                contentColor = ObsidianVoid
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("share_github_button")
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.shareGithubButton, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.exportAndroidApkZip(projectTitle) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonEmerald,
                                contentColor = ObsidianVoid
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_apk_zip_button")
                        ) {
                            Icon(Icons.Filled.Archive, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.exportApkZipButton, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // APK Build & GitHub Workflow Guide
        item {
            SectionTerminalCard {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Build, contentDescription = null, tint = NeonEmerald)
                        Text(
                            text = "GitHub Push & Android APK Build Pipeline",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = "Exporting the Project Zip includes MainActivity.kt, build.gradle.kts, and a ready .github/workflows/build_apk.yml GitHub Action that compiles and outputs app-debug.apk automatically on push.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    OutlinedButton(
                        onClick = {
                            viewModel.deviceControl.openUrl("https://github.com/new")
                        },
                        border = BorderStroke(1.dp, ObsidianBorder),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Open GitHub New Repo", style = MaterialTheme.typography.labelMedium, color = NeonCyan)
                    }
                }
            }
        }

        // Saved Code Snippets
        if (snippets.isNotEmpty()) {
            item {
                Text(
                    text = "Saved Code Library (${snippets.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            }
            items(snippets, key = { it.id }) { snippet ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ObsidianCard,
                    border = BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.loadSnippet(snippet) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = snippet.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                            Text(
                                text = snippet.language,
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonViolet
                            )
                        }
                        IconButton(onClick = { viewModel.deleteSnippet(snippet.id) }) {
                            Icon(
                                Icons.Filled.DeleteOutline,
                                contentDescription = "Delete snippet",
                                tint = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}
