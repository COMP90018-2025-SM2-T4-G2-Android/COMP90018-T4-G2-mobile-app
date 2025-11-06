# Screenshot Capture Guide - CashPal Mobile Payment App

This guide provides instructions for capturing all required screenshots for the project report.

---

## Screenshot Requirements

### Technical Specifications
- **Format**: PNG or JPEG
- **Resolution**: Minimum 1080x1920 (portrait) or 1920x1080 (landscape)
- **Quality**: High quality, clear text, good contrast
- **File Size**: Optimize for web/document (under 2MB per image recommended)

### File Organization
- Save all screenshots in: `docs/screenshots/` directory
- Use descriptive filenames (see naming convention below)
- Create subdirectories if needed for organization

---

## Required Screenshots

### 1. Android Studio Compilation Screenshot

**Purpose**: Demonstrate successful compilation

**What to Capture**:
- Android Studio Console/Build output
- Show successful build completion message
- Display build time and output directory
- Show "BUILD SUCCESSFUL" message
- Include Gradle build output

**Steps**:
1. Open Android Studio
2. Open the CashPal project
3. Build the project: `Build` → `Make Project` or `./gradlew assembleDebug`
4. Wait for build to complete
5. Open the Build output panel (View → Tool Windows → Build)
6. Take screenshot showing successful build output

**Filename**: `android_studio_compile.png`

**What to Look For**:
- ✅ "BUILD SUCCESSFUL" message
- ✅ No compilation errors
- ✅ Build time displayed
- ✅ Output APK location shown

---

### 2. Home Screen with Balance

**Purpose**: Show main app interface with balance display

**What to Capture**:
- Complete home screen
- Current balance prominently displayed
- Monthly change indicator
- Pending/reserved amounts
- Recent transactions list
- Bottom navigation bar visible
- Quick action buttons

**Steps**:
1. Launch the app
2. Log in (if not already logged in)
3. Navigate to home screen
4. Ensure balance is visible
5. Take screenshot

**Filename**: `home_screen_balance.png`

**What to Look For**:
- ✅ Balance clearly visible
- ✅ Material 3 design elements
- ✅ Transaction list showing
- ✅ Navigation bar visible

---

### 3. Payment Screen (Send Money)

**Purpose**: Demonstrate money transfer interface

**What to Capture**:
- Complete payment screen
- Contact selection interface
- Phone number input field
- Amount input field
- Send button
- Material 3 design elements

**Steps**:
1. Navigate to PayFragment (tap "Send Money" or bottom nav)
2. Show contact selection screen
3. Take screenshot of payment interface

**Filename**: `payment_screen.png`

**Alternative Screenshots**:
- `payment_contact_selection.png` - Contact list
- `payment_amount_input.png` - Amount input screen

**What to Look For**:
- ✅ Clear payment interface
- ✅ Contact selection visible
- ✅ Amount input field
- ✅ Material 3 button styling

---

### 4. QR Code Scanning Screen

**Purpose**: Show camera sensor integration

**What to Capture**:
- QR scanner camera view
- ML Kit scanning overlay/indicators
- Camera permission status (if shown)
- Scanning UI elements

**Steps**:
1. Navigate to ScanFragment
2. Grant camera permission if prompted
3. Show camera view with scanning overlay
4. Position QR code in view (optional)
5. Take screenshot

**Filename**: `qr_scan_screen.png`

**Alternative Screenshots**:
- `qr_scan_permission.png` - Permission request
- `qr_scan_result.png` - Scanned QR code result

**What to Look For**:
- ✅ Camera view active
- ✅ Scanning overlay visible
- ✅ ML Kit indicators shown
- ✅ Clear UI for scanning

---

### 5. NFC Payment Screen

**Purpose**: Demonstrate NFC sensor integration

**What to Capture**:
- NFC payment screen
- NFC status indicator
- Instructions for NFC tag detection
- Payment processing UI

**Steps**:
1. Navigate to NFC payment screen
2. Show NFC status (enabled/disabled)
3. Show instructions for tag detection
4. Take screenshot

**Filename**: `nfc_payment_screen.png`

**Alternative Screenshots**:
- `nfc_tag_detected.png` - Tag detection message
- `nfc_payment_confirmation.png` - Payment confirmation

**What to Look For**:
- ✅ NFC status displayed
- ✅ Clear instructions
- ✅ Material 3 design
- ✅ Payment processing UI

**Note**: If device doesn't have NFC, show the UI anyway and explain in report

---

### 6. Transaction History with Filters

**Purpose**: Show transaction management and filtering

**What to Capture**:
- Complete transaction history screen
- Transaction list with all transactions
- Filter options visible (month, date, state)
- Dynamic totals displayed
- Filter dropdowns/buttons

**Steps**:
1. Navigate to HistoryFragment
2. Show transaction list
3. Show filter options
4. Take screenshot

**Filename**: `transaction_history.png`

**Alternative Screenshots**:
- `transaction_history_filtered.png` - Filtered results
- `transaction_filter_menu.png` - Filter menu open
- `transaction_details.png` - Individual transaction detail

**What to Look For**:
- ✅ Transaction list visible
- ✅ Filter options shown
- ✅ Totals displayed
- ✅ Proper date/amount formatting

---

### 7. Export Options (CSV/PDF)

**Purpose**: Demonstrate export functionality

