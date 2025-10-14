# ModicAnalyzer Authentication System

## Overview
Added complete dummy authentication system with login, signup, and user session management.

## Features Implemented

### 🔐 Login Screen (`LoginActivity.kt`)
- **Email & Password Fields** with proper validation
- **Password Visibility Toggle** for better UX
- **Forgot Password** placeholder (shows toast message)
- **Demo User Access** - Skip authentication for testing
- **Auto-redirect** if user already logged in
- **Professional medical theme** with clean UI

### ✍️ Signup Screen (`SignupActivity.kt`)
- **Complete Registration Form:**
  - Full Name
  - Email Address
  - Phone Number
  - Role Selection (Patient, Doctor, Radiologist, Researcher)
  - Password & Confirm Password
- **Form Validation:**
  - All fields required
  - Password confirmation matching
  - Minimum password length (6 characters)
  - Terms & conditions acceptance
- **Role-based Registration** for different user types
- **Scrollable Layout** for better mobile experience

### 👤 User Session Management (`AuthManager.kt`)
- **Persistent Login State** using SharedPreferences
- **User Profile Storage** (name, email, role)
- **Demo User Support** for testing
- **Secure Logout** functionality
- **Session Validation** across app restarts

### 🎯 Enhanced Settings Screen
- **User Profile Display** showing current logged-in user
- **Role-based Information** (Patient, Doctor, etc.)
- **Logout Functionality** with confirmation
- **Clean UI Integration** with existing settings

### 🏠 Enhanced Main Screen
- **Welcome Message** showing logged-in user name
- **Dynamic TopBar** with user context
- **Seamless Integration** with existing functionality

## User Flow

```
1. App Launch → LoginActivity (launcher)
2. Login Options:
   - Enter credentials → Main App
   - Sign Up → SignupActivity → Main App  
   - Demo Access → Main App (as demo user)
3. Settings → View Profile & Logout → Back to Login
4. Auto-login on app restart if user logged in
```

## Authentication States

### 🟢 Logged In User
- Shows personalized welcome message
- Profile visible in settings
- Session persists across app restarts
- Full app functionality

### 🔵 Demo User  
- Quick access without registration
- Limited profile info
- Can still use all app features
- Shows "Demo User" in settings

### 🔴 Logged Out
- Redirected to login screen
- No session data stored
- Clean authentication state

## Technical Implementation

### Session Management
```kotlin
// Login user
authManager.login(email, name, role)

// Check login status
if (authManager.isLoggedIn()) { /* user authenticated */ }

// Get user info
val userName = authManager.getUserName()
val userRole = authManager.getUserRole()

// Logout
authManager.logout()
```

### Navigation Flow
- **LoginActivity** → Primary launcher activity
- **SignupActivity** → Registration flow
- **SimpleMainActivity** → Main app (requires authentication)
- **SettingsActivity** → User management & logout

## Dummy Data
- **Demo Login:** Any email/password combination works
- **Demo User:** demo@modicanalyzer.com (Patient)
- **Roles Available:** Patient, Doctor, Radiologist, Researcher
- **No Real Backend:** Pure frontend implementation

## UI/UX Features
- ✅ **Light Theme Only** - Professional medical appearance
- ✅ **Material 3 Design** - Modern Android UI components
- ✅ **Consistent Branding** - ModicAnalyzer medical theme
- ✅ **Form Validation** - User-friendly error messages
- ✅ **Loading States** - Progress indicators during actions
- ✅ **Accessibility** - Proper content descriptions and labels

## Ready for Integration
- Easy to integrate with real backend APIs
- Token-based authentication support ready
- User profile extensible for more fields
- Role-based access control foundation set

This provides a complete, professional authentication experience that matches the medical nature of the ModicAnalyzer application!

## Firebase Authentication (optional)

This project now includes optional Firebase Authentication support. The app will try to use Firebase at runtime when available; if Firebase is not configured (for example, `google-services.json` is not present), the app falls back to the local/demo SharedPreferences-based session so the UI and flows continue to work.

How to enable Firebase Auth:

1. Create a Firebase project at https://console.firebase.google.com/ and enable Email/Password sign-in in Authentication > Sign-in method.
2. In Firebase project settings, download the Android `google-services.json` file and place it into the project's `app/` directory.
3. Add the Google Services Gradle plugin to the project-level `build.gradle` classpath:

   buildscript {
     dependencies {
       classpath 'com.google.gms:google-services:4.3.15'
     }
   }

   Or with the new settings, add the plugin classpath to your Gradle settings as appropriate for your Gradle version.

4. (Optional) Uncomment the `apply(plugin = "com.google.gms.google-services")` line in `app/build.gradle.kts` to enable the google-services plugin. The codebase keeps this commented by default to avoid build errors when `google-services.json` is not present.

5. Rebuild the project in Android Studio. After configuring Firebase and enabling Email/Password authentication, sign-up and login will use Firebase; otherwise the app uses a local demo mode.

Notes:
- The code uses `AuthManager` to wrap Firebase calls and provide a safe fallback.
- No backend server changes are required for basic Firebase Auth usage, but storing additional user profile fields server-side requires a backend (Firestore/RealtimeDB or your own API).