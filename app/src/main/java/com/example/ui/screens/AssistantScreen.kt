package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.AppLanguage
import com.example.core.McLocalization
import com.example.data.model.AssistantMessage
import com.example.data.model.MessageSender
import com.example.ui.components.NeonPulseOrb
import com.example.ui.components.StatusPill
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCoral
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AiResponseState
import com.example.ui.viewmodel.McViewModel
import java.util.concurrent.Executors

@Composable
fun AssistantScreen(
    viewModel: McViewModel,
    onNavigateToCrypto: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isOnline by viewModel.isEffectiveOnline.collectAsStateWithLifecycle()
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val aiState by viewModel.aiState.collectAsStateWithLifecycle()
    val isListening by viewModel.voiceEngine.isListening.collectAsStateWithLifecycle()
    val wakeBannerVisible by viewModel.wakeBannerVisible.collectAsStateWithLifecycle()
    val btc5m by viewModel.btc5mLive.collectAsStateWithLifecycle()

    val strings = remember(settings.language) { McLocalization.get(settings.language) }
    var inputText by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.voiceEngine.startListening(continuousWake = false)
        }
    }

    val camPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.setBackgroundVideoTalk(true)
        }
    }

    LaunchedEffect(messages.size, aiState) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianVoid)
    ) {
        // Header Hub
        Surface(
            color = ObsidianSurface,
            border = BorderStroke(1.dp, ObsidianBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NeonPulseOrb(
                            isListening = isListening,
                            isWaking = wakeBannerVisible,
                            size = 56.dp,
                            onClick = {
                                if (ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    if (isListening) {
                                        viewModel.voiceEngine.stopListening()
                                    } else {
                                        viewModel.voiceEngine.startListening(continuousWake = false)
                                    }
                                } else {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "MC PERSONAL AI",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                            Text(
                                text = if (isOnline) strings.mcSubtitleOnline else strings.mcSubtitleOffline,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isOnline) NeonCyan else NeonAmber
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatusPill(
                            label = if (isOnline) strings.onlineLabel else strings.offlineLabel,
                            accentColor = if (isOnline) NeonEmerald else NeonAmber,
                            onClick = { viewModel.setOfflineMode(!settings.offlineMode) },
                            modifier = Modifier.testTag("online_offline_pill")
                        )
                        StatusPill(
                            label = if (settings.language == AppLanguage.BANGLA) "বাংলা" else "EN",
                            accentColor = NeonViolet,
                            onClick = {
                                val next = if (settings.language == AppLanguage.ENGLISH) {
                                    AppLanguage.BANGLA
                                } else {
                                    AppLanguage.ENGLISH
                                }
                                viewModel.setLanguage(next)
                            },
                            modifier = Modifier.testTag("language_pill")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Call MC + Video Talk quick row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { viewModel.triggerMcWakeResponse() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            contentColor = ObsidianVoid
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("call_mc_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.RecordVoiceOver,
                            contentDescription = "Call MC",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.callMcButton,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (settings.backgroundVideoTalk) NeonEmerald.copy(alpha = 0.16f) else ObsidianCard,
                        border = BorderStroke(
                            1.dp,
                            if (settings.backgroundVideoTalk) NeonEmerald else ObsidianBorder
                        ),
                        modifier = Modifier
                            .clickable {
                                val hasCam = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                                if (!settings.backgroundVideoTalk && !hasCam) {
                                    camPermissionLauncher.launch(Manifest.permission.CAMERA)
                                } else {
                                    viewModel.setBackgroundVideoTalk(!settings.backgroundVideoTalk)
                                }
                            }
                            .testTag("video_talk_toggle_chip")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (settings.backgroundVideoTalk) {
                                    Icons.Filled.Videocam
                                } else {
                                    Icons.Filled.VideocamOff
                                },
                                contentDescription = "Background Video Talk",
                                tint = if (settings.backgroundVideoTalk) NeonEmerald else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (settings.backgroundVideoTalk) strings.videoTalkActive else strings.videoTalkOff,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (settings.backgroundVideoTalk) NeonEmerald else TextSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.clearChatHistory() },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ObsidianCard)
                            .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
                            .testTag("clear_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Clear Chat",
                            tint = TextSecondary
                        )
                    }
                }

                // Live BTC 5m Ticker Strip
                btc5m?.let { btc ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ObsidianCard,
                        border = BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onNavigateToCrypto)
                            .testTag("assistant_btc_strip")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "BTC 5m $${String.format("%,.2f", btc.priceUsd)}",
                                style = MaterialTheme.typography.labelLarge,
                                color = TextPrimary
                            )
                            Text(
                                text = "Buy ${btc.buyerPercent}% • Sell ${btc.sellerPercent}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = NeonCyan
                            )
                            Text(
                                text = if (btc.direction.name == "UP") "UP ▲" else "DOWN ▼",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (btc.direction.name == "UP") NeonEmerald else NeonCoral,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Background Video Talk CameraX Feed when enabled
        AnimatedVisibility(visible = settings.backgroundVideoTalk) {
            VideoTalkCameraBar(
                onFrameCaptured = { bmp -> viewModel.updateCameraFrame(bmp) },
                onClose = { viewModel.setBackgroundVideoTalk(false) }
            )
        }

        // Wake Banner ("Yes, Boss! How can I help you?")
        AnimatedVisibility(visible = wakeBannerVisible) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(NeonCyan.copy(alpha = 0.22f), NeonEmerald.copy(alpha = 0.22f))
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "MC: \"${strings.mcGreetingWake}\"",
                    style = MaterialTheme.typography.titleMedium,
                    color = NeonEmerald,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Chat List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                ChatBubbleItem(
                    message = msg,
                    onCopy = { viewModel.deviceControl.copyToClipboard("MC Message", msg.text) },
                    onShare = { viewModel.deviceControl.shareText("MC Assistant", msg.text) },
                    onSpeak = { viewModel.voiceEngine.speak(msg.text) }
                )
            }

            when (val state = aiState) {
                is AiResponseState.Loading -> {
                    item {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = NeonCyan
                            )
                            Text(
                                text = if (settings.language == AppLanguage.BANGLA) {
                                    "এমসি ভাবছে..."
                                } else {
                                    "MC Neural Core thinking..."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = NeonCyan
                            )
                        }
                    }
                }
                is AiResponseState.Streaming -> {
                    item {
                        ChatBubbleItem(
                            message = AssistantMessage(
                                sender = MessageSender.MC,
                                text = state.text
                            ),
                            onCopy = {},
                            onShare = {},
                            onSpeak = {}
                        )
                    }
                }
                is AiResponseState.Error -> {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = NeonCoral.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, NeonCoral)
                        ) {
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = NeonCoral,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
                else -> {}
            }
        }

        // Quick Prompts Row
        val quickPrompts = if (settings.language == AppLanguage.BANGLA) {
            listOf(
                "MC",
                "মার্কেট বিশ্লেষণ করো",
                "বিটকয়েন ৫মি আপ না ডাউন?",
                "ফ্ল্যাশলাইট অন",
                "ওয়াই-ফাই খোলো"
            )
        } else {
            listOf(
                "MC",
                "Analyze live crypto market",
                "BTC 5m buyer seller %",
                "Open Wi-Fi",
                "Share market"
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickPrompts.forEach { chip ->
                Surface(
                    shape = RoundedCornerShape(50),
                    color = ObsidianCard,
                    border = BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier.clickable { viewModel.sendUserPrompt(chip) }
                ) {
                    Text(
                        text = chip,
                        style = MaterialTheme.typography.labelMedium,
                        color = NeonCyan,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }
        }

        // Bottom Input Bar
        Surface(
            color = ObsidianSurface,
            border = BorderStroke(1.dp, ObsidianBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            if (isListening) {
                                viewModel.voiceEngine.stopListening()
                            } else {
                                viewModel.voiceEngine.startListening(continuousWake = false)
                            }
                        } else {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (isListening) NeonCoral else ObsidianElevated)
                        .testTag("mic_input_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = "Speak to MC",
                        tint = if (isListening) ObsidianVoid else NeonCyan
                    )
                }

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            text = strings.askMcPlaceholder,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = ObsidianBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = NeonCyan
                    ),
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (inputText.isNotBlank()) {
                                viewModel.sendUserPrompt(inputText)
                                inputText = ""
                            }
                        }
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_prompt_input")
                )

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendUserPrompt(inputText)
                            inputText = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(NeonCyan)
                        .testTag("send_prompt_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = strings.sendLabel,
                        tint = ObsidianVoid
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatBubbleItem(
    message: AssistantMessage,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onSpeak: () -> Unit
) {
    val isUser = message.sender == MessageSender.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) NeonCyan.copy(alpha = 0.16f) else ObsidianCard,
            border = BorderStroke(
                1.dp,
                if (isUser) NeonCyan.copy(alpha = 0.55f) else ObsidianBorder
            ),
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isUser) "BOSS" else if (message.isOfflineReply) "MC • OFFLINE" else "MC • LIVE AI",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isUser) NeonCyan else NeonEmerald,
                        fontWeight = FontWeight.Bold
                    )

                    if (!isUser) {
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            IconButton(onClick = onSpeak, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Speak reply",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = "Copy reply",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(onClick = onShare, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    imageVector = Icons.Filled.Share,
                                    contentDescription = "Share reply",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary
                )
            }
        }
    }
}

