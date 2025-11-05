# Video Script Guide - CashPal Mobile Payment App Demonstration

**Duration**: Up to 10 minutes  
**Purpose**: Comprehensive demonstration of all features according to marking rubric

---

## Pre-Recording Checklist

- [ ] App is fully built and tested
- [ ] Test accounts created in Firebase
- [ ] NFC-capable device available (if demonstrating NFC)
- [ ] QR codes prepared for scanning demonstration
- [ ] Screen recording software ready
- [ ] Good lighting and clear audio
- [ ] All features tested and working

---

## Video Structure Overview

**Total Time**: ~10 minutes  
**Distribution**: ~1 minute per major feature + transitions

1. Introduction & App Launch (0:30)
2. Authentication (0:45)
3. Home Screen & Balance (0:45)
4. Send Money (1:30)
5. QR Code Scanning (1:00)
6. NFC Payment (1:00)
7. Transaction History & Filtering (1:15)
8. CSV/PDF Export (1:00)
9. AI Assistant (1:30)
10. Profile/Settings (0:45)
11. Closing Summary (0:30)

---

## Detailed Feature Demonstration Guide

### 1. Introduction & App Launch (0:30)

**What to Show**:
- Start screen recording
- Introduce the CashPal mobile payment app
- Mention it's a modern Android app built with Kotlin and Material 3
- Show app icon and launch the app

**What to Say**:
> "Welcome to CashPal, a modern mobile payment application built for Android. This app demonstrates comprehensive mobile computing features including sensor integration, Firebase connectivity, and AI-powered assistance."

**Visual Focus**:
- App icon on home screen
- App launch animation
- Splash screen with CashPal branding

**Key Points**:
- Highlight CashPal branding on splash screen
- Show smooth app launch

---

### 2. Authentication (0:45)

**What to Show**:
- Splash screen transition to login screen
- Material 3 design elements (colors, typography, spacing)
- Email/password login OR Google Sign-In button
- Show biometric authentication prompt (if enabled)
- Successful login transition

**What to Say**:
> "CashPal supports multiple authentication methods. Users can sign in with email and password, or use Google Sign-In for convenience. The app also supports biometric authentication for enhanced security."

**Visual Focus**:
- Login screen UI showing Material 3 design
- Input fields with proper labels
- Biometric prompt (fingerprint/Face ID)
- Smooth transition after login

**Key Points**:
- Demonstrate Material 3 UI elements
- Show biometric authentication working
- Highlight security features

---

### 3. Home Screen & Balance Display (0:45)

**What to Show**:
- Main home screen layout
- Current balance prominently displayed
- Monthly change indicator
- Pending and reserved amounts
- Recent transactions list
- Bottom navigation bar
- Quick action buttons (Send Money, Scan)

**What to Say**:
> "The home screen provides a clear overview of your financial status. The balance is displayed prominently, along with monthly change indicators and pending transactions. The Material 3 design ensures excellent readability and visual hierarchy."

**Visual Focus**:
- Balance display with currency
- Monthly change indicator (positive/negative)
- Transaction list with proper formatting
- Bottom navigation showing all tabs
- Material 3 cards and colors

**Key Points**:
- Highlight Material 3 design compliance
- Show real-time balance display
- Demonstrate clean, modern UI

---

### 4. Send Money (1:30)

**What to Show**:
- Navigate to PayFragment using bottom navigation
- Show payment screen UI
- Demonstrate contact selection from contact list
- Show phone number search functionality
- Enter payment amount
- **Highlight duplicate payment alert** (if applicable)
- Show payment confirmation
- **Demonstrate balance update in real-time**
- Return to home screen showing updated balance

**What to Say**:
> "Sending money is straightforward. Users can select from their contacts or search by phone number. The app includes duplicate payment prevention to avoid accidental transfers. Notice how the balance updates immediately after the transaction completes, demonstrating real-time Firebase synchronization."

**Visual Focus**:
- Contact list with search functionality
- Phone number input and validation
- Amount input with currency formatting
- Duplicate payment alert dialog
- Payment processing loading state
- Success confirmation
- Balance updating in real-time

**Key Points**:
- Demonstrate real-time Firebase sync
- Show duplicate payment prevention
- Highlight user-friendly error handling
- Show loading states during processing

---

### 5. QR Code Scanning (1:00)

**What to Show**:
- Navigate to ScanFragment
- Show camera permission prompt (if first time)
- Camera view with ML Kit barcode scanning overlay
- Position QR code in view
- Show successful QR code detection
- Display scanned QR code information
- Process payment from QR code
- Show payment confirmation

