package com.cashpal.app.models

import com.google.firebase.Timestamp

data class DeviceRegistration(
    val deviceId: String = "",
    val deviceModel: String = "",
    val platform: String = "android",
    val osVersion: String = "",
    val fcmToken: String = "",
    val lastKnownLatitude: Double? = null,
    val lastKnownLongitude: Double? = null,
    val lastLoginAt: Timestamp = Timestamp.now(),
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)

data class LoginSession(
    val id: String = "",
    val deviceId: String = "",
    val deviceModel: String = "",
    val fcmToken: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val createdAt: Timestamp = Timestamp.now()
)
