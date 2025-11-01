package com.quakescope

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.content.getSystemService
import com.quakescope.ui.notifications.NotificationConstants
import com.quakescope.R
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class QuakeScopeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NotificationConstants.CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notification_channel_description)
            }
            val notificationManager: NotificationManager? = getSystemService()
            notificationManager?.createNotificationChannel(channel)
        }
    }
}