**What to Say**:
> "CashPal uses CameraX and ML Kit for QR code scanning. The camera automatically detects QR codes, and users can quickly process payments by scanning a code. This demonstrates our camera sensor integration."

**Visual Focus**:
- Camera permission dialog
- Camera view with scanning overlay
- QR code detection animation
- Scanned data display
- Payment processing

**Key Points**:
- Demonstrate camera sensor integration
- Show ML Kit barcode scanning
- Highlight automatic detection
- Show permission handling

---

### 6. NFC Payment Receiving (1:00)

**What to Show**:
- Navigate to NFC payment screen
- Show NFC adapter detection (if available)
- Display NFC status and instructions
- Demonstrate NFC tag detection
- Show tag data reading
- Process NFC payment
- **Show sound feedback on successful payment**
- Display payment confirmation

**What to Say**:
> "The app supports NFC payments, allowing users to receive payments by tapping NFC tags. When a payment is successfully processed, you'll hear audio confirmation. This demonstrates our NFC sensor integration."

**Visual Focus**:
- NFC status indicator
- NFC tag detection message
- Payment processing animation
- Sound feedback (mention audio)
- Payment confirmation screen

**Key Points**:
- Demonstrate NFC sensor integration
- Show tag detection
- Highlight sound feedback feature
- Show NFC reader mode implementation

**Note**: If device doesn't have NFC, explain that NFC is optional and show the NFC screen UI

---

### 7. Transaction History & Filtering (1:15)

**What to Show**:
- Navigate to HistoryFragment
- Show complete transaction list
- Demonstrate month filter functionality
- Show date range filtering
- Demonstrate state filtering (sent/received/all)
- Show filtered results
- **Display dynamic totals (sent/received reflect filtered list)**
- Show transaction details on tap

**What to Say**:
> "Transaction history provides comprehensive filtering options. Users can filter by month, date range, and transaction type. Notice how the totals dynamically update based on the applied filters. This demonstrates responsive UI design and data processing."

**Visual Focus**:
- Transaction list with proper formatting
- Filter dropdowns/buttons
- Filtered results showing
- Dynamic totals updating
- Transaction detail view
- Date formatting and currency display

**Key Points**:
- Demonstrate advanced filtering
- Show dynamic totals calculation
- Highlight responsive UI
- Show proper data formatting

---

### 8. CSV/PDF Export (1:00)

**What to Show**:
- Show export options in transaction history
- Demonstrate CSV export:
  - Tap export button
  - Select CSV option
  - Show file location/save dialog
  - Open exported CSV file (if possible)
- Demonstrate PDF export:
  - Select PDF option
  - Show PDF generation
  - Display generated PDF preview
- Highlight export functionality

**What to Say**:
> "Users can export their transaction history in CSV or PDF format. This is useful for financial record-keeping and tax purposes. The export includes all transaction details with proper formatting."

**Visual Focus**:
- Export button/menu
- Format selection (CSV/PDF)
- File save dialog
- Exported file preview
- PDF formatting and layout

**Key Points**:
- Demonstrate data export functionality
- Show proper file generation
- Highlight professional formatting
- Show file system integration

---

### 9. AI Assistant Chat (1:30)

**What to Show**:
- Navigate to AI chat screen
- Show chat interface with Material 3 design
- Demonstrate asking financial questions:
  - "What's my current balance?"
  - "Show me my recent transactions"
  - "How much did I spend this month?"
- Show context-aware responses
- Highlight integration with user data
- Show AI understanding of user context

**What to Say**:
> "CashPal includes an AI-powered financial assistant powered by Google's Gemini API. The assistant has access to your transaction data and can answer questions about your finances. Notice how it provides context-aware responses based on your actual data."

**Visual Focus**:
- Chat interface UI
- Message bubbles with proper formatting
- AI response generation
- Context-aware answers
- User data integration in responses

**Key Points**:
- Demonstrate AI integration
- Show context awareness
- Highlight user data integration
- Show natural language processing

---

### 10. Profile/Settings (0:45)

**What to Show**:
- Navigate to MoreFragment/Profile screen
- Show profile information display
- Demonstrate settings options:
  - Biometric preferences toggle
  - Notification settings
  - Theme preferences (light/dark)
- Show biometric authentication settings
- Display user profile information

