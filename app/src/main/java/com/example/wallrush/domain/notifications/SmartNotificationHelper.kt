package com.example.wallrush.domain.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity

object SmartNotificationHelper {

    private const val CHANNEL_ID = "wallrush_daily_smart_channel"
    private const val CHANNEL_NAME = "WallRush Daily Quests & Achievements"
    private const val CHANNEL_DESC = "Notifications for daily Quoridor challenges and unlocked achievements"

    private const val NOTIFICATION_ID_DAILY = 1001
    private const val NOTIFICATION_ID_ACHIEVEMENT = 1002

    private const val PREFS_NOTIF = "wallrush_notifications_prefs"
    private const val KEY_NOTIF_ENABLED = "notifications_user_enabled"

    fun areNotificationsEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NOTIF, Context.MODE_PRIVATE)
        val userEnabled = prefs.getBoolean(KEY_NOTIF_ENABLED, true)
        return userEnabled && hasNotificationPermission(context)
    }

    fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NOTIF, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_NOTIF_ENABLED, enabled)
            .apply()
    }

    fun showNotification(context: Context, id: Int, title: String, body: String) {
        if (!areNotificationsEnabled(context)) return
        try {
            initChannel(context)
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.notify(id, builder.build())
        } catch (_: Exception) {
            // Gracefully ignore
        }
    }

    fun initChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                setShowBadge(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showDailyQuestsNotification(context: Context, title: String, message: String) {
        if (!hasNotificationPermission(context)) return

        try {
            initChannel(context)
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.notify(NOTIFICATION_ID_DAILY, builder.build())
        } catch (_: Exception) {
            // Gracefully ignore if notification fails
        }
    }

    fun showAchievementUnlockedNotification(context: Context, title: String, description: String, xp: Int) {
        if (!hasNotificationPermission(context)) return

        try {
            initChannel(context)
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val contentText = "$description (+$xp XP)"
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.star_on)
                .setContentTitle("🏆 إنجاز جديد: $title")
                .setContentText(contentText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.notify(NOTIFICATION_ID_ACHIEVEMENT, builder.build())
        } catch (_: Exception) {
            // Gracefully ignore
        }
    }
}
