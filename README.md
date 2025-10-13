# CashPal - Mobile Payment App (COMP90018-T4-G2-mobile-app)

![Android Build](https://github.com/YOUR_ORG/COMP90018-T4-G2-mobile-app/workflows/Android%20PR%20Check/badge.svg)
![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Platform](https://img.shields.io/badge/platform-Android-green.svg)
![Min SDK](https://img.shields.io/badge/minSdk-24-orange.svg)

A modern Android mobile payment application built with Kotlin and Material 3 design.

## 📋 Table of Contents
- [Features](#features)
- [Getting Started](#getting-started)
- [Contributing](#contributing)
- [CI/CD](#cicd)
- [Project Structure](#project-structure)
- [License](#license)

## ✨ Features
- Modern Material 3 UI/UX
- Payment card management
- Secure card storage
- Card scanning capability (ML Kit)
- Dark/Light theme support
- Firebase integration

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog or newer
- JDK 17
- Android SDK (minSdk 24, targetSdk 34)

### Quick Start
```bash
# Clone the repository
git clone https://github.com/YOUR_ORG/COMP90018-T4-G2-mobile-app.git

# Configure commit message template
cd COMP90018-T4-G2-mobile-app
git config commit.template .gitmessage

# Open in Android Studio and sync Gradle
# Run the app
./gradlew assembleDebug
```

For detailed build instructions, see [BUILD.md](BUILD.md)

## 🤝 Contributing

We welcome contributions! Please read our [Contributing Guidelines](.github/CONTRIBUTING.md) before submitting PRs.

### Quick Guidelines
- Follow the commit message template (`.gitmessage`)
- Fill out the PR template completely
- Ensure all CI checks pass
- Write tests for new features
- Follow Material 3 design guidelines

## 🔄 CI/CD

This project uses GitHub Actions for continuous integration:
- ✅ Automated build checks on all PRs
- ✅ Lint and test execution
- ✅ APK artifact generation
- ✅ Automatic PR status updates

See [Workflow Documentation](.github/workflows/README.md) for more details.

## 📁 Project Structure
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
