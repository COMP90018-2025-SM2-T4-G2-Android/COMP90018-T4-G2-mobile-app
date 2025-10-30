package com.cashpal.app.services

import com.cashpal.app.utils.NotificationService
import com.cashpal.app.R
import com.cashpal.app.di.ServiceLocator
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AppFirebaseMessagingService : FirebaseMessagingService() {

    private val job = SupervisorJob()
    private val serviceScope = CoroutineScope(job + Dispatchers.IO)

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val data = remoteMessage.data
        val messageType = data["type"] ?: remoteMessage.notification?.tag

        if (messageType == "fraud_alert") {
            val title = data["title"] ?: remoteMessage.notification?.title
            val body = data["body"]
                ?: remoteMessage.notification?.body
                ?: getString(R.string.fraud_alert_generic_message)
            val destination = data["destination"]
            val metadata = NotificationService.FraudAlertMetadata(
                locationLabel = data["location"],
                deviceName = data["device"],
                occurredAt = data["timestamp"]
            )

            NotificationService.showFraudAlert(
                context = applicationContext,
                title = title,
                message = body,
                metadata = metadata,
                deepLink = destination
            )
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        val securityRepository = try {
            ServiceLocator.getSecurityRepository()
        } catch (_: IllegalStateException) {
            null
        } ?: return

        serviceScope.launch {
            securityRepository.updateMessagingToken(applicationContext, token)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
