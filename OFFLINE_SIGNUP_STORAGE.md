# 📱 Offline Signup - Where Data Gets Saved

## 🎯 Quick Answer

**When you sign up offline, your info is saved in:**
1. **Room SQLite Database** on your device (`modicare_offline_db`)
2. **Specifically in the `users` table**
3. **Location**: `/data/data/com.example.modicanalyzer/databases/modicare_offline_db`

---

## 📊 Complete Data Flow

### Step-by-Step: What Happens When You Sign Up Offline

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. USER FILLS SIGNUP FORM (No Internet)                        │
├─────────────────────────────────────────────────────────────────┤
│ Email: john.doe@example.com                                     │
│ Password: SecurePass123!                                        │
│ Confirm Password: SecurePass123!                                │
│ Display Name: John Doe                                          │
└─────────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────────┐
│ 2. VALIDATION (ValidationUtil.kt)                               │
├─────────────────────────────────────────────────────────────────┤
│ ✅ Email format valid                                           │
│ ✅ Password meets requirements                                  │
│ ✅ Passwords match                                              │
│ ✅ Name is valid                                                │
└─────────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────────┐
│ 3. NETWORK CHECK (NetworkConnectivityObserver)                 │
├─────────────────────────────────────────────────────────────────┤
│ isOnline = false                                                │
│ → Takes OFFLINE branch                                          │
└─────────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────────┐
│ 4. PASSWORD PROCESSING (AuthRepository.kt line 128-136)        │
├─────────────────────────────────────────────────────────────────┤
│ A. Generate UUID:                                               │
│    userId = "a1b2c3d4-e5f6-7890-1234-567890abcdef"            │
│                                                                 │
│ B. Hash Password (SHA-256):                                     │
│    SecurePass123! → 5e884898da28047151d0e56f8dc6292773603d... │
│    (For offline login verification)                             │
│                                                                 │
│ C. Encrypt Password (AES-256):                                  │
│    SecurePass123! → IV:EncryptedData                           │
│    (For later Firebase sync)                                    │
└─────────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────────┐
│ 5. CREATE USER ENTITY (UserEntity.kt)                          │
├─────────────────────────────────────────────────────────────────┤
│ val userEntity = UserEntity(                                    │
│     userId = "a1b2c3d4-e5f6-...",                              │
│     email = "john.doe@example.com",                             │
│     passwordHash = "5e884898da28047151d0e56f8dc...",           │
│     encryptedPassword = "dGVzdGl2MTI...:ZW5jcnlwd...",         │
│     displayName = "John Doe",                                   │
│     isFirebaseAuth = false,                                     │
│     syncStatus = SyncStatus.PENDING,                            │
│     createdAt = 1729123456789,                                  │
│     lastSyncedAt = null                                         │
│ )                                                               │
└─────────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────────┐
│ 6. SAVE TO ROOM DATABASE (UserDao.kt)                          │
├─────────────────────────────────────────────────────────────────┤
│ userDao.insertUser(userEntity)                                  │
│                                                                 │
│ Saved to: Room SQLite Database                                  │
│ Database Name: modicare_offline_db                              │
│ Table Name: users                                               │
│ Location: /data/data/com.example.modicanalyzer/databases/      │
└─────────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────────┐
│ 7. USER CAN NOW LOGIN OFFLINE ✅                                │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🗄️ Database Details

### Room SQLite Database

**Database Name**: `modicare_offline_db`

**Full Path on Device**:
```
/data/data/com.example.modicanalyzer/databases/modicare_offline_db
```

**Table Name**: `users`

**Schema**:
```sql
CREATE TABLE users (
    userId TEXT PRIMARY KEY NOT NULL,
    email TEXT NOT NULL,
    passwordHash TEXT NOT NULL,
    encryptedPassword TEXT,
    displayName TEXT,
    isFirebaseAuth INTEGER NOT NULL,
    syncStatus TEXT NOT NULL,
    createdAt INTEGER NOT NULL,
    lastSyncedAt INTEGER
);
```

---

## 📦 What Gets Stored

### Example Record in Database

