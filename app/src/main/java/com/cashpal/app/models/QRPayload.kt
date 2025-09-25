package com.cashpal.app.models

data class QRPayload(
    val id: String,
    val name: String,
    val phone: String,
    val type: String = "personal",  // "personal" | "vendor" | others
    val amount: Double? = null      // Optional requested amount
)
