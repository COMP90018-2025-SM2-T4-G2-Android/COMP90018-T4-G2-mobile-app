package com.cashpal.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.util.Locale
import android.annotation.SuppressLint


object NotificationService {

    private const val CHANNEL_ID = "cashpal_payments"
    private const val CHANNEL_NAME = "Payment Notifications"
    private const val CHANNEL_DESCRIPTION = "Notifications for received payments"

    private var notificationId = 1000

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    fun showPaymentReceivedNotification(
        context: Context,
        senderName: String,
        amount: Double,
        timestamp: String = "Just now"
    ) {
        // Create transaction ID
        val transactionId = "TXN-${System.currentTimeMillis()}"
        // Use unique notification ID as request code to ensure each PendingIntent is unique
        val id = notificationId++

        // Intent to open Receipt page
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("openTab", "receipt")
            putExtra("transactionId", transactionId)
            putExtra("senderName", senderName)
            putExtra("amount", amount)
            putExtra("timestamp", timestamp)
            putExtra("status", "completed")
            putExtra("type", "received")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Custom layout
        val notificationLayout = RemoteViews(context.packageName, R.layout.notification_payment_received).apply {
            setTextViewText(R.id.tv_notification_sender, "From $senderName")
            setTextViewText(
                R.id.tv_notification_amount,
                "+$${String.format(Locale.US, "%.2f", amount)}"
            )
            setTextViewText(R.id.tv_notification_time, timestamp)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_trending_up)
            .setCustomContentView(notificationLayout)
            .setContentTitle("Payment Received")
            .setContentText(
                "From $senderName: +$${String.format(Locale.US, "%.2f", amount)}"
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(ContextCompat.getColor(context, R.color.received_color))
            .setVibrate(longArrayOf(0, 250, 250, 250))
            .setLights(ContextCompat.getColor(context, R.color.received_color), 1000, 3000)
            .build()

        post(context, id, notification)
    }

    fun showPaymentSentNotification(
        context: Context,
        recipientName: String,
        amount: Double
    ) {
        // Use unique notification ID as request code to ensure each PendingIntent is unique
        val id = notificationId++
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("openTab", "history")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_send)
            .setContentTitle("Payment Sent")
            .setContentText(
                "To $recipientName: -$${String.format(Locale.US, "%.2f", amount)}"
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(ContextCompat.getColor(context, R.color.sent_color))
            .build()

        post(context, id, notification)
    }

    fun showPaymentFailedNotification(
        context: Context,
        recipientName: String,
        amount: Double,
        reason: String
    ) {
        // Use unique notification ID as request code to ensure each PendingIntent is unique
        val id = notificationId++
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("openTab", "history")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val body = "Payment to $recipientName failed: $reason\n" +
                "Amount: $${String.format(Locale.US, "%.2f", amount)}"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Payment Failed")
            .setContentText("Payment to $recipientName failed: $reason")
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(ContextCompat.getColor(context, R.color.error))
            .build()

        post(context, id, notification)
    }

    fun showFraudAlert(
        context: Context,
        reason: String
    ) {
        val id = notificationId++

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("openTab", "home")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("Security check required")
            .setContentText(reason)
            .setStyle(NotificationCompat.BigTextStyle().bigText(reason))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setColor(ContextCompat.getColor(context, R.color.error))
            .setContentIntent(pendingIntent)
            .build()

        post(context, id, notification)
    }

    /** 
     * Demo helper to simulate a payment push inside the app.
     * Only works in debug builds - will be ignored in production.
     */
    fun simulatePaymentReceived(context: Context) {
        // Only allow simulation in debug builds to prevent spam in production
        // Check if app is debuggable as alternative to BuildConfig.DEBUG
        val isDebugBuild = try {
            (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        } catch (e: Exception) {
            false
        }
        
        if (!isDebugBuild) {
            android.util.Log.w("NotificationService", "simulatePaymentReceived called in production build - ignoring")
            return
        }
        
        val senders = listOf("Sarah Johnson", "John Doe", "Mike Wilson", "Emma Brown")
        val amounts = listOf(25.50, 50.00, 75.25, 100.00, 15.75)
        showPaymentReceivedNotification(
            context,
            senders.random(),
            amounts.random()
        )
    }

    // ---- internal helpers ----

    private fun canPostNotifications(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= 33) {
            ActivityCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    @SuppressLint("MissingPermission")
    private fun post(context: Context, id: Int, notification: android.app.Notification) {
        // Runtime guard – if user denied POST_NOTIFICATIONS on API 33+, do nothing
        if (!canPostNotifications(context)) return

        try {
            NotificationManagerCompat.from(context)
                .notify(id, notification)
        } catch (se: SecurityException) {
            // Extra safety on odd OEMs
        }
    }

}
