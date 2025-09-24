package com.cashpal.app.models

data class TransactionHistory(
    val name: String,
    val reference: String,
    val amount: String,
    val status: String,
    val date: String,
    val type: String
)