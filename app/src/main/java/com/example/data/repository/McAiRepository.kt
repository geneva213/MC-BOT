package com.example.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.example.core.AppLanguage
import com.example.data.model.CryptoTickerAnalysis
import com.example.data.model.MarketDirection
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import java.util.Locale

private const val TAG = "McAiRepository"

class McAiRepository {

    private val chatModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    "You are MC, a loyal, ultra-fast personal AI assistant, senior Android/software engineer, and real-time crypto order-flow analyst. " +
                        "Always address the user respectfully as 'Boss' (or 'বস' in Bangla). " +
                        "When asked in Bangla or when the user's language is Bangla, respond in natural, clear Bangla (with technical crypto/code terms kept accurate). " +
                        "When asked in English, respond in crisp, direct English. " +
                        "When analyzing crypto market data provided in the prompt, always explicitly state the exact Buyer % vs Seller % and clearly conclude UP (Bullish) or DOWN (Bearish) based on the data."
                )
            }
        )
    }

    fun streamAssistantResponse(
        prompt: String,
        language: AppLanguage,
        isOnline: Boolean,
        marketContext: List<CryptoTickerAnalysis>,
        cameraFrame: Bitmap? = null
    ): Flow<String> = flow {
        if (!isOnline) {
            emit(buildOfflineReply(prompt, language, marketContext))
            return@flow
        }

        val fullPrompt = buildString {
            appendLine("Active User Language: ${language.displayName} (${language.nativeLabel})")
            if (marketContext.isNotEmpty()) {
                appendLine("Live Crypto Market Order-Flow Snapshot:")
                marketContext.forEach { item ->
                    appendLine(
                        "- ${item.asset.symbol} (${item.timeframe.apiInterval}): Price=$${formatPrice(item.priceUsd)}, " +
                            "Buyers=${item.buyerPercent}%, Sellers=${item.sellerPercent}%, " +
                            "Direction=${item.direction.name}, Confidence=${item.confidencePercent}%, " +
                            "RSI14=${String.format(Locale.US, "%.1f", item.rsi14)}"
                    )
                }
            }
            appendLine()
            appendLine("Boss Request: $prompt")
        }

        val accumulated = StringBuilder()
        try {
            withTimeout(60_000L) {
                val inputContent = content {
                    if (cameraFrame != null) {
                        image(cameraFrame)
                    }
                    text(fullPrompt)
                }
                chatModel.generateContentStream(inputContent).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        emit(accumulated.toString())
                    }
                }
            }
            if (accumulated.isBlank()) {
                throw IllegalStateException("Gemini returned an empty response.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "MC AI request timed out", e)
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "MC AI request failed", e)
            throw e
        }
    }

    suspend fun generateCode(
        request: String,
        targetStack: String,
        language: AppLanguage,
        isOnline: Boolean
    ): String {
        if (!isOnline) {
            return buildOfflineCodeTemplate(request, targetStack)
        }

        val prompt = """
            Language preference for explanation: ${language.displayName}.
            Target Stack: $targetStack.
            Generate clean, complete, production-ready code for: $request
            Provide the code clearly in a fenced code block so Boss can copy, share to GitHub, or export as an Android project.
        """.trimIndent()

        return try {
            withTimeout(60_000L) {
                val response = chatModel.generateContent(prompt)
                response.text?.takeIf { it.isNotBlank() }
                    ?: throw IllegalStateException("Model returned empty code.")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Code generation failed", e)
            throw e
        }
    }

    private fun buildOfflineReply(
        prompt: String,
        language: AppLanguage,
        marketContext: List<CryptoTickerAnalysis>
    ): String {
        val lower = prompt.lowercase(Locale.US)
        val isBangla = language == AppLanguage.BANGLA

        if (lower.contains("market") || lower.contains("btc") || lower.contains("crypto") ||
            lower.contains("signal") || lower.contains("মার্কেট") || lower.contains("বিটকয়েন")
        ) {
            if (marketContext.isEmpty()) {
                return if (isBangla) {
                    "বস, আমি এখন অফলাইন মোডে আছি এবং কোনো ক্যাশ করা মার্কেট স্ন্যাপশট নেই। লাইভ ডেটার জন্য অনলাইন মোড চালু করুন।"
                } else {
                    "Boss, I am in Offline Mode and no market snapshot is cached yet. Switch to Online Mode once to capture live exchange data."
                }
            }
            return buildString {
                if (isBangla) {
                    appendLine("বস, অফলাইন ক্যাশ থেকে সর্বশেষ মার্কেট বিশ্লেষণ:")
                } else {
                    appendLine("Boss, here is the latest cached market order-flow analysis:")
                }
                marketContext.forEach { item ->
                    val dirText = if (item.direction == MarketDirection.UP) {
                        if (isBangla) "উপরে (UP ▲)" else "UP (BULLISH ▲)"
                    } else {
                        if (isBangla) "নিচে (DOWN ▼)" else "DOWN (BEARISH ▼)"
                    }
                    appendLine(
                        "• ${item.asset.symbol} [${item.timeframe.apiInterval}] $${formatPrice(item.priceUsd)} | " +
                            "Buyers ${item.buyerPercent}% / Sellers ${item.sellerPercent}% → $dirText"
                    )
                }
            }
        }

        return if (isBangla) {
            "হ্যাঁ বস! আমি অফলাইন লোকাল কোরে কাজ করছি। আপনি কল, মেসেজ, ফ্ল্যাশলাইট, অফলাইন কোড টেমপ্লেট এবং ক্যাশ করা ক্রিপ্টো সিগন্যাল ব্যবহার করতে পারেন। লাইভ জেমিনি AI উত্তরের জন্য অনলাইন মোড চালু করুন।"
        } else {
            "Yes, Boss! MC Local Core is active offline. I can dial calls, draft SMS, toggle flashlight/system panels, share code templates, and show your cached crypto Bull/Bear snapshots. Switch Online for live Gemini AI & real-time exchange ticks."
        }
    }

    private fun buildOfflineCodeTemplate(request: String, targetStack: String): String {
        return """
            ```kotlin
            // Offline MC Template ($targetStack) for: $request
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
                                    Text("MC Generated App", style = MaterialTheme.typography.headlineMedium)
                                    Spacer(Modifier.height(12.dp))
                                    Text("Prompt: $request")
                                }
                            }
                        }
                    }
                }
            }
            ```
        """.trimIndent()
    }

    private fun formatPrice(price: Double): String {
        return when {
            price >= 1000.0 -> String.format(Locale.US, "%,.2f", price)
            price >= 1.0 -> String.format(Locale.US, "%.4f", price)
            else -> String.format(Locale.US, "%.5f", price)
        }
    }
}
