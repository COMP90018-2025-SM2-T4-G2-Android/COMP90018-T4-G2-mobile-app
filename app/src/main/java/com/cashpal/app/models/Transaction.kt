package com.cashpal.app.models

import com.google.firebase.Timestamp

data class Transaction(
    val id: String = "",
    val fromUserId: String = "",
    val toUserId: String = "",
    val amount: Double = 0.0,
    val currency: String = "AUD",
    val description: String = "",
    val category: TransactionCategory = TransactionCategory.OTHER,
    val status: TransactionStatus = TransactionStatus.PENDING,
    val type: TransactionType = TransactionType.TRANSFER,
    val createdAt: Timestamp = Timestamp.now(),
    val completedAt: Timestamp? = null,
    val qrCodeData: String? = null,
    val location: TransactionLocation? = null,
    val metadata: Map<String, Any> = emptyMap()
)

enum class TransactionStatus {
    PENDING,
    COMPLETED,
    FAILED,
    CANCELLED,
    REFUNDED
}

enum class TransactionType {
    TRANSFER,
    PAYMENT,
    RECEIPT,
    WITHDRAWAL,
    DEPOSIT
}

enum class TransactionCategory {
    FOOD,
    TRANSPORT,
    SHOPPING,
    ENTERTAINMENT,
    BILLS,
    HEALTHCARE,
    EDUCATION,
    TRAVEL,
    OTHER
}

data class TransactionLocation(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String? = null,
    val city: String? = null,
    val country: String? = null
)
