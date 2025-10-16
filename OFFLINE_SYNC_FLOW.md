# Offline-First Authentication & Sync Flow

## Overview
This document explains how the app handles offline user signup and automatic synchronization to Firebase when connectivity is restored.

---

## 🔒 Security Features

### Password Encryption
- **Plain Password**: Never stored, only used during signup/login
- **Password Hash (SHA-256)**: Stored for offline authentication validation
- **Encrypted Password (AES-256)**: Temporarily stored for offline users, deleted after Firebase sync

### Encryption Details
- **Algorithm**: AES-256 in CBC mode with PBKDF2 key derivation
- **Key Derivation**: PBKDF2WithHmacSHA256, 10,000 iterations
- **IV**: Random 16-byte initialization vector per encryption
- **Storage Format**: `Base64(IV):Base64(EncryptedData)`

**Security Notes**:
- Encrypted password is ONLY stored for offline-created users
- Automatically deleted after successful sync to Firebase
- Used exclusively for migrating offline accounts to Firebase Auth

---

## 📱 Offline Signup Flow

### Step-by-Step Process

#### 1. User Fills Signup Form (No Internet)
```
Email: user@example.com
Password: SecurePass123
Display Name: John Doe
```

#### 2. Network Check
```kotlin
NetworkConnectivityObserver detects: isOnline = false
UI shows "Offline Mode" indicator
```

#### 3. Local User Creation
```kotlin
// In AuthRepository.signUp()
val userId = UUID.randomUUID().toString()  // Temporary UUID
val userEntity = UserEntity(
    userId = "a1b2c3d4-e5f6-...",           // Temporary ID
    email = "user@example.com",
    passwordHash = hashPassword(password),   // SHA-256 hash
    encryptedPassword = EncryptionUtil.encryptPassword(password), // AES-256 encrypted
    displayName = "John Doe",
    isFirebaseAuth = false,                  // Not in Firebase yet
    syncStatus = SyncStatus.PENDING          // Needs sync
)

userDao.insertUser(userEntity)
```

#### 4. Immediate App Access
- User logged in with temporary UUID
- All operations work offline (Room database)
- Data created will have `syncStatus = PENDING`
- User sees "Offline Mode - Will sync when online" message

---

## 🔄 Automatic Sync When Online

### Trigger Conditions
WorkManager automatically triggers sync when:
1. **Network Connectivity Changes**: Device goes from offline → online
2. **Periodic Sync**: Every 15 minutes (configurable)
3. **Manual Sync**: User taps "Sync Now" button
4. **App Startup**: If pending data exists

### Sync Process

#### 1. SyncWorker Detects Pending Users
```kotlin
val unsyncedUsers = userDao.getUnsyncedUsers()
// Returns users with syncStatus = PENDING or FAILED
```

#### 2. For Each Offline User:

**Step 2.1: Decrypt Password**
```kotlin
val plainPassword = EncryptionUtil.decryptPassword(user.encryptedPassword)
// Decrypts: "IV:EncryptedData" → "SecurePass123"
```

**Step 2.2: Create Firebase Account**
```kotlin
val authResult = firebaseAuth.createUserWithEmailAndPassword(
    email = "user@example.com",
    password = "SecurePass123"  // Decrypted password
).await()

val firebaseUser = authResult.user
// Firebase assigns real UID: "xYz123AbC..."
```

**Step 2.3: Update Firebase Profile**
```kotlin
if (user.displayName != null) {
    val profileUpdates = UserProfileChangeRequest.Builder()
        .setDisplayName("John Doe")
        .build()
    firebaseUser.updateProfile(profileUpdates).await()
}
```

**Step 2.4: Update Local Database**
```kotlin
val updatedUser = user.copy(
    userId = "xYz123AbC...",              // Firebase UID
    isFirebaseAuth = true,                 // Now Firebase user
    syncStatus = SyncStatus.SYNCED,        // Sync complete
    lastSyncedAt = System.currentTimeMillis(),
    encryptedPassword = null               // DELETE encrypted password
)
userDao.insertUser(updatedUser)

// Delete old record with temporary UUID
userDao.deleteUser("a1b2c3d4-e5f6-...")
```

**Step 2.5: Sync to Firestore**
```kotlin
firestoreDataSource.syncUserProfile(
    userId = "xYz123AbC...",
    email = "user@example.com",
    displayName = "John Doe"
)
```

#### 3. Sync User's Data
```kotlin
// Sync all user's pending data entries
dataRepository.syncPendingData(userId = "xYz123AbC...")
```

#### 4. Completion
- User now has full Firebase account
- All data backed up to Firestore
- Encrypted password deleted for security
- User ID updated from UUID → Firebase UID

---

## 🗄️ Database Schema

### UserEntity Structure
```kotlin
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val userId: String,              // UUID → Firebase UID after sync
    val email: String,
    val passwordHash: String,        // SHA-256 hash for offline login
    val encryptedPassword: String?,  // AES-256 encrypted, deleted after sync
    val displayName: String?,
    val isFirebaseAuth: Boolean,     // false → true after sync
    val syncStatus: SyncStatus,      // PENDING → SYNCED
    val createdAt: Long,
    val lastSyncedAt: Long?
)
```

### Database Migration
```sql
-- Version 1 → 2: Add encryptedPassword column
ALTER TABLE users ADD COLUMN encryptedPassword TEXT DEFAULT NULL
```

