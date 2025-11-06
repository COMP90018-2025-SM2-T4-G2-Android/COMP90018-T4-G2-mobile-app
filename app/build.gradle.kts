plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.cashpal.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.cashpal.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Read API keys from multiple sources (priority order):
        // 1. Environment variables (CI/CD, secure)
        // 2. gradle.properties (can be committed, with example file)
        // 3. local.properties (local development only, gitignored)
        
        var hfApiKey = ""
        var geminiApiKey = ""
        
        // Try environment variables first (for CI/CD)
        hfApiKey = System.getenv("HF_API_KEY") ?: ""
        geminiApiKey = System.getenv("GEMINI_API_KEY") ?: ""
        
        // Try gradle.properties (project-level properties)
        // Check each key independently to maintain proper precedence
        val gradleProperties = rootProject.file("gradle.properties")
        if (gradleProperties.exists()) {
            gradleProperties.readLines().forEach { line ->
                when {
                    line.startsWith("HF_API_KEY=") && hfApiKey.isEmpty() -> {
                        hfApiKey = line.substringAfter("=").trim()
                    }
                    line.startsWith("GEMINI_API_KEY=") && geminiApiKey.isEmpty() -> {
                        geminiApiKey = line.substringAfter("=").trim()
                    }
                }
            }
        }
        
        // Fallback to local.properties (for local development)
        // Check each key independently to maintain proper precedence
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.readLines().forEach { line ->
                when {
                    line.startsWith("HF_API_KEY=") && hfApiKey.isEmpty() -> {
                        hfApiKey = line.substringAfter("=").trim()
                    }
                    line.startsWith("GEMINI_API_KEY=") && geminiApiKey.isEmpty() -> {
                        geminiApiKey = line.substringAfter("=").trim()
                    }
                }
            }
        }
        
        // Set BuildConfig fields - these will be compiled into the APK
        buildConfigField("String", "HF_API_KEY", "\"$hfApiKey\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true  // Enable code obfuscation
            isShrinkResources = true  // Remove unused resources
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
                "proguard-rules-api-keys.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")
    
    // JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")

    // AI Feature
    // Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:34.1.0"))
    // Firebase AI Logic client SDK
    implementation("com.google.firebase:firebase-ai")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-config")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    
    // CameraX
    implementation("androidx.camera:camera-core:1.3.3")
    implementation("androidx.camera:camera-camera2:1.3.3")
    implementation("androidx.camera:camera-lifecycle:1.3.3")
    implementation("androidx.camera:camera-view:1.3.3")

    // ML Kit Barcode Scanning
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    // Lifecycle + ViewModel
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")

    // ZXing core library for QR code generation
    implementation("com.google.zxing:core:3.5.0")

    // Coroutines for Firebase
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    
    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    
    // Biometric Authentication
    implementation("androidx.biometric:biometric:1.2.0-alpha05")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    //Pin authentication
    implementation("androidx.security:security-crypto-ktx:1.1.0-alpha06")

    implementation("com.google.android.gms:play-services-location:21.0.1")
    
    // HTTP Networking for AI Assistant
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
}
