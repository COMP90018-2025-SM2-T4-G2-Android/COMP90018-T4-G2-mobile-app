package com.cashpal.app.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.cashpal.app.models.DeviceRegistration
import com.cashpal.app.models.LoginSession
import com.cashpal.app.services.FirebaseAuthService
import com.cashpal.app.services.FirestoreService
import com.google.android.gms.location.LocationServices
import com.google.firebase.Timestamp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class SecurityRepository(
    private val authService: FirebaseAuthService,
    private val firestoreService: FirestoreService
) {

    suspend fun registerDeviceForAlerts(context: Context): Result<Unit> {
        val user = authService.getCurrentUser()
            ?: return Result.failure(IllegalStateException("User not authenticated"))

        val token = try {
            FirebaseMessaging.getInstance().token.await()
        } catch (e: Exception) {
            return Result.failure(e)
        }

        val location = fetchLastLocation(context)
        val now = Timestamp.now()

        val deviceRegistration = DeviceRegistration(
            deviceId = getDeviceId(context),
            deviceModel = resolveDeviceModel(),
            platform = "android",
            osVersion = resolveOsVersion(),
            fcmToken = token,
            lastKnownLatitude = location?.latitude,
            lastKnownLongitude = location?.longitude,
            lastLoginAt = now,
            createdAt = now,
            updatedAt = now
        )

        return firestoreService.upsertDeviceRegistration(user.uid, deviceRegistration)
    }

    suspend fun updateMessagingToken(context: Context, token: String): Result<Unit> {
        val user = authService.getCurrentUser()
            ?: return Result.failure(IllegalStateException("User not authenticated"))

        val now = Timestamp.now()
        val deviceRegistration = DeviceRegistration(
            deviceId = getDeviceId(context),
            deviceModel = resolveDeviceModel(),
            platform = "android",
            osVersion = resolveOsVersion(),
            fcmToken = token,
            lastLoginAt = now,
            createdAt = now,
            updatedAt = now
        )

        return firestoreService.upsertDeviceRegistration(user.uid, deviceRegistration)
    }

    suspend fun logLoginEvent(context: Context): Result<String> {
        val user = authService.getCurrentUser()
            ?: return Result.failure(IllegalStateException("User not authenticated"))

        val location = fetchLastLocation(context)
        val currentToken = try {
            FirebaseMessaging.getInstance().token.await()
        } catch (_: Exception) {
            null
        }

        val session = LoginSession(
            deviceId = getDeviceId(context),
            deviceModel = resolveDeviceModel(),
            fcmToken = currentToken,
            latitude = location?.latitude,
            longitude = location?.longitude,
            createdAt = Timestamp.now()
        )

        return firestoreService.logLoginSession(user.uid, session)
    }

    suspend fun simulateRemoteLoginForTesting(
        context: Context,
        latitude: Double = -33.8688,
        longitude: Double = 151.2093
    ): Result<String> {
        val user = authService.getCurrentUser()
            ?: return Result.failure(IllegalStateException("User not authenticated"))

        // Ensure this device is registered so the Cloud Function has a target token
        val registrationResult = registerDeviceForAlerts(context)
        if (registrationResult.isFailure) {
            return Result.failure(
                registrationResult.exceptionOrNull()
                    ?: IllegalStateException("Failed to register device for testing")
            )
        }

        val simulatedSession = LoginSession(
            deviceId = "debug-remote-device",
            deviceModel = "[TEST] Remote Device",
            fcmToken = null,
            latitude = latitude,
            longitude = longitude,
            createdAt = Timestamp.now()
        )

        return firestoreService.logLoginSession(user.uid, simulatedSession)
    }

    private fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "${Build.MANUFACTURER}_${Build.MODEL}"
    }

    private suspend fun fetchLastLocation(context: Context): Location? = withContext(Dispatchers.IO) {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) return@withContext null

        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        return@withContext try {
            fusedClient.lastLocation.await()
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveDeviceModel(): String {
        val model = Build.MODEL
        if (model.isNotBlank()) return model
        val manufacturer = Build.MANUFACTURER
        return if (manufacturer.isNotBlank()) "$manufacturer device" else "Android device"
    }

    private fun resolveOsVersion(): String {
        val release = Build.VERSION.RELEASE
        return if (release.isNotBlank()) release else Build.VERSION.CODENAME
    }
}
