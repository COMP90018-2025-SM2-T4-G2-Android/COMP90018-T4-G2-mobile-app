package com.cashpal.app.models

import com.google.firebase.Timestamp
import java.util.Date

data class User(
    val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val phoneNumber: String? = null,
    val avatarUrl: String? = null,
    val balance: Double = 0.0,
    val currency: String = "USD",
    val isVerified: Boolean = false,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val preferences: UserPreferences = UserPreferences()
)

data class UserPreferences(
    val theme: String = "light", // light, dark, system
    val notifications: Boolean = true,
    val biometricAuth: Boolean = false,
    val currency: String = "USD",
    val language: String = "en"
)