**What to Capture**:
- Export menu/dialog
- CSV and PDF options visible
- File save dialog (if possible)
- Export confirmation

**Steps**:
1. Navigate to transaction history
2. Tap export button/menu
3. Show export options (CSV/PDF)
4. Take screenshot

**Filename**: `export_options.png`

**Alternative Screenshots**:
- `export_csv_dialog.png` - CSV export dialog
- `export_pdf_preview.png` - PDF preview
- `export_success.png` - Export success message

**What to Look For**:
- ✅ Export options visible
- ✅ Clear format selection
- ✅ File location shown

---

### 8. AI Chat Assistant Screen

**Purpose**: Show AI integration

**What to Capture**:
- Complete AI chat interface
- Chat messages visible
- Input field
- AI responses shown
- Material 3 chat UI

**Steps**:
1. Navigate to AI chat screen
2. Show chat interface
3. Optionally show example conversation
4. Take screenshot

**Filename**: `ai_chat_screen.png`

**Alternative Screenshots**:
- `ai_chat_conversation.png` - Chat conversation example
- `ai_chat_input.png` - Message input

**What to Look For**:
- ✅ Clean chat interface
- ✅ Message bubbles properly formatted
- ✅ Input field visible
- ✅ Material 3 design

---

### 9. Profile/Settings Screen

**Purpose**: Show user profile and settings

**What to Capture**:
- Complete profile/settings screen
- User profile information
- Settings options visible
- Biometric preferences toggle
- Theme switcher (if visible)

**Steps**:
1. Navigate to MoreFragment/Profile screen
2. Show profile information
3. Show settings list
4. Take screenshot

**Filename**: `profile_settings.png`

**Alternative Screenshots**:
- `settings_biometric.png` - Biometric settings
- `settings_theme.png` - Theme preferences

**What to Look For**:
- ✅ Profile information displayed
- ✅ Settings options visible
- ✅ Clear UI layout

---

## Screenshot Capture Methods

### Method 1: Android Device Screenshot
**For App Screenshots**:
- **Power + Volume Down** (most Android devices)
- **Power + Home** (older devices)
- **Three-finger swipe down** (some devices)

### Method 2: Android Studio Screenshot
**For Compilation Screenshot**:
- Use Android Studio's built-in screenshot tool
- Or use system screenshot tool

### Method 3: Screen Recording
**Alternative Approach**:
- Record screen and extract frames
- Use video editing software to export frames
- Useful for capturing dynamic states

### Method 4: Emulator Screenshot
**For Development**:
- Android Studio emulator has screenshot button
- Camera icon in emulator toolbar
- Useful for consistent screenshots

---

## Screenshot Editing Tips

### Basic Editing
- **Crop**: Remove unnecessary UI elements (status bar, navigation bar if not needed)
- **Resize**: Ensure consistent dimensions if needed
- **Optimize**: Reduce file size for document inclusion
- **Annotate**: Add arrows or labels if needed (optional)

### Tools
- **Built-in**: Android screenshot editor
- **External**: GIMP, Photoshop, Canva
- **Online**: Photopea, Remove.bg

### Best Practices
- Keep screenshots consistent in size
- Use same device/emulator for consistency
- Capture in good lighting conditions
- Ensure text is readable
- Remove sensitive information if needed

---

## File Naming Convention

Use descriptive, consistent filenames:

```
android_studio_compile.png
home_screen_balance.png
payment_screen.png
qr_scan_screen.png
nfc_payment_screen.png
transaction_history.png
export_options.png
ai_chat_screen.png
profile_settings.png
```

**Pattern**: `feature_area_screen.png`

---

## Directory Structure

```
docs/
└── screenshots/
    ├── android_studio_compile.png
    ├── home_screen_balance.png
    ├── payment_screen.png
    ├── qr_scan_screen.png
    ├── nfc_payment_screen.png
    ├── transaction_history.png
    ├── export_options.png
    ├── ai_chat_screen.png
    └── profile_settings.png
```

---

## Checklist

Before submitting, ensure:
- [ ] Android Studio compilation screenshot captured
- [ ] All 8 app screenshots captured
- [ ] Screenshots are clear and readable
- [ ] File sizes are reasonable (< 2MB each)
- [ ] Files are properly named
- [ ] Files are saved in `docs/screenshots/` directory
- [ ] Screenshots show relevant features
- [ ] Material 3 design elements visible
- [ ] No sensitive information exposed

---

## Troubleshooting

### Issue: Screenshot too large
**Solution**: Compress images using image editing software or online tools

### Issue: Screenshot unclear
**Solution**: 
- Ensure device screen is clean
- Use high-resolution device/emulator
- Check screenshot settings

### Issue: Missing features in screenshot
**Solution**: 
- Scroll to show all relevant content
- Take multiple screenshots if needed
- Use landscape orientation if helpful

### Issue: Android Studio screenshot not showing
**Solution**: 
- Ensure build completed successfully
- Check Build output panel is visible
- Use system screenshot tool as alternative

---

## Additional Screenshots (Optional)

Consider capturing:
- **Splash Screen**: App launch screen
- **Login Screen**: Authentication interface
- **Error States**: Error handling examples
- **Loading States**: Loading indicators
- **Dark Theme**: Dark mode screenshots
- **Different Screen Sizes**: Tablet/phone variations

---

**Last Updated**: November 2025  
**Version**: 1.0

