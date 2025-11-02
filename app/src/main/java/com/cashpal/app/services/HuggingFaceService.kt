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

data class HuggingFaceRequest(
    val inputs: String
)

data class HuggingFaceResponse(
    @SerializedName("generated_text")
    val generatedText: String?,
    @SerializedName("error")
    val error: String? = null
)

interface HuggingFaceApi {
    // Note: If you get 404 errors, try these alternative models:
    // - "models/microsoft/DialoGPT-small" (smaller but more reliable)
    // - "models/gpt2" (basic text generation)
    // - "models/distilgpt2" (faster version)
    // Make sure the model exists on HuggingFace: https://huggingface.co/models
    @POST("models/microsoft/DialoGPT-small")
    suspend fun chat(@Body request: HuggingFaceRequest): retrofit2.Response<List<HuggingFaceResponse>>
}

object HuggingFaceService {
    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val newRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer ${BuildConfig.HF_API_KEY}")
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
        .baseUrl("https://api-inference.huggingface.co/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val api: HuggingFaceApi = retrofit.create(HuggingFaceApi::class.java)
}