```json
{
  "userId": "a1b2c3d4-e5f6-7890-1234-567890abcdef",
  "email": "john.doe@example.com",
  "passwordHash": "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8",
  "encryptedPassword": "dGVzdGl2MTIzNDU2Nzg=:ZW5jcnlwdGVkZGF0YQ==",
  "displayName": "John Doe",
  "isFirebaseAuth": 0,
  "syncStatus": "PENDING",
  "createdAt": 1729123456789,
  "lastSyncedAt": null
}
```

### Field Explanations

| Field | Value | Purpose |
|-------|-------|---------|
| **userId** | UUID string | Temporary ID (replaced with Firebase UID after sync) |
| **email** | john.doe@example.com | User's email address |
| **passwordHash** | SHA-256 hash | For offline login verification |
| **encryptedPassword** | AES-256 encrypted | For Firebase sync (deleted after sync) |
| **displayName** | John Doe | User's name |
| **isFirebaseAuth** | 0 (false) | Not yet in Firebase |
| **syncStatus** | PENDING | Waiting to sync to Firebase |
| **createdAt** | Unix timestamp | When user signed up |
| **lastSyncedAt** | null | Not synced yet |

---

## 🔍 How to View the Database

### Method 1: Android Studio Database Inspector
```
1. Run app on emulator/device
2. View → Tool Windows → App Inspection
3. Database Inspector tab
4. Select "modicare_offline_db"
5. Click "users" table
6. See all offline signups
```

### Method 2: ADB Shell
```bash
# Connect to device
adb shell

# Navigate to app's database directory
cd /data/data/com.example.modicanalyzer/databases/

# List databases
ls -la

# Open SQLite database
sqlite3 modicare_offline_db

# Query users table
SELECT * FROM users;

# Check pending users
SELECT email, syncStatus FROM users WHERE syncStatus = 'PENDING';

# Exit
.exit
```

### Method 3: Export Database File
```bash
# Pull database from device to computer
adb pull /data/data/com.example.modicanalyzer/databases/modicare_offline_db ./

# Open with SQLite browser
# Download from: https://sqlitebrowser.org/
```

---

## 🔄 What Happens When Internet Returns

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. APP DETECTS INTERNET CONNECTION                             │
├─────────────────────────────────────────────────────────────────┤
│ ModicareApplication.checkAndSyncOnStartup()                     │
│ OR                                                              │
│ Periodic WorkManager job (every 15 minutes)                     │
└─────────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────────┐
│ 2. SYNCWORKER QUERIES DATABASE                                 │
├─────────────────────────────────────────────────────────────────┤
│ userDao.getUnsyncedUsers()                                      │
│ → Returns users where syncStatus = PENDING or FAILED            │
└─────────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────────┐
│ 3. FOR EACH OFFLINE USER                                       │
├─────────────────────────────────────────────────────────────────┤
│ A. Decrypt password:                                            │
│    "dGVzdGl2..." → "SecurePass123!"                            │
│                                                                 │
│ B. Create Firebase account:                                     │
│    firebaseAuth.createUserWithEmailAndPassword(                 │
│        "john.doe@example.com",                                  │
│        "SecurePass123!"                                         │
│    )                                                            │
│    → Firebase returns UID: "xYz123AbC..."                       │
│                                                                 │
│ C. Update database record:                                      │
│    userId: UUID → Firebase UID                                  │
│    isFirebaseAuth: false → true                                 │
│    syncStatus: PENDING → SYNCED                                 │
│    encryptedPassword: "..." → NULL (deleted!)                   │
│    lastSyncedAt: null → current timestamp                       │
│                                                                 │
│ D. Delete old UUID record                                       │
└─────────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────────┐
│ 4. SYNC TO FIRESTORE                                           │
├─────────────────────────────────────────────────────────────────┤
│ firestoreDataSource.syncUserProfile(                            │
│     userId = "xYz123AbC...",                                    │
│     email = "john.doe@example.com",                             │
│     displayName = "John Doe"                                    │
│ )                                                               │
└─────────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────────┐
│ 5. FINAL STATE IN DATABASE                                     │
├─────────────────────────────────────────────────────────────────┤
│ {                                                               │
│   "userId": "xYz123AbC...",        ← Firebase UID              │
│   "email": "john.doe@example.com",                              │
│   "passwordHash": "5e884898...",                                │
│   "encryptedPassword": null,        ← DELETED!                  │
│   "displayName": "John Doe",                                    │
│   "isFirebaseAuth": 1,              ← true                      │
│   "syncStatus": "SYNCED",           ← Synced!                   │
│   "createdAt": 1729123456789,                                   │
│   "lastSyncedAt": 1729126789123     ← Timestamp added           │
│ }                                                               │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🔐 Security Features

