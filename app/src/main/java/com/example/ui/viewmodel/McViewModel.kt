package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.AppLanguage
import com.example.core.DeviceControlManager
import com.example.core.DeviceNotificationItem
import com.example.core.McLocalization
import com.example.core.McNotificationListenerService
import com.example.core.McVoiceEngine
import com.example.core.McWakeService
import com.example.core.NetworkMonitor
import com.example.core.SystemPanelType
import com.example.data.local.ChatMessageEntity
import com.example.data.local.CodeSnippetEntity
import com.example.data.local.McDatabase
import com.example.data.local.McSettings
import com.example.data.local.SettingsDataStore
import com.example.data.model.AssistantMessage
import com.example.data.model.CryptoAsset
import com.example.data.model.CryptoTickerAnalysis
import com.example.data.model.CryptoTimeframe
import com.example.data.model.MarketDirection
import com.example.data.model.MessageSender
import com.example.data.model.SavedCodeSnippet
import com.example.data.remote.CryptoMarketService
import com.example.data.repository.CryptoRepository
import com.example.data.repository.McAiRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

private const val TAG = "McViewModel"

sealed interface AiResponseState {
    data object Idle : AiResponseState
    data object Loading : AiResponseState
    data class Streaming(val text: String) : AiResponseState
    data class Error(val message: String) : AiResponseState
}

sealed interface CryptoUiState {
    data object Loading : CryptoUiState
    data class Ready(val tickers: List<CryptoTickerAnalysis>) : CryptoUiState
    data class Error(val message: String) : CryptoUiState
}

class McViewModel(application: Application) : AndroidViewModel(application) {

    private val db = McDatabase.getInstance(application)
    private val settingsStore = SettingsDataStore(application)
    private val networkMonitor = NetworkMonitor(application)
    private val cryptoRepository = CryptoRepository(CryptoMarketService(), db.cryptoSnapshotDao())
    private val aiRepository = McAiRepository()
    val deviceControl = DeviceControlManager(application)
    val voiceEngine = McVoiceEngine(application)

