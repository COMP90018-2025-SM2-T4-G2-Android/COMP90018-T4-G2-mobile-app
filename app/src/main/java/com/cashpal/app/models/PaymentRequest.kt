package com.cashpal.app.models

import com.google.firebase.Timestamp

data class PaymentRequest(
    val id: String = "",
    val fromUserId: String = "",
    val toUserId: String = "",
    val amount: Double = 0.0,
    val currency: String = "USD",
    val description: String = "",
    val status: PaymentRequestStatus = PaymentRequestStatus.PENDING,
    val expiresAt: Timestamp? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val respondedAt: Timestamp? = null,
    val qrCodeData: String? = null
)

enum class PaymentRequestStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    EXPIRED,
    CANCELLED
}
