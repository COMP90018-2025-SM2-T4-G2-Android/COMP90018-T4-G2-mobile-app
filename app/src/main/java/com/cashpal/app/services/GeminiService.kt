package com.cashpal.app.services

import com.cashpal.app.BuildConfig
import com.google.gson.annotations.SerializedName
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

// Gemini API Request/Response models
data class GeminiRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null
) {
    data class Content(
        val parts: List<Part>,
        val role: String? = null
    ) {
        data class Part(
            val text: String
        )
    }
    
    data class GenerationConfig(
        val temperature: Double = 0.7,
        val topK: Int = 40,
        val topP: Double = 0.95,
        val maxOutputTokens: Int = 1024
    )
}

data class GeminiResponse(
    val candidates: List<Candidate>? = null,
    val error: GeminiError? = null
) {
    data class Candidate(
        val content: Content,
        val finishReason: String? = null
    ) {
        data class Content(
            val parts: List<Part>,
            val role: String? = null
        ) {
            data class Part(
                val text: String? = null
            )
        }
    }
    
    data class GeminiError(
        val code: Int? = null,
        val message: String? = null,
        val status: String? = null
    )
}

data class ModelsListResponse(
    val models: List<ModelInfo>? = null,
    val error: GeminiResponse.GeminiError? = null
) {
    data class ModelInfo(
        val name: String,
        val displayName: String? = null,
        val supportedGenerationMethods: List<String>? = null
    )
}

interface GeminiApi {
    // List available models
    @GET("v1/models")
    suspend fun listModels(): Response<ModelsListResponse>
    
    // Try multiple endpoints - Google AI Studio free tier models
    // Try newer gemini-2.5 models first (current standard)
    @POST("v1/models/gemini-2.5-flash:generateContent")
    suspend fun generateContent25Flash(@Body request: GeminiRequest): Response<GeminiResponse>
    
    @POST("v1/models/gemini-2.5-pro:generateContent")
    suspend fun generateContent25Pro(@Body request: GeminiRequest): Response<GeminiResponse>
    
    // Try v1beta versions
    @POST("v1beta/models/gemini-1.5-flash:generateContent")
    suspend fun generateContentV1Beta(@Body request: GeminiRequest): Response<GeminiResponse>
    
    @POST("v1/models/gemini-1.5-flash:generateContent")
    suspend fun generateContentV1(@Body request: GeminiRequest): Response<GeminiResponse>
    
    // Fallback: Try gemini-1.5-pro
    @POST("v1beta/models/gemini-1.5-pro:generateContent")
    suspend fun generateContentPro(@Body request: GeminiRequest): Response<GeminiResponse>
}

// Helper function to try multiple endpoints
suspend fun GeminiApi.generateContentWithFallback(request: GeminiRequest): Response<GeminiResponse> {
    // First, try to list available models (for debugging)
    try {
        val modelsResponse = listModels()
        if (modelsResponse.isSuccessful && modelsResponse.body()?.models != null) {
            val availableModels = modelsResponse.body()!!.models!!.map { it.name }
            val availableModelsStr = availableModels.joinToString()
            android.util.Log.d("GeminiService", "Available models: $availableModelsStr")
        } else {
            android.util.Log.w("GeminiService", "Failed to list models: ${modelsResponse.code()}")
        }
    } catch (e: Exception) {
        android.util.Log.e("GeminiService", "Exception listing models: ${e.message}", e)
    }
    
    // Try gemini-2.5-flash first (confirmed available via API)
    android.util.Log.d("GeminiService", "Attempting v1/models/gemini-2.5-flash")
    val response1 = try {
        generateContent25Flash(request)
    } catch (e: Exception) {
        android.util.Log.e("GeminiService", "Exception calling gemini-2.5-flash: ${e.message}", e)
        null
    }
    if (response1 != null && response1.isSuccessful) {
        android.util.Log.i("GeminiService", "✅ SUCCESS with v1/models/gemini-2.5-flash")
        return response1
    }
    android.util.Log.w("GeminiService", "❌ Failed v1/models/gemini-2.5-flash: ${response1?.code()}")
    response1?.errorBody()?.let { errorBody ->
        try {
            android.util.Log.d("GeminiService", "Error body: ${errorBody.string()}")
        } catch (e: Exception) {
            android.util.Log.e("GeminiService", "Error reading error body: ${e.message}")
        }
    }
    
    // Try gemini-2.5-pro
    android.util.Log.d("GeminiService", "Attempting v1/models/gemini-2.5-pro")
    val response2 = try {
        generateContent25Pro(request)
    } catch (e: Exception) {
        android.util.Log.e("GeminiService", "Exception calling gemini-2.5-pro: ${e.message}", e)
        null
    }
    if (response2 != null && response2.isSuccessful) {
        android.util.Log.i("GeminiService", "✅ SUCCESS with v1/models/gemini-2.5-pro")
        return response2
    }
    android.util.Log.w("GeminiService", "❌ Failed v1/models/gemini-2.5-pro: ${response2?.code()}")
    
    // Return the last response (or create a failed response if all failed)
    val mediaType = "application/json".toMediaTypeOrNull()!!
    val errorBody = "{\"error\":{\"message\":\"All models failed\"}}".toResponseBody(mediaType)
    return response2 ?: response1 ?: Response.error(404, errorBody)
}

object GeminiService {
    /**
     * Get API key from Firebase Remote Config, fallback to BuildConfig
     */
    private fun getApiKey(): String {
        val remoteConfigKey = FirebaseConfigService.getGeminiApiKey()
        return if (remoteConfigKey.isNotEmpty()) {
            remoteConfigKey
        } else {
            BuildConfig.GEMINI_API_KEY // Fallback for development
        }
    }
    
    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val apiKey = getApiKey()
        val newRequest = originalRequest.newBuilder()
            .header("Content-Type", "application/json")
            .url(originalRequest.url.newBuilder().addQueryParameter("key", apiKey).build())
            .build()
        chain.proceed(newRequest)
    }
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val api: GeminiApi = retrofit.create(GeminiApi::class.java)
    
    // Convenience method that tries multiple endpoints
    suspend fun generateContent(request: GeminiRequest): Response<GeminiResponse> {
        return api.generateContentWithFallback(request)
    }
}

