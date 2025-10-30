package com.cashpal.app.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.util.Locale
import android.annotation.SuppressLint
import com.cashpal.app.MainActivity
import com.cashpal.app.R


object NotificationService {

    private const val CHANNEL_ID = "cashpal_payments"
    private const val CHANNEL_NAME = "Payment Notifications"
    private const val CHANNEL_DESCRIPTION = "Notifications for received payments"
    private const val SECURITY_CHANNEL_ID = "cashpal_security"
    private const val SECURITY_CHANNEL_NAME = "Security Alerts"
    private const val SECURITY_CHANNEL_DESCRIPTION = "Notifications for security and fraud alerts"

    const val EXTRA_FRAUD_ALERT = "extra_fraud_alert"
    const val EXTRA_FRAUD_TITLE = "extra_fraud_title"
    const val EXTRA_FRAUD_MESSAGE = "extra_fraud_message"
    const val EXTRA_FRAUD_LOCATION = "extra_fraud_location"
    const val EXTRA_FRAUD_DEVICE = "extra_fraud_device"
    const val EXTRA_FRAUD_TIMESTAMP = "extra_fraud_timestamp"

    private var notificationId = 1000

    data class FraudAlertMetadata(
        val locationLabel: String? = null,
        val deviceName: String? = null,
        val occurredAt: String? = null
    )

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            val securityChannel = NotificationChannel(
                SECURITY_CHANNEL_ID,
                SECURITY_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = SECURITY_CHANNEL_DESCRIPTION
                enableVibration(true)
                setShowBadge(true)
                enableLights(true)
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
            nm.createNotificationChannel(securityChannel)
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
            context, 0, intent,
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

        post(context, notification)
    }

    fun showPaymentSentNotification(
        context: Context,
        recipientName: String,
        amount: Double
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("openTab", "history")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
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

        post(context, notification)
    }

    fun showPaymentFailedNotification(
        context: Context,
        recipientName: String,
        amount: Double,
        reason: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("openTab", "history")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
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

        post(context, notification)
    }

    /** Demo helper to simulate a payment push inside the app */
    fun simulatePaymentReceived(context: Context) {
        val senders = listOf("Sarah Johnson", "John Doe", "Mike Wilson", "Emma Brown")
        val amounts = listOf(25.50, 50.00, 75.25, 100.00, 15.75)
        showPaymentReceivedNotification(
            context,
            senders.random(),
            amounts.random()
        )
    }

    // ---- internal helpers ----

    fun showFraudAlert(
        context: Context,
        title: String? = null,
        message: String,
        metadata: FraudAlertMetadata? = null,
        deepLink: String? = null
    ) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("openTab", deepLink ?: "security")
            putExtra(EXTRA_FRAUD_ALERT, true)
            putExtra(EXTRA_FRAUD_TITLE, title ?: context.getString(R.string.fraud_alert_title))
            putExtra(EXTRA_FRAUD_MESSAGE, message)
            metadata?.locationLabel?.let { putExtra(EXTRA_FRAUD_LOCATION, it) }
            metadata?.deviceName?.let { putExtra(EXTRA_FRAUD_DEVICE, it) }
            metadata?.occurredAt?.let { putExtra(EXTRA_FRAUD_TIMESTAMP, it) }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, SECURITY_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle(title ?: context.getString(R.string.fraud_alert_title))
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(ContextCompat.getColor(context, R.color.error))
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .build()

        post(context, notification)
    }

    private fun canPostNotifications(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= 33) {
            ActivityCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    @SuppressLint("MissingPermission")
    private fun post(context: Context, notification: android.app.Notification) {
        // Runtime guard – if user denied POST_NOTIFICATIONS on API 33+, do nothing
        if (!canPostNotifications(context)) return

        try {
            NotificationManagerCompat.from(context)
                .notify(notificationId++, notification)
        } catch (se: SecurityException) {
            // Extra safety on odd OEMs
        }
    }

}
