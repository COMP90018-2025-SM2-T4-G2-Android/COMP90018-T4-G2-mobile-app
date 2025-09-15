package com.cashpal.app.data

data class BalanceInfo(
    val availableBalance: String,
    val thisMonthChange: String,
    val pendingAmount: String,
    val reservedAmount: String
)

data class QuickAction(
    val id: String,
    val title: String,
    val icon: String
)

data class Transaction(
    val id: String,
    val merchant: String,
    val amount: String,
    val timeAgo: String,
    val status: String,
    val icon: String,
    val isIncoming: Boolean
)

data class AppData(
    val balanceInfo: BalanceInfo,
    val quickActions: List<QuickAction>,
    val recentTransactions: List<Transaction>
)