### What's Stored Securely

1. **Password Hash (SHA-256)**
   - Stored: `5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8`
   - Purpose: Verify password during offline login
   - Security: One-way hash, cannot be reversed

2. **Encrypted Password (AES-256)**
   - Stored: `dGVzdGl2MTIzNDU2Nzg=:ZW5jcnlwdGVkZGF0YQ==`
   - Purpose: Temporary storage for Firebase sync
   - Security: Encrypted, deleted after sync
   - Format: `Base64(IV):Base64(EncryptedData)`

3. **No Plain Password**
   - ❌ Plain password NEVER stored anywhere
   - ✅ Only hashed or encrypted versions

---

## 📱 Storage Location by Platform

### Android Device/Emulator
```
Internal Storage:
/data/data/com.example.modicanalyzer/databases/modicare_offline_db

Backup Files (if enabled):
/data/data/com.example.modicanalyzer/databases/modicare_offline_db-shm
/data/data/com.example.modicanalyzer/databases/modicare_offline_db-wal
```

### Physical Size
```
Typical database size:
- Empty: ~20 KB
- 1 user: ~21 KB
- 100 users: ~40 KB
- 1000 users: ~150 KB
```

---

## 🧪 Verify Offline Signup Storage

### Test Steps

1. **Turn OFF internet**
2. **Sign up**:
   - Email: test@offline.com
   - Password: OfflineTest123!
   - Name: Test User

3. **Open Database Inspector** (Android Studio):
   ```
   View → Tool Windows → App Inspection → Database Inspector
   ```

4. **Check `users` table**:
   ```sql
   SELECT 
     userId,
     email,
     displayName,
     syncStatus,
     isFirebaseAuth
   FROM users
   WHERE email = 'test@offline.com';
   ```

5. **Expected Result**:
   ```
   userId: a1b2c3d4-... (UUID)
   email: test@offline.com
   displayName: Test User
   syncStatus: PENDING
   isFirebaseAuth: 0 (false)
   ```

6. **Turn ON internet and wait** (or restart app)

7. **Check again**:
   ```
   userId: xYz123AbC... (Firebase UID)
   email: test@offline.com
   displayName: Test User
   syncStatus: SYNCED
   isFirebaseAuth: 1 (true)
   ```

---

## 📊 Summary

### Where Offline Signup Info is Saved:

| Component | Location | Details |
|-----------|----------|---------|
| **Database** | Room SQLite | `/data/data/com.example.modicanalyzer/databases/modicare_offline_db` |
| **Table** | `users` | SQLite table with user records |
| **Storage** | Device internal storage | Not on SD card, app-private |
| **Size** | ~1-2 KB per user | Minimal storage footprint |
| **Persistence** | Until app uninstalled | Survives app restarts |
| **Security** | Encrypted & Hashed | Passwords never stored in plain text |

### What Happens Next:

```
Offline Signup → Room Database (PENDING)
                      ↓
Internet Returns → Auto-Sync to Firebase
                      ↓
Firebase Account Created → Database Updated (SYNCED)
                      ↓
Encrypted Password Deleted → Secure Storage
```

---

## 🎯 Key Takeaways

1. ✅ **Offline signups saved in Room SQLite database** on device
2. ✅ **Located at** `/data/data/com.example.modicanalyzer/databases/`
3. ✅ **Stored securely** with hashed + encrypted passwords
4. ✅ **Automatically synced** to Firebase when internet returns
5. ✅ **User can login offline** immediately after signup
6. ✅ **No manual intervention** needed for sync

**Your offline signups are safe, secure, and automatically synced!** 🔒✨
