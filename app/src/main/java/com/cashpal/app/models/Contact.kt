package com.cashpal.app.models

import com.google.firebase.Timestamp

data class Contact(
    val id: String = "",
    val userId: String = "", // Owner of this contact
    val contactUserId: String? = null, // If this is a registered user
    val name: String = "",
    val email: String? = null,
    val phone: String? = null,
    val avatarUrl: String? = null,
    val isFrequent: Boolean = false,
    val lastTransactionDate: Timestamp? = null,
    val totalTransactions: Int = 0,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)

data class ContactGroup(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val contactIds: List<String> = emptyList(),
    val createdAt: Timestamp = Timestamp.now()
)
