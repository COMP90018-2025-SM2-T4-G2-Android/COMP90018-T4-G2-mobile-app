package com.cashpal.app.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.annotation.SuppressLint
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.cashpal.app.MainActivity
import com.cashpal.app.R

object NotificationService {

    private const val CHANNEL_ID = "cashpal_payments"
    private const val CHANNEL_NAME = "Payment Notifications"
    private const val CHANNEL_DESCRIPTION = "Notifications for payments and security alerts"

    private var nextId = 1000

    /** Call once at app start (e.g., in MainActivity.onCreate) */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    /** Android 13+ runtime permission gate */
    private fun canPost(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    @SuppressLint("MissingPermission") // gated by canPost()
    private fun post(context: Context, builder: NotificationCompat.Builder) {
        if (!canPost(context)) return
        try {
            NotificationManagerCompat.from(context).notify(nextId++, builder.build())
        } catch (_: SecurityException) {
            // Silently ignore if OEM policy blocks notifications.
        }
    }

    // --- Payment Received ---
    fun showPaymentReceivedNotification(
        context: Context,
        senderName: String,
        amount: Double,
        timestamp: String = "Just now"
    ) {
        val txnId = "TXN-${System.currentTimeMillis()}"

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("openTab", "receipt")
            putExtra("transactionId", txnId)
            putExtra("senderName", senderName)
            putExtra("amount", amount)
            putExtra("timestamp", timestamp)
            putExtra("status", "completed")
            putExtra("type", "received")
        }
        val pi = PendingIntent.getActivity(
            context, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val msg = "From $senderName: +$${String.format("%.2f", amount)}"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // or your own drawable
            .setContentTitle("Payment received")
            .setContentText(msg)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$msg  •  $timestamp"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setColor(ContextCompat.getColor(context, R.color.received_color))

        post(context, builder)
    }

    // --- Payment Sent ---
    fun showPaymentSentNotification(
        context: Context,
        recipientName: String,
        amount: Double
    ) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("openTab", "history")
        }
        val pi = PendingIntent.getActivity(
            context, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val msg = "To $recipientName: -$${String.format("%.2f", amount)}"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_input_add)
            .setContentTitle("Payment sent")
            .setContentText(msg)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setColor(ContextCompat.getColor(context, R.color.sent_color))

        post(context, builder)
    }

    // --- Payment Failed ---
    fun showPaymentFailedNotification(
        context: Context,
        recipientName: String,
        amount: Double,
        reason: String
    ) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("openTab", "history")
        }
        val pi = PendingIntent.getActivity(
            context, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Payment failed")
            .setContentText("To $recipientName: $reason")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Payment to $recipientName failed: $reason\n" +
                            "Amount: $${String.format("%.2f", amount)}"
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setColor(ContextCompat.getColor(context, R.color.error))

        post(context, builder)
    }

    // --- Fraud Alert (for GPS/MFA checks) ---
    fun showFraudAlert(context: Context, reason: String) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("openTab", "home")
        }
        val pi = PendingIntent.getActivity(
            context, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("Security check required")
            .setContentText(reason)
            .setStyle(NotificationCompat.BigTextStyle().bigText(reason))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setColor(ContextCompat.getColor(context, R.color.error))

        post(context, builder)
    }

    // Simple demo trigger
    fun simulatePaymentReceived(context: Context) {
        val senders = listOf("Sarah Johnson", "John Doe", "Mike Wilson", "Emma Brown")
        val amounts = listOf(25.50, 50.00, 75.25, 100.00, 15.75)
        showPaymentReceivedNotification(context, senders.random(), amounts.random())
    }
}
