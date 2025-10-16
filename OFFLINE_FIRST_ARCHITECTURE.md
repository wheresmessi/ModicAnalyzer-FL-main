# Offline-First Firebase Android App - Complete Guide

## 📱 Overview

This is a **production-ready offline-first Android application** built with Kotlin, featuring:
- **Firebase Authentication** for secure user management
- **Room (SQLite)** for local offline data storage
- **Firestore** for cloud database synchronization
- **WorkManager** for reliable background sync
- **MVVM Architecture** with Repository pattern
- **Hilt** for dependency injection
- **Jetpack Compose** for modern UI

---

## 🏗️ Architecture

```
app/
├── data/
│   ├── local/                    # Local database (Room)
│   │   ├── dao/                  # Data Access Objects
│   │   │   ├── UserDao.kt
│   │   │   └── LocalDataDao.kt
│   │   ├── entity/               # Database entities
│   │   │   ├── UserEntity.kt
│   │   │   └── LocalDataEntity.kt
│   │   ├── AppDatabase.kt        # Room database
│   │   └── Converters.kt         # Type converters
│   ├── remote/                   # Remote data source
│   │   └── FirestoreDataSource.kt
│   ├── repository/               # Repository layer
│   │   ├── AuthRepository.kt
│   │   └── DataRepository.kt
│   └── model/                    # Data models
│       ├── SyncStatus.kt
│       ├── AuthState.kt
│       └── SyncState.kt
├── viewmodel/                    # ViewModels
│   ├── AuthViewModel.kt
│   └── SyncViewModel.kt
├── ui/
│   └── screens/                  # Composable screens
│       └── ModernLoginScreen.kt
├── worker/                       # Background workers
│   └── SyncWorker.kt
├── util/                         # Utilities
│   └── NetworkConnectivityObserver.kt
├── di/                           # Dependency injection
│   └── AppModule.kt
└── ModicareApplication.kt        # Application class
```

---

## 🔄 Offline-First Data Flow

### 1. **User Signup (Offline)**
```
User enters credentials → Save to Room with synced=false
                       → Generate local UUID as userId
                       → Password hashed with SHA-256
                       → SyncStatus = PENDING
```

### 2. **User Signup (Online)**
```
User enters credentials → Create Firebase Auth user
                       → Save to Room with Firebase UID
                       → Sync profile to Firestore
                       → SyncStatus = SYNCED
```

### 3. **Data Creation**
```
User creates data → Save to Room immediately
                  → Set SyncStatus = PENDING
                  → Return success to UI
                  → (Background) WorkManager syncs to Firestore
                  → Update SyncStatus = SYNCED
```

### 4. **Network State Changes**
```
Offline → Online → NetworkConnectivityObserver detects
                 → Trigger SyncWorker via WorkManager
                 → Sync unsynced users
                 → Sync unsynced data
                 → Update SyncStatus
```

---

## 📊 Database Schema

### Users Table (Room)
| Column          | Type    | Description                          |
|-----------------|---------|--------------------------------------|
| userId          | String  | Primary Key (Firebase UID or UUID)   |
| email           | String  | User's email address                 |
| passwordHash    | String  | Hashed password for offline auth     |
| displayName     | String? | User's display name                  |
| isFirebaseAuth  | Boolean | True if Firebase user                |
| syncStatus      | Enum    | PENDING/SYNCING/SYNCED/FAILED        |
| createdAt       | Long    | Timestamp of creation                |
| lastSyncedAt    | Long?   | Last successful sync timestamp       |

### LocalData Table (Room)
| Column      | Type   | Description                              |
|-------------|--------|------------------------------------------|
| id          | String | Primary Key (UUID)                       |
| userId      | String | Foreign Key to Users table               |
| dataType    | String | Type of data (e.g., "scan_result")       |
| dataContent | String | JSON string of actual data               |
| metadata    | String?| Additional metadata                      |
| syncStatus  | Enum   | PENDING/SYNCING/SYNCED/FAILED            |
| createdAt   | Long   | Creation timestamp                       |
| modifiedAt  | Long   | Last modification timestamp              |
| syncedAt    | Long?  | Last sync timestamp                      |

