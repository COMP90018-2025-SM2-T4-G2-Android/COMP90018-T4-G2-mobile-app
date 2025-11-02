package com.cashpal.app.services

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import kotlinx.coroutines.tasks.await

/**
 * Firebase Remote Config Service
 * Fetches API keys from Firebase Remote Config at runtime
 * This way API keys are never in the APK or code repository
 */
object FirebaseConfigService {
    private val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
    
    init {
        // Configure Remote Config settings
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 3600 // Fetch once per hour
            fetchTimeoutInSeconds = 10
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        
        // Set default values (fallback if remote config unavailable)
        // These will be used until Remote Config is fetched
        remoteConfig.setDefaultsAsync(
            mapOf(
                "gemini_api_key" to "",
                "hf_api_key" to ""
            )
        )
        
        // Try to fetch immediately (non-blocking)
        // Main fetch will happen in MainActivity
        android.util.Log.d("FirebaseConfigService", "Remote Config initialized with defaults")
    }
    
    /**
     * Fetch API keys from Firebase Remote Config
     * Call this once at app startup (e.g., in Application class or MainActivity)
     */
    suspend fun fetchApiKeys(): Result<Unit> {
        return try {
            remoteConfig.fetchAndActivate().await()
            android.util.Log.d("FirebaseConfigService", "Successfully fetched API keys from Remote Config")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseConfigService", "Failed to fetch API keys from Remote Config", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get Gemini API key from Remote Config
     */
    fun getGeminiApiKey(): String {
        return remoteConfig.getString("gemini_api_key")
    }
    
    /**
     * Get HuggingFace API key from Remote Config
     */
    fun getHuggingFaceApiKey(): String {
        return remoteConfig.getString("hf_api_key")
    }
    
    /**
     * Check if API keys are available
     */
    fun hasApiKeys(): Boolean {
        val geminiKey = getGeminiApiKey()
        val hfKey = getHuggingFaceApiKey()
        return geminiKey.isNotEmpty() || hfKey.isNotEmpty()
    }
}