---

## 🔐 Security Considerations

### ✅ What We Do Right
1. **Never store plain passwords** - Only hashed or encrypted
2. **Encrypted passwords are temporary** - Deleted after sync
3. **AES-256 encryption** - Industry standard
4. **Random IV per encryption** - Prevents pattern analysis
5. **PBKDF2 key derivation** - Slows down brute force attacks

### ⚠️ Production Improvements
1. **Use Android Keystore** for encryption keys instead of hardcoded constant
2. **Implement EncryptedSharedPreferences** for sensitive data
3. **Use Bcrypt/Argon2** instead of SHA-256 for password hashing
4. **Add biometric authentication** as alternative to passwords
5. **Implement certificate pinning** for API requests
6. **Add ProGuard rules** to obfuscate encryption code

---

## 🛠️ Testing the Flow

### Manual Testing Steps

1. **Turn off WiFi/Mobile Data**
2. **Open app and signup**:
   - Email: `test@example.com`
   - Password: `TestPassword123`
   - Display Name: `Test User`
3. **Verify offline mode**:
   - Check "Offline Mode" indicator
   - Verify user can login
   - Create some data entries
4. **Turn on connectivity**
5. **Watch sync happen**:
   - Check Logcat for "Synced offline user" messages
   - Verify Firebase Authentication console shows new user
   - Check Firestore for user profile and data
6. **Verify encryption cleanup**:
   - Check database inspector: `encryptedPassword` should be NULL
   - Verify `isFirebaseAuth` is now `true`
   - Confirm `syncStatus` is `SYNCED`

### Logcat Filters
```
tag:SyncWorker OR tag:AuthRepository
```

Expected logs:
```
D/SyncWorker: Starting sync: type=full
D/AuthRepository: Synced offline user: test@example.com -> xYz123AbC...
D/SyncWorker: Synced 1 users
D/SyncWorker: Sync completed: users=1, data=5
```

---

## 📊 Sync Status States

| Status | Meaning | User Action |
|--------|---------|-------------|
| `PENDING` | Created offline, not synced | Wait for internet |
| `SYNCING` | Currently syncing to Firebase | In progress... |
| `SYNCED` | Successfully synced | ✅ Complete |
| `FAILED` | Sync failed, will retry | Check connection |

---

## 🚨 Error Handling

### Common Errors & Solutions

#### 1. "Email already exists" (Firebase)
```
Scenario: User signed up offline, then someone else used same email online
Solution: Show error, ask user to use different email or login
```

#### 2. Decryption Failed
```
Scenario: Encrypted password corrupted or encryption key changed
Solution: Mark sync as FAILED, require user to re-enter password on next login
```

#### 3. Network Timeout
```
Scenario: Sync started but network dropped
Solution: WorkManager automatically retries with exponential backoff
```

#### 4. Firebase Auth Disabled
```
Scenario: Firebase project has email/password auth disabled
Solution: Enable in Firebase Console → Authentication → Sign-in Methods
```

---

## 🔄 Data Migration Example

### Before Sync (Offline User)
```json
{
  "userId": "a1b2c3d4-e5f6-7890-...",
  "email": "user@example.com",
  "passwordHash": "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8",
  "encryptedPassword": "dGVzdGl2MTIzNDU2Nzg=:ZW5jcnlwdGVkZGF0YQ==",
  "displayName": "John Doe",
  "isFirebaseAuth": false,
  "syncStatus": "PENDING"
}
```

### After Sync (Firebase User)
```json
{
  "userId": "xYz123AbCdEfGhIjKl",  // ← Firebase UID
  "email": "user@example.com",
  "passwordHash": "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8",
  "encryptedPassword": null,  // ← DELETED
  "displayName": "John Doe",
  "isFirebaseAuth": true,  // ← Firebase authenticated
  "syncStatus": "SYNCED",  // ← Synced
  "lastSyncedAt": 1729123456789
}
```

---

## 📝 Implementation Files

### Core Components
1. **EncryptionUtil.kt** - AES-256 encryption/decryption
2. **UserEntity.kt** - Database schema with `encryptedPassword` field
3. **AuthRepository.kt** - `syncOfflineUsers()` method
4. **SyncWorker.kt** - WorkManager background sync job
5. **AppDatabase.kt** - Room database with migration v1→v2
6. **UserDao.kt** - Database queries with sync status management

### Configuration
- **AppModule.kt** - Hilt dependency injection with migration
- **ModicareApplication.kt** - WorkManager initialization
- **AndroidManifest.xml** - WorkManager auto-init disabled

---

## ✅ Benefits of This Approach

1. **✅ True Offline-First** - Users can signup/login without internet
2. **✅ Automatic Sync** - No manual intervention needed
3. **✅ Reliable** - WorkManager guarantees sync execution
4. **✅ Secure** - Encryption ensures password safety
5. **✅ Seamless** - User doesn't notice the migration
6. **✅ Conflict-Free** - Old UUID deleted, Firebase UID used
7. **✅ Privacy** - Encrypted password deleted after sync
8. **✅ Battery Efficient** - WorkManager respects battery optimization

---

## 🎯 Next Steps

- Test offline signup flow thoroughly
- Monitor sync success rate in production
- Implement retry notifications for failed syncs
- Add analytics to track offline vs online signups
- Consider adding manual "Force Sync" button in settings