---

## 🔐 Authentication Flow

### Firebase Auth Integration
```kotlin
// AuthRepository handles both offline and online auth
authRepository.signUp(email, password, displayName, isOnline)
    .collect { authState ->
        when (authState) {
            is AuthState.Loading -> // Show loading
            is AuthState.Success -> // Navigate to main screen
            is AuthState.Error -> // Show error message
        }
    }
```

### Offline Login
```kotlin
// User logs in offline (no internet)
authRepository.login(email, password, isOnline = false)
// Validates against local Room database
// Password verified using stored hash
```

### Online Login
```kotlin
// User logs in online
authRepository.login(email, password, isOnline = true)
// Tries Firebase Auth first
// Falls back to local auth if Firebase fails
// Updates local database with Firebase data
```

---

## 🔄 Synchronization System

### WorkManager Configuration
```kotlin
// Automatic sync on network availability
val constraints = Constraints.Builder()
    .setRequiredNetworkType(NetworkType.CONNECTED)
    .build()

val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
    .setConstraints(constraints)
    .setInputData(workDataOf(
        SyncWorker.KEY_USER_ID to userId,
        SyncWorker.KEY_SYNC_TYPE to SyncWorker.SYNC_TYPE_FULL
    ))
    .build()
```

### Sync Worker Process
```kotlin
// SyncWorker.doWork()
1. Check network connectivity
2. Sync unsynced users to Firebase Auth
3. Sync user's pending data to Firestore
4. Update sync status in Room database
5. Return Result.success() or Result.retry()
```

### Manual Sync Trigger
```kotlin
// User presses "Sync Now" button
syncViewModel.syncNow()
// Immediately triggers WorkManager sync job
// Shows sync progress in UI
```

---

## 🌐 Network Connectivity Monitoring

```kotlin
// NetworkConnectivityObserver provides reactive Flow
networkObserver.observe()
    .collect { isOnline ->
        if (isOnline) {
            // Trigger sync
            // Update UI to show online status
        } else {
            // Show offline banner
            // Disable online-only features
        }
    }
```

### Features:
- Real-time connectivity updates
- Detects Wi-Fi, Cellular, Ethernet
- Battery-efficient implementation
- Automatic sync trigger on reconnection

---

## 🎨 UI State Management

### AuthState (Sealed Class)
```kotlin
sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(
        val userId: String,
        val email: String,
        val isFirebaseAuth: Boolean
    ) : AuthState()
    data class Error(val message: String) : AuthState()
    object Unauthenticated : AuthState()
}
```

### SyncState (Sealed Class)
```kotlin
sealed class SyncState {
    object Offline : SyncState()
    object Online : SyncState()
    data class Syncing(val progress: Int? = null) : SyncState()
    object Synced : SyncState()
    data class Error(val message: String) : SyncState()
}
```

### Usage in Compose
```kotlin
val authState by authViewModel.authState.collectAsStateWithLifecycle()
val isOnline by authViewModel.isOnline.collectAsStateWithLifecycle()

when (authState) {
    is AuthState.Loading -> ShowLoadingSpinner()
    is AuthState.Success -> NavigateToMainScreen()
    is AuthState.Error -> ShowErrorMessage()
}
```

---

## 🧪 Testing the Offline-First System

### Test Scenario 1: Offline Signup
```
1. Turn off Wi-Fi and mobile data
2. Open the app
3. Sign up with new credentials
4. Verify user is saved in Room database
5. Check SyncStatus = PENDING
6. Turn on internet
7. Verify user is synced to Firebase
8. Check SyncStatus = SYNCED
```

### Test Scenario 2: Offline Data Creation
```
1. Log in to the app (online)
2. Turn off internet
3. Create new data entry (e.g., medical scan)
4. Verify data is saved locally
5. Check "Unsynced: 1" in UI
6. Turn on internet
7. Verify data is synced to Firestore
8. Check "All synced ✓" in UI
```

