# CashPal - Mobile Payment App (COMP90018-T4-G2-mobile-app)

![Android Build](https://github.com/YOUR_ORG/COMP90018-T4-G2-mobile-app/workflows/Android%20PR%20Check/badge.svg)
![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Platform](https://img.shields.io/badge/platform-Android-green.svg)
![Min SDK](https://img.shields.io/badge/minSdk-24-orange.svg)

A modern Android mobile payment application built with Kotlin and Material 3 design.

## Table of Contents
- [Features](#features)
- [Getting Started](#getting-started)
- [Contributing](#contributing)
- [CI/CD](#cicd)
- [Project Structure](#project-structure)
- [License](#license)

## Features
- Modern Material 3 UI/UX
- Payment card management
- Secure card storage
- Card scanning capability (ML Kit)
- Dark/Light theme support
- Firebase integration

## Getting Started

### Prerequisites

Before compiling and running the CashPal application, ensure you have the following installed:

- **Android Studio**: Hedgehog (2023.1.1) or newer
- **JDK**: Version 17 or higher
- **Android SDK**: 
  - Minimum SDK: 26 (Android 8.0)
  - Target SDK: 36 (Android 14)
  - Compile SDK: 36
- **Gradle**: Version 8.0+ (included via Gradle Wrapper)

### Step-by-Step Compilation Instructions

#### Step 1: Clone the Repository

```bash
git clone https://github.com/COMP90018-2025-SM2-T4-G2-Android/COMP90018-T4-G2-mobile-app.git
cd COMP90018-T4-G2-mobile-app
```

#### Step 2: Configure API Keys

The application requires API keys for external services (Gemini AI and HuggingFace). **API keys are automatically retrieved from Firebase Remote Config** (configured in Step 3), so manual configuration is typically not required.

However, if you need to override these values locally or for CI/CD purposes, you can configure them manually in three ways (in priority order):

1. **Environment Variables** (Recommended for CI/CD):
   ```bash
   export GEMINI_API_KEY=your_gemini_api_key
   export HF_API_KEY=your_huggingface_api_key
   ```

2. **gradle.properties** (Project-level, for team builds):
   Create or edit `gradle.properties` in the project root:
   ```properties
   GEMINI_API_KEY=your_gemini_api_key
   HF_API_KEY=your_huggingface_api_key
   ```

3. **local.properties** (Local development only, gitignored):
   Add to `local.properties`:
   ```properties
   GEMINI_API_KEY=your_gemini_api_key
   HF_API_KEY=your_huggingface_api_key
   ```

For detailed API key integration instructions, refer to `docs/API_KEY_INTEGRATION.md`.

#### Step 3: Set Up Firebase

1. Create a Firebase project at [Firebase Console](https://console.firebase.google.com/)
2. Add an Android app to your Firebase project:
   - Package name: `com.cashpal.app`
   - Download `google-services.json`
   - Place `google-services.json` in the `app/` directory
3. Enable the following Firebase services:
   - **Authentication**: Email/Password and Google Sign-In
   - **Cloud Firestore**: Create database in production mode
   - **Storage**: Create default storage bucket
   - **Remote Config**: Enable Remote Config
4. Configure Firestore Security Rules (see `firestore.rules`)
5. Deploy security rules:
   ```bash
   firebase deploy --only firestore:rules
   ```

For detailed Firebase setup instructions, refer to `docs/Firebase_Database_Guide.md`.

#### Step 4: Open in Android Studio

1. Launch Android Studio
2. Select "Open" and navigate to the cloned repository directory
3. Android Studio will automatically detect the Gradle project and sync dependencies
4. Wait for Gradle sync to complete (this may take several minutes on first run)

#### Step 5: Sync Gradle

If Gradle sync doesn't happen automatically:
- Click "File" → "Sync Project with Gradle Files"
- Or use the sync icon in the toolbar

#### Step 6: Build the Application

**Debug Build**:
```bash
./gradlew assembleDebug
```

The debug APK will be generated at: `app/build/outputs/apk/debug/app-debug.apk`

**Release Build**:
```bash
./gradlew assembleRelease
```

The release APK will be generated at: `app/build/outputs/apk/release/app-release-unsigned.apk`

**Using Android Studio GUI**:
- Select "Build" → "Make Project" (or press `Ctrl+F9` / `Cmd+F9`)
- Or click the green "Run" button to build and run directly

#### Step 7: Run on Device/Emulator

**Using Android Studio**:
1. Connect an Android device via USB (enable USB debugging) OR start an Android emulator
2. Select the target device from the device dropdown
3. Click "Run" (green play button) or press `Shift+F10` / `Shift+Ctrl+R`

**Using Command Line**:
```bash
./gradlew installDebug
```

Or use ADB directly:
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Troubleshooting Common Issues

#### Issue: API Keys Not Found
**Solution**: 
- **Primary**: API keys are automatically retrieved from Firebase Remote Config. Ensure Firebase Remote Config is properly set up in Step 3 and the keys are configured in your Firebase Console.
- **Fallback**: If you need to override locally, ensure API keys are set in one of the three locations mentioned in Step 2. Check `app/build.gradle.kts` to verify keys are being read correctly.

#### Issue: Firebase Connection Errors
**Solution**: 
- Verify `google-services.json` is in the `app/` directory
- Ensure Firebase services are enabled in Firebase Console
- Check internet connection
- Verify package name matches Firebase project configuration

#### Issue: Build Failures Related to Dependencies
**Solution**:
- Clean and rebuild: `./gradlew clean build`
- Invalidate caches: File → Invalidate Caches / Restart
- Update Gradle wrapper if needed

#### Issue: Permission Errors at Runtime
**Solution**: 
- Ensure device has Android 8.0 (API 26) or higher
- Grant necessary permissions when prompted:
  - Camera (for QR scanning)
  - Location (for risk assessment)
  - NFC (for NFC payments)
  - Biometric (for authentication)

#### Issue: NFC Not Working
**Solution**: 
- Ensure device has NFC hardware
- Verify NFC is enabled in device settings
- Some features require Android 10+ for full NFC support

### Additional Resources

- API Key Setup: `docs/API_KEY_INTEGRATION.md`
- Firebase Setup: `docs/Firebase_Database_Guide.md`
- Material 3 Compliance: `docs/Material3_Compliance.md`

## Contributing

We welcome contributions! Please read our [Contributing Guidelines](.github/CONTRIBUTING.md) before submitting PRs.

### Quick Guidelines
- Follow the commit message template (`.gitmessage`)
- Fill out the PR template completely
- Ensure all CI checks pass
- Write tests for new features
- Follow Material 3 design guidelines

## CI/CD

This project uses GitHub Actions for continuous integration:
- Automated build checks on all PRs
- Lint and test execution
- APK artifact generation
- Automatic PR status updates

See [Workflow Documentation](.github/workflows/README.md) for more details.

## Project Structure
View in Code mode

```
mobile-computing-app/
 ├── app/                  # Android Studio project root<br>
 │   ├── app/              # Main app module (Java/Kotlin code)<br>
 │   ├── gradle/           # Gradle wrapper<br>
 │   └── ...               # Other Android Studio configs<br>
 ├── docs/                 # Reports, diagrams, screenshots<br>
 │   ├── report-draft.md
 │   └── screenshots/
 ├── video/                # Optional placeholder for video link<br>
 │   └── README.md         # Add YouTube link here<br>
 ├── CONTRIBUTIONS.md      # Itemised breakdown for assignment<br>
 ├── BUILD.md              # Build/run instructions<br>
 ├── .gitignore
 ├── README.md             # Overview of project (with badges/screenshots)<br>
 └── LICENSE (optional)
```
