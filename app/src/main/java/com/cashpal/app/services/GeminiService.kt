package com.cashpal.app.services

import com.cashpal.app.BuildConfig
import com.google.gson.annotations.SerializedName
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
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

interface GeminiApi {
    // Using v1 API with gemini-pro (most stable and widely available)
    // If this doesn't work, try: "v1/models/gemini-1.5-flash:generateContent"
    // or check available models at: https://ai.google.dev/gemini-api/docs/models
    @POST("v1/models/gemini-pro:generateContent")
    suspend fun generateContent(@Body request: GeminiRequest): Response<GeminiResponse>
}

object GeminiService {
    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val newRequest = originalRequest.newBuilder()
            .header("Content-Type", "application/json")
            .url(originalRequest.url.newBuilder().addQueryParameter("key", BuildConfig.GEMINI_API_KEY).build())
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
}