### Test Scenario 3: Network Interruption
```
1. Start sync process
2. Turn off internet mid-sync
3. Verify WorkManager retries automatically
4. Turn on internet
5. Verify sync completes successfully
```

---

## 🔧 Configuration

### Dependencies (build.gradle.kts)
```kotlin
// Room
implementation("androidx.room:room-runtime:2.6.1")
kapt("androidx.room:room-compiler:2.6.1")

// WorkManager
implementation("androidx.work:work-runtime-ktx:2.9.0")

// Hilt
implementation("com.google.dagger:hilt-android:2.50")
kapt("com.google.dagger:hilt-android-compiler:2.50")

// Firebase
implementation("com.google.firebase:firebase-auth-ktx:22.4.0")
implementation("com.google.firebase:firebase-firestore-ktx:24.10.3")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
```

### AndroidManifest.xml
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<application
    android:name=".ModicareApplication"
    ...>
```

---

## 📈 Sync Statistics

```kotlin
// Get sync stats for current user
val stats = syncViewModel.getSyncStats()

// Display in UI
Text("Total: ${stats.totalEntries}")
Text("Synced: ${stats.syncedEntries}")
Text("Pending: ${stats.pendingEntries}")
Text("Failed: ${stats.failedEntries}")
```

---

## 🚀 Best Practices

### 1. **Always Save Locally First**
```kotlin
// ❌ Bad: Wait for Firestore before confirming
val result = firestoreDataSource.syncData(...)
if (result.isSuccess) {
    localDataDao.insertData(...)
}

// ✅ Good: Save locally immediately
localDataDao.insertData(...)
// Background sync happens automatically
```

### 2. **Handle Network Changes**
```kotlin
// Always observe network state
viewModelScope.launch {
    networkObserver.observe().collect { isOnline ->
        if (isOnline) triggerSync()
    }
}
```

### 3. **Use Sealed Classes for States**
```kotlin
// Type-safe state management
when (val state = authState.value) {
    is AuthState.Success -> println(state.userId)
    is AuthState.Error -> println(state.message)
    // Compiler ensures all cases are handled
}
```

### 4. **Implement Proper Error Handling**
```kotlin
try {
    dataRepository.createData(...)
} catch (e: Exception) {
    _syncState.value = SyncState.Error(e.message ?: "Unknown error")
}
```

---

## 📝 Common Use Cases

### Create Data Entry
```kotlin
// From ViewModel
viewModelScope.launch {
    val result = dataRepository.createData(
        userId = currentUserId,
        dataType = "medical_scan",
        dataContent = scanJsonString,
        metadata = metadataJsonString
    )
    
    if (result.isSuccess) {
        // Data saved locally, will sync in background
    }
}
```

### Manual Sync Trigger
```kotlin
// From UI
Button(onClick = { syncViewModel.syncNow() }) {
    Text("Sync Now")
}
```

### Observe Sync Status
```kotlin
val unsyncedCount by syncViewModel.unsyncedCount.collectAsStateWithLifecycle()
Text("Unsynced items: $unsyncedCount")
```

---

## 🎯 Key Features

✅ **Offline-first**: Works without internet connection
✅ **Automatic sync**: Background sync when online
✅ **Conflict resolution**: Last-write-wins strategy
✅ **Type-safe**: Sealed classes for state management
✅ **Reactive UI**: Flow and StateFlow for real-time updates
✅ **Dependency injection**: Hilt for clean architecture
✅ **Background jobs**: WorkManager with retry logic
✅ **Network awareness**: Automatic connectivity monitoring

---

## 📞 Support & Documentation

For questions or issues:
1. Check this README first
2. Review inline code comments (every class is documented)
3. Test with Wi-Fi off/on to understand offline behavior
4. Check WorkManager logs in Logcat for sync status

---

## 🔒 Security Notes

1. **Password Hashing**: Uses SHA-256 (upgrade to bcrypt/argon2 for production)
2. **Local Storage**: Room database (consider encryption for sensitive data)
3. **Network Security**: Uses HTTPS for all Firebase communication
4. **Authentication**: Firebase Auth provides secure token-based auth

---

**Built with ❤️ for offline-first mobile experiences**
