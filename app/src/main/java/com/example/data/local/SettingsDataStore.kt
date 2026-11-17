package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.core.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.mcDataStore: DataStore<Preferences> by preferencesDataStore(name = "mc_settings")

data class McSettings(
    val language: AppLanguage = AppLanguage.ENGLISH,
    val backgroundVideoTalk: Boolean = false,
    val screenOffFastWake: Boolean = false,
    val offlineMode: Boolean = false,
    val autoSpeakReplies: Boolean = true,
    val githubUsername: String = ""
)

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val BACKGROUND_VIDEO_TALK = booleanPreferencesKey("background_video_talk")
        val SCREEN_OFF_FAST_WAKE = booleanPreferencesKey("screen_off_fast_wake")
        val OFFLINE_MODE = booleanPreferencesKey("offline_mode")
        val AUTO_SPEAK = booleanPreferencesKey("auto_speak")
        val GITHUB_USERNAME = stringPreferencesKey("github_username")
    }

    val settingsFlow: Flow<McSettings> = context.mcDataStore.data.map { prefs ->
        val langCode = prefs[Keys.LANGUAGE] ?: AppLanguage.ENGLISH.code
        McSettings(
            language = if (langCode == AppLanguage.BANGLA.code) AppLanguage.BANGLA else AppLanguage.ENGLISH,
            backgroundVideoTalk = prefs[Keys.BACKGROUND_VIDEO_TALK] ?: false,
            screenOffFastWake = prefs[Keys.SCREEN_OFF_FAST_WAKE] ?: false,
            offlineMode = prefs[Keys.OFFLINE_MODE] ?: false,
            autoSpeakReplies = prefs[Keys.AUTO_SPEAK] ?: true,
            githubUsername = prefs[Keys.GITHUB_USERNAME] ?: ""
        )
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.mcDataStore.edit { it[Keys.LANGUAGE] = language.code }
    }

    suspend fun setBackgroundVideoTalk(enabled: Boolean) {
        context.mcDataStore.edit { it[Keys.BACKGROUND_VIDEO_TALK] = enabled }
    }

    suspend fun setScreenOffFastWake(enabled: Boolean) {
        context.mcDataStore.edit { it[Keys.SCREEN_OFF_FAST_WAKE] = enabled }
    }

    suspend fun setOfflineMode(offline: Boolean) {
        context.mcDataStore.edit { it[Keys.OFFLINE_MODE] = offline }
    }

    suspend fun setAutoSpeakReplies(enabled: Boolean) {
        context.mcDataStore.edit { it[Keys.AUTO_SPEAK] = enabled }
    }

    suspend fun setGithubUsername(username: String) {
        context.mcDataStore.edit { it[Keys.GITHUB_USERNAME] = username.trim() }
    }
}
