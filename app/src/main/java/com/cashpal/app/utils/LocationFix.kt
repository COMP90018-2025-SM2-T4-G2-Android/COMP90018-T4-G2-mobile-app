package com.cashpal.app.utils

data class LocationFix(
    val lat: Double,
    val lon: Double,
    val accuracyM: Float = 0f
)