@Composable
private fun VideoTalkCameraBar(
    onFrameCaptured: (Bitmap) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    var lastFrameTime by remember { mutableStateOf(0L) }

    DisposableEffect(Unit) {
        onDispose {
            executor.shutdown()
        }
    }

    Surface(
        color = ObsidianElevated,
        border = BorderStroke(1.dp, NeonEmerald.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 88.dp, height = 66.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, NeonEmerald, RoundedCornerShape(10.dp))
            ) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            runCatching {
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.surfaceProvider = previewView.surfaceProvider
                                }
                                val analysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()
                                    .also { imageAnalysis ->
                                        imageAnalysis.setAnalyzer(executor) { proxy ->
                                            val now = System.currentTimeMillis()
                                            if (now - lastFrameTime > 1500L) {
                                                lastFrameTime = now
                                                val bmp = proxy.toBitmap()
                                                val matrix = Matrix().apply {
                                                    postRotate(proxy.imageInfo.rotationDegrees.toFloat())
                                                }
                                                val rotated = Bitmap.createBitmap(
                                                    bmp, 0, 0, bmp.width, bmp.height, matrix, true
                                                )
                                                onFrameCaptured(rotated)
                                            }
                                            proxy.close()
                                        }
                                    }
                                cameraProvider.unbindAll()
                                val selector = if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                                    CameraSelector.DEFAULT_FRONT_CAMERA
                                } else {
                                    CameraSelector.DEFAULT_BACK_CAMERA
                                }
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    selector,
                                    preview,
                                    analysis
                                )
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "BACKGROUND VIDEO TALK ACTIVE",
                    style = MaterialTheme.typography.labelMedium,
                    color = NeonEmerald,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "MC sees your live camera frames when you ask questions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCoral,
                    contentColor = TextPrimary
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("OFF", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
