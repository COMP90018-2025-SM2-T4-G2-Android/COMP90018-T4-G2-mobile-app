# Team Contributions - CashPal Mobile Payment App

**Group**: G2  
**Course**: COMP90018 - Mobile Computing  
**Semester**: T4 2025

---

## Contribution Breakdown

This document provides an itemized breakdown of contributions by each team member. Contributions are categorized by feature area and specific tasks implemented.

| Team Member | Contribution Area | Specific Tasks/Features | Code Files | Git Commits |
|-------------|-------------------|------------------------|------------|-------------|
| **John Minseok Kim** | [To be filled] | [To be filled] | [To be filled] | [To be filled] |
| **Emily Trudgen** | [To be filled] | [To be filled] | [To be filled] | [To be filled] |
| **Litong Lu** | [To be filled] | [To be filled] | [To be filled] | [To be filled] |
| **Erick Wong** | [To be filled] | [To be filled] | [To be filled] | [To be filled] |
| **Ruiting Li** | [To be filled] | [To be filled] | [To be filled] | [To be filled] |
| **Jianan Zheng** | [To be filled] | [To be filled] | [To be filled] | [To be filled] |

---

## Feature Areas

### Firebase Integration
- **Authentication**: Email/password, Google Sign-In
- **Firestore Database**: User data, transactions, contacts
- **Storage**: Profile images, QR codes, receipts
- **Remote Config**: API key management
- **Security Rules**: Firestore security rules deployment

**Related Files**:
- `app/src/main/java/com/cashpal/app/repository/CashPalRepository.kt`
- `app/src/main/java/com/cashpal/app/services/FirebaseConfigService.kt`
- `firestore.rules`

### NFC Payment Implementation
- NFC reader mode implementation
- Tag detection and handling
- NDEF message parsing
- Sound feedback on payment
- Payment processing integration

**Related Files**:
- `app/src/main/java/com/cashpal/app/fragments/NfcPaymentFragment.kt`
- `app/src/main/AndroidManifest.xml` (NFC permissions)

### QR Code Scanning
- CameraX integration
- ML Kit barcode scanning
- QR code parsing and validation
- Camera permission handling
- Payment processing from QR codes

**Related Files**:
- `app/src/main/java/com/cashpal/app/fragments/ScanFragment.kt`
- `app/src/main/AndroidManifest.xml` (Camera permissions)

### AI Assistant
- Gemini API integration
- Context-aware conversation handling
- User data integration
- Chat UI implementation
- AI service abstraction

**Related Files**:
- `app/src/main/java/com/cashpal/app/services/AIAssistantManager.kt`
- `app/src/main/java/com/cashpal/app/services/GeminiService.kt`
- `app/src/main/java/com/cashpal/app/AIChatActivity.kt`

### UI/UX Design
- Material 3 compliance
- Fragment layouts and navigation
- Color scheme and theming
- Dark mode support
- Animations and transitions

**Related Files**:
- `app/src/main/res/values/colors.xml`
- `app/src/main/res/values-night/colors.xml`
- `app/src/main/res/values/typography.xml`
- `app/src/main/res/values/dimens.xml`
- `app/src/main/res/layout/` (all layout files)
- `docs/Material3_Compliance.md`

### Transaction Processing
- Repository pattern implementation
- Balance synchronization
- Atomic batch writes
- Transaction history management
- Contact auto-creation

**Related Files**:
- `app/src/main/java/com/cashpal/app/repository/CashPalRepository.kt`
- `app/src/main/java/com/cashpal/app/fragments/PayFragment.kt`
- `app/src/main/java/com/cashpal/app/fragments/HistoryFragment.kt`

### Export Functionality
- CSV export generation
- PDF export generation
- Transaction filtering
- File system integration

**Related Files**:
- `app/src/main/java/com/cashpal/app/fragments/HistoryFragment.kt`

### Backend Services
- Service Locator pattern
- Dependency injection
- Service abstractions

**Related Files**:
- `app/src/main/java/com/cashpal/app/di/ServiceLocator.kt`

### Sensor Integration
- Biometric authentication
- Location services
- Location risk assessment

**Related Files**:
- `app/src/main/java/com/cashpal/app/utils/BiometricAuthManager.kt`
- `app/src/main/java/com/cashpal/app/utils/BiometricPreferences.kt`
- `app/src/main/java/com/cashpal/app/utils/LocationHelper.kt`
- `app/src/main/java/com/cashpal/app/utils/LocationRisk.kt`

### Testing & Documentation
- Unit tests
- Integration tests
- Technical documentation
- API documentation

**Related Files**:
- `app/src/test/`
- `app/src/androidTest/`
- `docs/` directory

---

## Git Commit Analysis

Recent commits demonstrate collaborative development:

- `63072b1` - Add duplicate payment alert before processing transfers
- `be96704` - Revert Merge Main To Dev (#20)
- `51d6cb2` - Merge branch 'main' into dev
- `689e95e` - Feature enhancement - Backend, history, activate camera, exports, UI, permission, balance history, AI, etc.
- `156325e` - Refresh balance after payment and improve logging
- `7dd6608` - Improve contact handling and auto-create contacts after transactions
- `959204c` - Add camera permission handling to ScanFragment
- `7124ed7` - Add export to CSV and PDF in transaction history
- `6c9d936` - Add month filter and improve transaction filtering
- `403644a` - Add phone number support to sign up and payments
- `8fa6c25` - Improve balance sync and transaction handling
- `fcb549f` - Merge pull request #17 - AI assistance feature

---

## Notes

- This contribution breakdown should be filled in by team members based on actual work performed
- Git commit history can be analyzed using: `git log --pretty=format:"%h - %an, %ar : %s"`
- Code review and pull request history provide additional contribution evidence
- Contribution percentages should reflect actual time and effort spent on each area

---

**Last Updated**: November 2025  
**Version**: 1.0

