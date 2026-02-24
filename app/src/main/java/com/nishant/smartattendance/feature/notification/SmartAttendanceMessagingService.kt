package com.nishant.smartattendance.feature.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.nishant.smartattendance.R
import com.nishant.smartattendance.feature.auth.LoginActivity

class SmartAttendanceMessagingService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_ID = "smart_attendance_channel"
        const val CHANNEL_NAME = "Smart Attendance"

        // Intent extras — StudentActivity reads these to navigate to Mark Attendance tab
        const val EXTRA_NAVIGATE_TO = "navigate_to"
        const val NAVIGATE_MARK_ATTENDANCE = "mark_attendance"
        const val EXTRA_SUBJECT = "subject"
        const val EXTRA_CODE = "code"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "Smart Attendance"

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: ""

        val type = remoteMessage.data["type"]
        val subject = remoteMessage.data["subject"] ?: ""
        val code = remoteMessage.data["code"] ?: ""

        showNotification(title, body, type, subject, code)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        NotificationRepository.saveTokenIfLoggedIn(token)
    }

    private fun showNotification(
        title: String,
        body: String,
        type: String?,
        subject: String,
        code: String
    ) {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create channel for Android 8+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Attendance session start notifications"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Build the tap intent
        // - If it's a session_started notification → open StudentActivity with deep link
        //   to Mark Attendance tab
        // - Otherwise → open LoginActivity (fallback / re-login flow)
        val tapIntent: Intent = if (type == "session_started") {
            // NOTE: Replace StudentActivity::class.java with your actual student main activity
            // if it has a different name. The EXTRA_NAVIGATE_TO flag tells it to open
            // the Mark Attendance tab instead of the default tab.
            Intent(this, Class.forName(
                "${applicationContext.packageName}.feature.student.StudentDashboardActivity"
            )).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(EXTRA_NAVIGATE_TO, NAVIGATE_MARK_ATTENDANCE)
                putExtra(EXTRA_SUBJECT, subject)
                putExtra(EXTRA_CODE, code)
            }
        } else {
            Intent(this, LoginActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_check_circle)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}