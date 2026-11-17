package com.example.core

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

private const val TAG = "McVoiceEngine"

class McVoiceEngine(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var speechRecognizer: SpeechRecognizer? = null
    private var currentLanguage = AppLanguage.ENGLISH
    private var continuousWakeEnabled = false

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _lastRecognizedText = MutableStateFlow("")
    val lastRecognizedText: StateFlow<String> = _lastRecognizedText.asStateFlow()

    private val _voiceStatus = MutableStateFlow("")
    val voiceStatus: StateFlow<String> = _voiceStatus.asStateFlow()

    var onWakeWordDetected: (() -> Unit)? = null
    var onCommandRecognized: ((String) -> Unit)? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true
                applyTtsLocale(currentLanguage)
            } else {
                Log.w(TAG, "TTS initialization failed with status=$status")
            }
        }
    }

    fun setLanguage(language: AppLanguage) {
        currentLanguage = language
        if (ttsReady) {
            applyTtsLocale(language)
        }
    }

    private fun applyTtsLocale(language: AppLanguage) {
        val targetLocale = if (language == AppLanguage.BANGLA) {
            Locale("bn", "BD")
        } else {
            Locale.US
        }
        val result = tts?.setLanguage(targetLocale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts?.setLanguage(Locale.US)
        }
    }

    fun speak(text: String, flush: Boolean = true) {
        if (!ttsReady || text.isBlank()) return
        val cleanText = text
            .replace(Regex("```[\\s\\S]*?```"), " Code block ready on screen. ")
            .replace(Regex("[*#`▲▼]"), "")
            .take(500)
        val queueMode = if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        tts?.speak(cleanText, queueMode, null, "mc_${System.currentTimeMillis()}")
    }

    fun stopSpeaking() {
        tts?.stop()
    }

    fun isMcWakePhrase(input: String): Boolean {
        val normalized = input.trim().lowercase(Locale.US)
        if (normalized.isBlank()) return false
        return normalized == "mc" ||
            normalized == "m.c" ||
            normalized == "m c" ||
            normalized == "hey mc" ||
            normalized == "hi mc" ||
            normalized == "ok mc" ||
            normalized == "okay mc" ||
            normalized == "call mc" ||
            normalized.contains("এমসি") ||
            normalized.startsWith("mc ") ||
            normalized.startsWith("hey mc ")
    }

    fun startListening(continuousWake: Boolean = false) {
        continuousWakeEnabled = continuousWake
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _voiceStatus.value = "Speech recognition unavailable on this device"
            _isListening.value = false
            return
        }

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isListening.value = true
                        _voiceStatus.value = if (currentLanguage == AppLanguage.BANGLA) {
                            "\"MC\" বা কমান্ড বলুন..."
                        } else {
                            "Listening for \"MC\" or command..."
                        }
                    }

                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        _isListening.value = false
                    }

                    override fun onError(error: Int) {
                        _isListening.value = false
                        if (continuousWakeEnabled && error != SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                            startListening(continuousWake = true)
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        _isListening.value = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val spoken = matches?.firstOrNull().orEmpty().trim()
                        if (spoken.isNotBlank()) {
                            _lastRecognizedText.value = spoken
                            handleSpokenInput(spoken)
                        } else if (continuousWakeEnabled) {
                            startListening(continuousWake = true)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val partial = partialResults
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()
                            .orEmpty()
                        if (partial.isNotBlank() && isMcWakePhrase(partial)) {
                            _lastRecognizedText.value = partial
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE,
                    if (currentLanguage == AppLanguage.BANGLA) "bn-BD" else "en-US"
                )
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start speech recognizer", e)
            _isListening.value = false
            _voiceStatus.value = e.localizedMessage ?: "Mic error"
        }
    }

    private fun handleSpokenInput(spoken: String) {
        val lower = spoken.trim().lowercase(Locale.US)
        val wakeOnly = lower == "mc" || lower == "hey mc" || lower == "ok mc" ||
            lower == "okay mc" || lower == "hi mc" || lower == "এমসি"
        if (wakeOnly) {
            onWakeWordDetected?.invoke()
        } else if (lower.startsWith("mc ") || lower.startsWith("hey mc ")) {
            val cleaned = spoken.replaceFirst(Regex("^(hey\\s+)?mc[,:\\s]+", RegexOption.IGNORE_CASE), "").trim()
            if (cleaned.isBlank()) {
                onWakeWordDetected?.invoke()
            } else {
                onCommandRecognized?.invoke(cleaned)
            }
        } else {
            onCommandRecognized?.invoke(spoken)
        }
    }

    fun stopListening() {
        continuousWakeEnabled = false
        _isListening.value = false
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (_: Exception) {
        }
    }

    fun destroy() {
        stopListening()
        tts?.stop()
        tts?.shutdown()
    }
}
