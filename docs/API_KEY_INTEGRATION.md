# API Key Integration Guide for Android APK

## Overview

API keys are compiled into the APK using `BuildConfig` fields. When you build the app, Gradle reads the keys from various sources and embeds them into the compiled APK.

## How It Works

1. **Build Time**: Gradle reads API keys from one of these sources (in priority order):
   - Environment variables (for CI/CD)
   - `gradle.properties` (project-level, can be committed)
   - `local.properties` (local development only, gitignored)

2. **Compilation**: Keys are embedded into `BuildConfig` class:
   ```kotlin
   BuildConfig.GEMINI_API_KEY  // Available in your code
   BuildConfig.HF_API_KEY       // Available in your code
   ```

3. **APK**: The keys become part of the compiled APK file

## Setup Options

### Option 1: gradle.properties (Recommended for Team Builds)

1. Add your keys to `gradle.properties`:
   ```properties
   GEMINI_API_KEY=your_actual_api_key_here
   HF_API_KEY=your_huggingface_key_here
   ```

2. **Pros**: 
   - Can be committed to version control (with team agreement)
   - Works for all team members
   - Easy to update

3. **Cons**: 
   - Keys visible in version control (if committed)
   - Anyone with repo access can see keys

### Option 2: Environment Variables (CI/CD)

1. Set environment variables before building:
   ```bash
   export GEMINI_API_KEY=your_key_here
   export HF_API_KEY=your_key_here
   ./gradlew assembleRelease
   ```

2. **Pros**: 
   - Most secure for CI/CD pipelines
   - Keys never in code repository
   - Can use different keys per environment

3. **Cons**: 
   - Requires setting up environment variables
   - Not convenient for local development

### Option 3: local.properties (Local Development Only)

1. Already configured - add keys to `local.properties`:
   ```properties
   GEMINI_API_KEY=your_key_here
   HF_API_KEY=your_key_here
   ```

2. **Pros**: 
   - Gitignored (won't be committed)
   - Easy for local development

3. **Cons**: 
   - Only works locally
   - Won't work for distribution builds

## Security Considerations

### ⚠️ Important: API Keys in APKs Can Be Extracted

**Reality**: Any API key embedded in an Android APK can be extracted by reverse engineering the APK. This is unavoidable.

### Security Best Practices:

1. **Use API Key Restrictions**:
   - In Google Cloud Console, restrict your Gemini API key to:
     - Only your app's package name
     - Only Android apps
     - Set usage quotas/limits
   - For HuggingFace, use API key restrictions if available

2. **Enable ProGuard/R8 Obfuscation** (Already Enabled):
   - Code obfuscation makes it harder to extract keys
   - Enabled in release builds: `isMinifyEnabled = true`

3. **For Sensitive APIs**: Use a Backend Proxy
   - Store API keys on your backend server
   - App calls your backend, backend calls the API
   - Most secure, but requires backend infrastructure

4. **Monitor API Usage**:
   - Set up alerts for unusual usage
   - Rotate keys if compromised

## Build Commands

### Debug Build (uses local.properties or gradle.properties):
```bash
./gradlew assembleDebug
```

### Release Build (uses gradle.properties or environment variables):
```bash
./gradlew assembleRelease
```

### Release Build with Environment Variables:
```bash
export GEMINI_API_KEY=your_key
export HF_API_KEY=your_key
./gradlew assembleRelease
```

## Verification

After building, verify keys are in BuildConfig:
```bash
# Decompile APK and check
aapt dump badging app-release.apk
# Or check BuildConfig in Android Studio
```

## Files Created/Modified

- ✅ `app/build.gradle.kts` - Updated to read from multiple sources
- ✅ `gradle.properties` - Can store API keys here
- ✅ `gradle.properties.example` - Template file (can commit)
- ✅ `app/proguard-rules-api-keys.pro` - Obfuscation rules for API keys
- ✅ Enabled ProGuard obfuscation for release builds

## Recommended Setup for Distribution

1. **For Production Release**:
   - Add keys to `gradle.properties` (and don't commit this file with real keys)
   - OR use environment variables in CI/CD
   - Build release APK: `./gradlew assembleRelease`

2. **For Team Development**:
   - Each developer uses `local.properties` (gitignored)
   - OR commit `gradle.properties` with keys (if team agrees)

3. **For CI/CD**:
   - Use environment variables (most secure)
   - Set in GitHub Actions / GitLab CI / etc.

The keys will be automatically compiled into the APK regardless of which method you use!