**What to Say**:
> "The settings screen allows users to customize their experience. Users can enable or disable biometric authentication, adjust notification preferences, and switch between light and dark themes."

**Visual Focus**:
- Profile screen layout
- Settings list with toggles
- Biometric preferences
- Theme switcher
- User information display

**Key Points**:
- Show settings management
- Demonstrate biometric preferences
- Highlight theme support
- Show user profile information

---

### 11. Closing Summary (0:30)

**What to Show**:
- Quick recap of main features
- Show app logo/name
- End screen with app information

**What to Say**:
> "CashPal demonstrates comprehensive mobile computing features including sensor integration, Firebase connectivity, AI assistance, and modern Material 3 design. Thank you for watching this demonstration."

**Visual Focus**:
- App overview
- Feature highlights
- Professional closing

**Key Points**:
- Summarize key features
- Highlight technical achievements
- Professional conclusion

---

## Technical Requirements

### Recording Setup
- **Orientation**: Landscape preferred for better visibility
- **Resolution**: Minimum 1080p (1920x1080)
- **Frame Rate**: 30fps minimum, 60fps preferred
- **Audio**: Clear narration with minimal background noise
- **Screen Recording**: Use native Android screen recording or third-party tool

### Recording Tips
1. **Test First**: Record a test run to check audio and video quality
2. **Prepare Script**: Have this script nearby for reference
3. **Smooth Transitions**: Plan transitions between features
4. **Focus on Features**: Keep each feature demo concise (~1 minute)
5. **Show Interactions**: Clearly demonstrate user interactions
6. **Highlight Key Points**: Emphasize sensor integrations, real-time updates, Material 3 design

### Post-Production
- Trim unnecessary pauses
- Add brief text overlays for feature names (optional)
- Ensure audio is clear and synchronized
- Add intro/outro if desired
- Keep total length under 10 minutes

---

## Key Demonstrations to Highlight

### Must Show:
1. ✅ **Balance updates after transactions** (real-time Firebase sync)
2. ✅ **Real-time Firebase synchronization** (show balance updating)
3. ✅ **Sensor integrations** (camera, NFC, biometric, location)
4. ✅ **Material 3 UI design elements** (throughout the app)
5. ✅ **Dark/light theme support** (if time permits)
6. ✅ **Error handling and user feedback** (show error states)
7. ✅ **Loading states and responsiveness** (show loading indicators)

### Bonus Points:
- Show multiple users/accounts interacting
- Demonstrate offline behavior (if implemented)
- Show accessibility features
- Demonstrate edge cases and error handling

---

## Common Issues & Solutions

### Issue: NFC not working
**Solution**: Explain that NFC requires compatible hardware, show the UI and explain the feature

### Issue: Camera permission denied
**Solution**: Show permission request flow, explain security features

### Issue: Slow loading
**Solution**: Mention that loading states are shown, demonstrate proper error handling

### Issue: Audio not clear
**Solution**: Use external microphone or re-record narration

---

## Upload Guidelines

### YouTube Upload Checklist:
- [ ] Video is under 10 minutes
- [ ] Audio is clear and synchronized
- [ ] All features are demonstrated
- [ ] Video is properly titled: "CashPal Mobile Payment App - Feature Demonstration"
- [ ] Description includes:
  - Brief app description
  - Key features demonstrated
  - Technology stack used
  - Link to GitHub repository
- [ ] Video is set to "Unlisted" or "Public" (as required)
- [ ] Thumbnail is clear and representative

### Video Description Template:
```
CashPal - Mobile Payment App Feature Demonstration

This video demonstrates the CashPal mobile payment application, showcasing:
- Firebase Authentication & Real-time Database
- NFC Payment Receiving
- QR Code Scanning with ML Kit
- AI-powered Financial Assistant
- Transaction History with Export
- Material 3 Design

Built with: Kotlin, Android SDK, Firebase, Gemini AI, CameraX, ML Kit

GitHub: [Repository Link]
Group: G2 | COMP90018 - Mobile Computing | T4 2025
```

---

## Final Checklist

Before finalizing the video:
- [ ] All major features demonstrated
- [ ] Sensor integrations shown (Camera, NFC, Biometric, Location)
- [ ] Real-time updates demonstrated
- [ ] Material 3 design highlighted
- [ ] AI assistant shown with context awareness
- [ ] Export functionality demonstrated
- [ ] Video is under 10 minutes
- [ ] Audio is clear
- [ ] Video quality is good
- [ ] YouTube link ready for report

---

**Good luck with your recording!**

