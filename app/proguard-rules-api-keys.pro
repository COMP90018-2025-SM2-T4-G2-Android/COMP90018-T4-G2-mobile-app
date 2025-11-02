# API Keys Obfuscation Rules
# Keep BuildConfig fields but obfuscate their usage
-keepclassmembers class com.cashpal.app.BuildConfig {
    public static final java.lang.String HF_API_KEY;
    public static final java.lang.String GEMINI_API_KEY;
}

# Obfuscate API key usage in services
-keep class com.cashpal.app.services.GeminiService { *; }
-keep class com.cashpal.app.services.HuggingFaceService { *; }

# But allow obfuscation of internal implementation details
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}

# Keep Retrofit interfaces
-keep interface com.cashpal.app.services.GeminiApi
-keep interface com.cashpal.app.services.HuggingFaceApi

# Keep Retrofit response models
-keep class com.cashpal.app.services.GeminiRequest { *; }
-keep class com.cashpal.app.services.GeminiResponse { *; }
-keep class com.cashpal.app.services.HuggingFaceRequest { *; }
-keep class com.cashpal.app.services.HuggingFaceResponse { *; }

