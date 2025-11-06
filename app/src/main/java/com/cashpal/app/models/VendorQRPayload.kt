package com.cashpal.app.models

data class VendorQRPayload(
    val id: String,
    val name: String,
    val merchantId: String,
    val category: String,
    val location: String,
    val contact: String? = null,
    val type: String = "store",
    val amount: Double? = null,
    val currency: String = "AUD",
    val timestamp: String
)
