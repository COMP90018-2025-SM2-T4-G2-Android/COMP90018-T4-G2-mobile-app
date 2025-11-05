package com.cashpal.app.models

data class RecentScan(
    val vendor: String,
    val location: String,
    val time: String,
    val amount: String,
    val targetUserId: String? = null,
    val transactionId: String? = null,
    val isReceived: Boolean = false,
    val currency: String = "AUD",
    val qrCodeData: String? = null,
    val metadata: Map<String, Any> = emptyMap()
)