    val settings: StateFlow<McSettings> = settingsStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, McSettings())

    val isNetworkAvailable: StateFlow<Boolean> = networkMonitor.isConnected

    val isEffectiveOnline: StateFlow<Boolean> = combine(settings, isNetworkAvailable) { s, net ->
        !s.offlineMode && net
    }.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val chatMessages: StateFlow<List<AssistantMessage>> = db.chatMessageDao()
        .observeAll()
        .map { list ->
            list.map {
                AssistantMessage(
                    id = it.id,
                    sender = if (it.sender == MessageSender.USER.name) MessageSender.USER else MessageSender.MC,
                    text = it.text,
                    timestamp = it.timestamp,
                    isOfflineReply = it.isOfflineReply
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedSnippets: StateFlow<List<SavedCodeSnippet>> = db.codeSnippetDao()
        .observeAll()
        .map { list ->
            list.map {
                SavedCodeSnippet(
                    id = it.id,
                    title = it.title,
                    language = it.language,
                    code = it.code,
                    timestamp = it.timestamp
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deviceNotifications: StateFlow<List<DeviceNotificationItem>> =
        McNotificationListenerService.notifications

    val isNotificationAccessActive: StateFlow<Boolean> =
        McNotificationListenerService.isListenerConnected

    private val _selectedTimeframe = MutableStateFlow(CryptoTimeframe.M5)
    val selectedTimeframe: StateFlow<CryptoTimeframe> = _selectedTimeframe.asStateFlow()

    private val _cryptoState = MutableStateFlow<CryptoUiState>(CryptoUiState.Loading)
    val cryptoState: StateFlow<CryptoUiState> = _cryptoState.asStateFlow()

    private val _btc5mLive = MutableStateFlow<CryptoTickerAnalysis?>(null)
    val btc5mLive: StateFlow<CryptoTickerAnalysis?> = _btc5mLive.asStateFlow()

    private val _aiState = MutableStateFlow<AiResponseState>(AiResponseState.Idle)
    val aiState: StateFlow<AiResponseState> = _aiState.asStateFlow()

    private val _marketAnalysisPopup = MutableStateFlow<String?>(null)
    val marketAnalysisPopup: StateFlow<String?> = _marketAnalysisPopup.asStateFlow()

    private val _isMarketAnalyzing = MutableStateFlow(false)
    val isMarketAnalyzing: StateFlow<Boolean> = _isMarketAnalyzing.asStateFlow()

    private val _generatedCode = MutableStateFlow(defaultStarterKotlinCode())
    val generatedCode: StateFlow<String> = _generatedCode.asStateFlow()

    private val _isCodeGenerating = MutableStateFlow(false)
    val isCodeGenerating: StateFlow<Boolean> = _isCodeGenerating.asStateFlow()

    private val _codeError = MutableStateFlow<String?>(null)
    val codeError: StateFlow<String?> = _codeError.asStateFlow()

    private val _isFlashlightOn = MutableStateFlow(false)
    val isFlashlightOn: StateFlow<Boolean> = _isFlashlightOn.asStateFlow()

    private val _latestCameraBitmap = MutableStateFlow<Bitmap?>(null)
    val latestCameraBitmap: StateFlow<Bitmap?> = _latestCameraBitmap.asStateFlow()

    private val _wakeBannerVisible = MutableStateFlow(false)
    val wakeBannerVisible: StateFlow<Boolean> = _wakeBannerVisible.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        voiceEngine.onWakeWordDetected = {
            triggerMcWakeResponse()
        }
        voiceEngine.onCommandRecognized = { spoken ->
            sendUserPrompt(spoken)
        }

        viewModelScope.launch {
            settings.collect { s ->
                voiceEngine.setLanguage(s.language)
            }
        }

        // Seed welcome message if empty
        viewModelScope.launch {
            delay(350)
            if (chatMessages.value.isEmpty()) {
                val strings = McLocalization.get(settings.value.language)
                insertMcMessage(
                    text = "${strings.mcGreetingWake}\n\nMC Core initialized. Call \"MC\" anytime, ask for live 5m/15m/1h crypto Bull/Bear analysis, device control, or Android & GitHub coding.",
                    isOffline = !isEffectiveOnline.value
                )
            }
        }

        // Live 12-second crypto auto-polling loop
        viewModelScope.launch {
            while (isActive) {
                refreshCryptoMarket(showLoader = _cryptoState.value is CryptoUiState.Loading)
                delay(12_000L)
            }
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun dismissMarketAnalysisPopup() {
        _marketAnalysisPopup.value = null
    }

    fun updateCameraFrame(bitmap: Bitmap) {
        _latestCameraBitmap.value = bitmap
    }

    fun triggerMcWakeResponse() {
        val currentSettings = settings.value
        val strings = McLocalization.get(currentSettings.language)
        _wakeBannerVisible.value = true
        voiceEngine.speak(strings.mcGreetingWake)

        viewModelScope.launch {
            insertUserMessage("MC")
            insertMcMessage(strings.mcGreetingWake, isOffline = !isEffectiveOnline.value)
            delay(4000)
            _wakeBannerVisible.value = false
        }
    }

    fun selectTimeframe(tf: CryptoTimeframe) {
        _selectedTimeframe.value = tf
        viewModelScope.launch {
            refreshCryptoMarket(showLoader = true)
        }
    }

    fun refreshCryptoMarket(showLoader: Boolean = true) {
        viewModelScope.launch {
            if (showLoader) {
                _cryptoState.value = CryptoUiState.Loading
            }
            val offline = !isEffectiveOnline.value
            val btc5m = cryptoRepository.fetchBitcoin5mLive(offline)
            if (btc5m != null) {
                _btc5mLive.value = btc5m
            }

            val result = cryptoRepository.fetchLiveMarket(_selectedTimeframe.value, offline)
            result.onSuccess { list ->
                _cryptoState.value = CryptoUiState.Ready(list)
                if (_selectedTimeframe.value == CryptoTimeframe.M5) {
                    list.firstOrNull { it.asset == CryptoAsset.BTC }?.let { _btc5mLive.value = it }
                }
            }.onFailure { err ->
                if (_cryptoState.value !is CryptoUiState.Ready) {
                    _cryptoState.value = CryptoUiState.Error(
                        err.localizedMessage ?: "Failed to load market data."
                    )
                }
            }
        }
    }

    fun sendUserPrompt(rawPrompt: String) {
        val prompt = rawPrompt.trim()
        if (prompt.isEmpty()) return

        if (voiceEngine.isMcWakePhrase(prompt) &&
            prompt.split(Regex("\\s+")).size <= 2 &&
            !prompt.lowercase(Locale.US).contains("code") &&
            !prompt.lowercase(Locale.US).contains("market")
        ) {
            triggerMcWakeResponse()
            return
        }

        viewModelScope.launch {
            insertUserMessage(prompt)
            val currentSettings = settings.value
            val online = isEffectiveOnline.value

            // Check if prompt is a direct device control command
            val deviceHandled = tryHandleDeviceCommand(prompt, currentSettings.language)
            if (deviceHandled != null) {
                insertMcMessage(deviceHandled, isOffline = true)
                if (currentSettings.autoSpeakReplies) {
                    voiceEngine.speak(deviceHandled)
                }
                return@launch
            }

            // Ensure fresh crypto context if market-related
            val lower = prompt.lowercase(Locale.US)
            val isMarketQuery = lower.contains("market") || lower.contains("crypto") ||
                lower.contains("btc") || lower.contains("bitcoin") || lower.contains("eth") ||
                lower.contains("xrp") || lower.contains("bnb") || lower.contains("doge") ||
                lower.contains("sol") || lower.contains("bull") || lower.contains("bear") ||
                lower.contains("buyer") || lower.contains("seller") || lower.contains("signal") ||
                lower.contains("মার্কেট") || lower.contains("বিটকয়েন") || lower.contains("ক্রিপ্টো")

            if (isMarketQuery && online) {
                refreshCryptoMarket(showLoader = false)
            }

            val marketSnapshot = (_cryptoState.value as? CryptoUiState.Ready)?.tickers.orEmpty()
            val frame = if (currentSettings.backgroundVideoTalk) _latestCameraBitmap.value else null

            _aiState.value = AiResponseState.Loading
            var finalReply = ""
            try {
                aiRepository.streamAssistantResponse(
                    prompt = prompt,
                    language = currentSettings.language,
                    isOnline = online,
                    marketContext = marketSnapshot,
                    cameraFrame = frame
                ).collect { partial ->
                    finalReply = partial
                    _aiState.value = AiResponseState.Streaming(partial)
                }
                _aiState.value = AiResponseState.Idle
                if (finalReply.isNotBlank()) {
                    insertMcMessage(finalReply, isOffline = !online)
                    if (currentSettings.autoSpeakReplies) {
                        voiceEngine.speak(finalReply)
                    }
                }
            } catch (e: TimeoutCancellationException) {
                val msg = "Boss, Gemini AI timed out after 60s. Please retry."
                _aiState.value = AiResponseState.Error(msg)
                insertMcMessage(msg, isOffline = !online)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI stream failed", e)
                val msg = e.localizedMessage ?: "AI connection error."
                _aiState.value = AiResponseState.Error(msg)
                insertMcMessage("Boss, AI request error: $msg", isOffline = !online)
            }
        }
    }

    fun analyzeMarketWithMc(specificAsset: CryptoTickerAnalysis? = null) {
        viewModelScope.launch {
            _isMarketAnalyzing.value = true
            val currentSettings = settings.value
            val online = isEffectiveOnline.value
            val tickers = if (specificAsset != null) {
                listOf(specificAsset)
            } else {
                (_cryptoState.value as? CryptoUiState.Ready)?.tickers
                    ?: listOfNotNull(_btc5mLive.value)
            }

            val prompt = if (specificAsset != null) {
                "Analyze ${specificAsset.asset.displayName} (${specificAsset.asset.symbol}) on the ${specificAsset.timeframe.apiInterval} timeframe. " +
                    "State clearly: Buyers ${specificAsset.buyerPercent}% vs Sellers ${specificAsset.sellerPercent}%, " +
                    "current price $${specificAsset.priceUsd}, RSI ${String.format(Locale.US, "%.1f", specificAsset.rsi14)}, " +
                    "and clearly tell Boss if the market direction is UP or DOWN and why."
            } else {
                "Provide a crisp live crypto market analysis for Boss covering every asset in the snapshot (BTC, ETH, XRP, BNB, DOGE, SOL). " +
                    "For each coin, state the Buyer % vs Seller % and clearly say UP or DOWN."
            }

            val accumulated = StringBuilder()
            try {
                aiRepository.streamAssistantResponse(
                    prompt = prompt,
                    language = currentSettings.language,
                    isOnline = online,
                    marketContext = tickers
                ).collect { chunk ->
                    accumulated.clear()
                    accumulated.append(chunk)
                    _marketAnalysisPopup.value = chunk
                }
                if (currentSettings.autoSpeakReplies && accumulated.isNotBlank()) {
                    voiceEngine.speak(accumulated.toString())
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _marketAnalysisPopup.value = "Analysis error: ${e.localizedMessage ?: "Unknown error"}"
            } finally {
                _isMarketAnalyzing.value = false
            }
        }
    }

    fun shareCryptoAnalysis(ticker: CryptoTickerAnalysis) {
        val dir = if (ticker.direction == MarketDirection.UP) "UP ▲ (BULLISH)" else "DOWN ▼ (BEARISH)"
        val report = buildString {
            appendLine("MC LIVE CRYPTO SIGNAL [${ticker.timeframe.apiInterval.uppercase(Locale.US)}]")
            appendLine("${ticker.asset.displayName} (${ticker.asset.symbol}/USD): $${formatPrice(ticker.priceUsd)}")
            appendLine("24h Change: ${String.format(Locale.US, "%+.2f%%", ticker.change24hPercent)}")
            appendLine("Buyers (Bull): ${ticker.buyerPercent}% | Sellers (Bear): ${ticker.sellerPercent}%")
            appendLine("Signal Direction: $dir (Confidence ${ticker.confidencePercent}%)")
            appendLine("RSI-14: ${String.format(Locale.US, "%.1f", ticker.rsi14)} | Source: ${ticker.dataSource}")
        }
        deviceControl.shareText("${ticker.asset.symbol} Live Signal", report)
    }

    fun shareAllMarketSnapshot() {
        val tickers = (_cryptoState.value as? CryptoUiState.Ready)?.tickers.orEmpty()
        if (tickers.isEmpty()) return
        val report = buildString {
            appendLine("MC LIVE CRYPTO MARKET (${_selectedTimeframe.value.apiInterval.uppercase(Locale.US)})")
            tickers.forEach { t ->
                val arrow = if (t.direction == MarketDirection.UP) "UP ▲" else "DOWN ▼"
                appendLine(
                    "${t.asset.symbol}: $${formatPrice(t.priceUsd)} | Buyers ${t.buyerPercent}% / Sellers ${t.sellerPercent}% → $arrow"
                )
            }
        }
        deviceControl.shareText("MC Live Crypto Market", report)
    }

    fun generateCode(prompt: String, stack: String) {
        if (prompt.isBlank()) return
        viewModelScope.launch {
            _isCodeGenerating.value = true
            _codeError.value = null
            try {
                val raw = aiRepository.generateCode(
                    request = prompt,
                    targetStack = stack,
                    language = settings.value.language,
                    isOnline = isEffectiveOnline.value
                )
                val extracted = extractPrimaryCodeBlock(raw)
                _generatedCode.value = extracted
                db.codeSnippetDao().insert(
                    CodeSnippetEntity(
                        title = prompt.take(40),
                        language = stack,
                        code = extracted,
                        timestamp = System.currentTimeMillis()
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _codeError.value = e.localizedMessage ?: "Code generation failed."
            } finally {
                _isCodeGenerating.value = false
            }
        }
    }

    fun loadSnippet(snippet: SavedCodeSnippet) {
        _generatedCode.value = snippet.code
    }

    fun deleteSnippet(id: Long) {
        viewModelScope.launch {
            db.codeSnippetDao().deleteById(id)
        }
    }

    fun updateCodeText(newCode: String) {
        _generatedCode.value = newCode
    }

    fun shareCodeToGithub() {
        val username = settings.value.githubUsername
        deviceControl.copyToClipboard("MC Generated Code", _generatedCode.value)
        _toastMessage.value = "Code copied! Opening GitHub Gist / Repo share..."
        val url = if (username.isNotBlank()) {
            "https://gist.github.com/"
        } else {
            "https://github.com/new"
        }
        deviceControl.openUrl(url)
    }

    fun exportAndroidApkZip(projectTitle: String) {
        deviceControl.shareAndroidProjectZip(
            projectName = projectTitle.ifBlank { "McAndroidApp" },
            codeContent = _generatedCode.value,
            githubUser = settings.value.githubUsername
        )
    }

    fun shareSingleCodeFile(fileName: String) {
        deviceControl.shareCodeFile(fileName, _generatedCode.value)
    }

    fun toggleFlashlight() {
        val next = !_isFlashlightOn.value
        if (deviceControl.toggleFlashlight(next)) {
            _isFlashlightOn.value = next
        } else {
            _toastMessage.value = "Flashlight unavailable on this device"
        }
    }

    fun shareNotificationsSummary() {
        val items = deviceNotifications.value
        val body = if (items.isEmpty()) {
            "No active notifications captured. Ensure MC Notification Bridge access is enabled in Android Settings."
        } else {
            buildString {
                appendLine("MC Device Notifications Summary:")
                items.forEach { n ->
                    appendLine("• [${n.appPackage}] ${n.title}: ${n.text}")
                }
            }
        }
        deviceControl.shareText("MC Notifications Summary", body)
    }

    // Settings updates
    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            settingsStore.setLanguage(language)
            if (settings.value.screenOffFastWake) {
                McWakeService.start(getApplication(), language, settings.value.backgroundVideoTalk)
            }
        }
    }

    fun setBackgroundVideoTalk(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setBackgroundVideoTalk(enabled)
            if (settings.value.screenOffFastWake) {
                McWakeService.start(getApplication(), settings.value.language, enabled)
            }
        }
    }

    fun setScreenOffFastWake(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setScreenOffFastWake(enabled)
            if (enabled) {
                McWakeService.start(getApplication(), settings.value.language, settings.value.backgroundVideoTalk)
            } else {
                McWakeService.stop(getApplication())
            }
        }
    }

    fun setOfflineMode(offline: Boolean) {
        viewModelScope.launch {
            settingsStore.setOfflineMode(offline)
            refreshCryptoMarket(showLoader = true)
        }
    }

    fun setAutoSpeakReplies(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setAutoSpeakReplies(enabled)
            if (!enabled) voiceEngine.stopSpeaking()
        }
    }

    fun setGithubUsername(username: String) {
        viewModelScope.launch {
            settingsStore.setGithubUsername(username)
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            db.chatMessageDao().clearAll()
        }
    }

    private fun tryHandleDeviceCommand(prompt: String, language: AppLanguage): String? {
        val lower = prompt.lowercase(Locale.US)
        val isBn = language == AppLanguage.BANGLA

        // Call number command
        val phoneMatch = Regex("(\\+?[0-9][0-9\\-\\s]{5,}[0-9])").find(prompt)
        if ((lower.startsWith("call ") || lower.contains("dial ") || lower.contains("কল")) && phoneMatch != null) {
            val number = phoneMatch.value.replace("\\s".toRegex(), "")
            deviceControl.dialNumber(number)
            return if (isBn) "বস, $number নম্বরে ডায়ালার ওপেন করা হয়েছে।" else "Yes Boss, opening dialer for $number."
        }

        // Message command
        if ((lower.startsWith("sms ") || lower.startsWith("message ") || lower.startsWith("text ")) && phoneMatch != null) {
            val number = phoneMatch.value.replace("\\s".toRegex(), "")
            val msgBody = prompt.substringAfter(phoneMatch.value).trim()
            deviceControl.sendSms(number, msgBody)
            return if (isBn) "বস, $number-এ মেসেজ কম্পোজার খোলা হয়েছে।" else "Yes Boss, opening SMS composer for $number."
        }

        if (lower.contains("flashlight on") || lower.contains("torch on") || lower.contains("টর্চ অন")) {
            deviceControl.toggleFlashlight(true)
            _isFlashlightOn.value = true
            return if (isBn) "বস, ফ্ল্যাশলাইট চালু করা হয়েছে।" else "Yes Boss, flashlight is now ON."
        }

        if (lower.contains("flashlight off") || lower.contains("torch off") || lower.contains("টর্চ অফ")) {
            deviceControl.toggleFlashlight(false)
            _isFlashlightOn.value = false
            return if (isBn) "বস, ফ্ল্যাশলাইট বন্ধ করা হয়েছে।" else "Yes Boss, flashlight is now OFF."
        }

        if (lower.contains("open wifi") || lower.contains("wi-fi")) {
            deviceControl.openSystemPanel(SystemPanelType.WIFI)
            return if (isBn) "বস, ওয়াই-ফাই প্যানেল খোলা হয়েছে।" else "Yes Boss, opening Wi-Fi panel."
        }

        if (lower.contains("open bluetooth") || lower.contains("bluetooth")) {
            deviceControl.openSystemPanel(SystemPanelType.BLUETOOTH)
            return if (isBn) "বস, ব্লুটুথ সেটিংস খোলা হয়েছে।" else "Yes Boss, opening Bluetooth settings."
        }

        if (lower.contains("share market") || lower.contains("share crypto") || lower.contains("মার্কেট শেয়ার")) {
            shareAllMarketSnapshot()
            return if (isBn) "বস, লাইভ ক্রিপ্টো মার্কেট শেয়ার শিট খোলা হয়েছে।" else "Yes Boss, sharing live crypto market report."
        }

        if (lower.contains("share notification") || lower.contains("read notification")) {
            val items = deviceNotifications.value
            if (items.isEmpty()) {
                return if (isBn) {
                    "বস, এখনো কোনো নোটিফিকেশন পাওয়া যায়নি। কন্ট্রোল ট্যাব থেকে Notification Access চালু করুন।"
                } else {
                    "Boss, no active notifications captured yet. Enable Notification Access in the Control tab."
                }
            }
            return buildString {
                appendLine(if (isBn) "বস, সাম্প্রতিক নোটিফিকেশন:" else "Boss, your active notifications:")
                items.take(5).forEach { n ->
                    appendLine("• [${n.appPackage}] ${n.title}: ${n.text}")
                }
            }
        }

        return null
    }

    private suspend fun insertUserMessage(text: String) {
        db.chatMessageDao().insert(
            ChatMessageEntity(
                sender = MessageSender.USER.name,
                text = text,
                timestamp = System.currentTimeMillis(),
                isOfflineReply = false
            )
        )
    }

    private suspend fun insertMcMessage(text: String, isOffline: Boolean) {
        db.chatMessageDao().insert(
            ChatMessageEntity(
                sender = MessageSender.MC.name,
                text = text,
                timestamp = System.currentTimeMillis(),
                isOfflineReply = isOffline
            )
        )
    }

    private fun extractPrimaryCodeBlock(raw: String): String {
        val regex = Regex("```(?:[a-zA-Z0-9_+-]*)\\s*\\n([\\s\\S]*?)```")
        val match = regex.find(raw)
        return match?.groupValues?.getOrNull(1)?.trim() ?: raw.trim()
    }

    private fun formatPrice(price: Double): String {
        return when {
            price >= 1000.0 -> String.format(Locale.US, "%,.2f", price)
            price >= 1.0 -> String.format(Locale.US, "%.4f", price)
            else -> String.format(Locale.US, "%.5f", price)
        }
    }

    private fun defaultStarterKotlinCode(): String = """
package com.mc.generated

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Built with MC Coding Studio", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { /* Ready for APK build */ }) {
                            Text("Launch Action")
                        }
                    }
                }
            }
        }
    }
}
    """.trimIndent()

    override fun onCleared() {
        voiceEngine.destroy()
        super.onCleared()
    }
}
