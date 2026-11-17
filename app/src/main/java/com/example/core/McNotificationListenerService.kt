package com.example.core

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DeviceNotificationItem(
    val key: String,
    val appPackage: String,
    val title: String,
    val text: String,
    val timestamp: Long
)

class McNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        _isListenerConnected.value = true
        refreshActive()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        _isListenerConnected.value = false
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        refreshActive()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        refreshActive()
    }

    private fun refreshActive() {
        try {
            val items = activeNotifications.orEmpty()
                .filter { it.packageName != packageName }
                .mapNotNull { sbn ->
                    val extras = sbn.notification?.extras
                    val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
                    val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
                    if (title.isBlank() && text.isBlank()) return@mapNotNull null
                    DeviceNotificationItem(
                        key = sbn.key,
                        appPackage = sbn.packageName.substringAfterLast('.'),
                        title = title.ifBlank { sbn.packageName },
                        text = text,
                        timestamp = sbn.postTime
                    )
                }
                .sortedByDescending { it.timestamp }
                .take(25)
            _notifications.value = items
        } catch (_: Exception) {
        }
    }

    companion object {
        private val _notifications = MutableStateFlow<List<DeviceNotificationItem>>(emptyList())
        val notifications: StateFlow<List<DeviceNotificationItem>> = _notifications.asStateFlow()

        private val _isListenerConnected = MutableStateFlow(false)
        val isListenerConnected: StateFlow<Boolean> = _isListenerConnected.asStateFlow()

        fun isNotificationAccessEnabled(context: Context): Boolean {
            val cn = ComponentName(context, McNotificationListenerService::class.java)
            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: return false
            return flat.contains(cn.flattenToString())
        }
    }
}
