# Offline-First Authentication Flow Diagram

## 📊 Complete Flow Visualization

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        OFFLINE SIGNUP FLOW                                   │
└─────────────────────────────────────────────────────────────────────────────┘

    USER INPUT                    PROCESSING                      STORAGE
    ─────────                     ──────────                      ───────

┌──────────────┐              ┌─────────────┐              ┌──────────────┐
│   Email:     │              │             │              │              │
│ user@ex.com  │─────────────▶│  Generate   │─────────────▶│   userId:    │
│              │              │  UUID       │              │ a1b2-c3d4... │
│   Password:  │              │             │              │              │
│  Pass123     │              └─────────────┘              │   email:     │
│              │                                           │ user@ex.com  │
│ Display Name:│              ┌─────────────┐              │              │
│  John Doe    │─────────────▶│  Hash       │─────────────▶│ passwordHash:│
└──────────────┘              │  (SHA-256)  │              │  5e884898... │
                              └─────────────┘              │              │
                                                           │ encrypted    │
                              ┌─────────────┐              │ Password:    │
       ┌─────────────────────▶│  Encrypt    │─────────────▶│  dGVzdGl=... │
       │                      │  (AES-256)  │              │              │
       │                      └─────────────┘              │ displayName: │
       │                                                   │  John Doe    │
       │                                                   │              │
       │  Store for                                        │ isFirebase   │
       │  later sync                                       │ Auth: false  │
       │                                                   │              │
       │                                                   │ syncStatus:  │
       │                                                   │  PENDING     │
       │                                                   └──────────────┘
       │                                                          │
       │                                                          ▼
       │                                                   ┌──────────────┐
       └──────────────────────────────────────────────────│ Room SQLite  │
                                                          │  Database    │
                                                          └──────────────┘

                              ✅ User can now use app offline!


┌─────────────────────────────────────────────────────────────────────────────┐
│                    AUTOMATIC SYNC WHEN ONLINE                                │
└─────────────────────────────────────────────────────────────────────────────┘

    TRIGGER                      SYNC PROCESS                    RESULT
    ───────                      ────────────                    ──────

┌──────────────┐              ┌─────────────┐              ┌──────────────┐
│              │              │             │              │              │
│  WiFi ON     │─────────────▶│ WorkManager │─────────────▶│  SyncWorker  │
│              │              │  detects    │              │   starts     │
└──────────────┘              │  network    │              └──────────────┘
                              └─────────────┘                     │
                                                                  ▼
┌──────────────┐              ┌─────────────┐              ┌──────────────┐
│  Database    │              │             │              │              │
│              │◀─────────────│   Query     │◀─────────────│ Find PENDING │
│ PENDING      │              │   users     │              │    users     │
│  users       │              │             │              │              │
└──────────────┘              └─────────────┘              └──────────────┘
      │                              │
      │                              ▼
      │                       ┌─────────────┐
      │                       │   Decrypt   │
      │                       │  Password   │
      │                       └─────────────┘
      │                              │
      │                              ▼                       ┌──────────────┐
      │                       ┌─────────────┐               │  Firebase    │
      │                       │   Create    │──────────────▶│  Auth API    │
      │                       │  Firebase   │               │              │
      │                       │   Account   │               │ Returns UID: │
      │                       └─────────────┘               │ xYz123AbC... │
      │                              │                      └──────────────┘
      │                              ▼                              │
      │                       ┌─────────────┐                       │
      │                       │   Update    │◀──────────────────────┘
      │                       │   Profile   │
      │                       └─────────────┘
      │                              │
      │                              ▼
      │                       ┌─────────────────────────────────┐
      │                       │  Update Local User Record:      │
      │                       │  • userId: UUID → Firebase UID  │
      │                       │  • isFirebaseAuth: true         │
      │                       │  • syncStatus: SYNCED           │
      │                       │  • encryptedPassword: NULL ❌   │
      │                       └─────────────────────────────────┘
      │                              │
      │                              ▼
      │                       ┌─────────────┐
      │                       │   Delete    │
      │                       │  Old UUID   │
      │                       │   Record    │
      │                       └─────────────┘
      │                              │
      ▼                              ▼
┌──────────────┐              ┌─────────────┐              ┌──────────────┐
│  Firebase    │◀─────────────│    Sync     │              │ ✅ COMPLETE  │
│  Firestore   │              │  User Data  │─────────────▶│              │
│              │              │ to Cloud    │              │ User is now  │
└──────────────┘              └─────────────┘              │ fully synced │
                                                           └──────────────┘


┌─────────────────────────────────────────────────────────────────────────────┐
│                      DATA TRANSFORMATION                                     │
└─────────────────────────────────────────────────────────────────────────────┘

BEFORE SYNC (Offline User)          AFTER SYNC (Firebase User)
──────────────────────────          ──────────────────────────

┌─────────────────────────┐         ┌─────────────────────────┐
│ userId:                 │         │ userId:                 │
│   "a1b2-c3d4-..."      │   ────▶ │   "xYz123AbC..."       │  ← Firebase UID
│                         │         │                         │
│ email:                  │         │ email:                  │
│   "user@example.com"    │   ────▶ │   "user@example.com"    │  ← Same
│                         │         │                         │
│ passwordHash:           │         │ passwordHash:           │
│   "5e884898da28..."     │   ────▶ │   "5e884898da28..."     │  ← Same (for offline login)
│                         │         │                         │
│ encryptedPassword:      │         │ encryptedPassword:      │
│   "dGVzdGl2MTI..."      │   ────▶ │   NULL                  │  ← DELETED! 🔒
│                         │         │                         │
│ displayName:            │         │ displayName:            │
│   "John Doe"            │   ────▶ │   "John Doe"            │  ← Same
│                         │         │                         │
│ isFirebaseAuth:         │         │ isFirebaseAuth:         │
│   false                 │   ────▶ │   true                  │  ← Changed!
│                         │         │                         │
│ syncStatus:             │         │ syncStatus:             │
│   PENDING               │   ────▶ │   SYNCED                │  ← Changed!
│                         │         │                         │
│ lastSyncedAt:           │         │ lastSyncedAt:           │
│   null                  │   ────▶ │   1729123456789         │  ← Timestamp added
└─────────────────────────┘         └─────────────────────────┘


┌─────────────────────────────────────────────────────────────────────────────┐
│                      SECURITY LIFECYCLE                                      │
└─────────────────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────────────────┐
│  PASSWORD STATES                                                           │
└────────────────────────────────────────────────────────────────────────────┘

  Stage 1: User Input           Stage 2: Storage           Stage 3: Cleanup
  ──────────────────           ────────────────           ────────────────

  "SecurePass123"
        │
        ├──────────────────┐
        │                  │
        ▼                  ▼
   SHA-256 Hash       AES-256 Encrypt
        │                  │
        ▼                  ▼
  "5e884898..."      "IV:EncryptedData"
        │                  │
        │                  │
        │    OFFLINE PERIOD (Days/Weeks)
        │                  │
        │                  │
        ▼                  ▼
  [Stored in DB]     [Stored in DB]
        │                  │
        │    INTERNET RETURNS
        │                  │
        │                  ├──────────▶ Decrypt
        │                  │              │
        │                  │              ▼
        │                  │        "SecurePass123"
        │                  │              │
        │                  │              ▼
        │                  │      Create Firebase Account
        │                  │              │
        │                  │              ▼
        │                  ▼           Success!
        │              DELETE ❌           │
        │                  │              │
        ▼                  ▼              ▼
  [Kept for         [REMOVED]      Firebase manages
   offline login]                   password now


┌─────────────────────────────────────────────────────────────────────────────┐
│                      ENCRYPTION DETAILS                                      │
└─────────────────────────────────────────────────────────────────────────────┘

Input: "SecurePass123"
  │
  ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 1. Generate Random IV (16 bytes)                                           │
│    ────────────────────────────────                                         │
│    Example: [0x4a, 0x7b, 0x2c, 0x9f, 0x1e, 0x8d, 0x3a, 0xb4, ...]        │
└─────────────────────────────────────────────────────────────────────────────┘
  │
  ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 2. Derive Encryption Key (PBKDF2)                                          │
│    ───────────────────────────────────                                      │
│    • Secret: "ModicareApp2025SecureOfflineSync"                            │
│    • Salt: IV (16 bytes)                                                   │
│    • Iterations: 10,000                                                    │
│    • Key Length: 256 bits                                                  │
│    • Algorithm: PBKDF2WithHmacSHA256                                       │
└─────────────────────────────────────────────────────────────────────────────┘
  │
  ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 3. Encrypt with AES-256-CBC                                                │
│    ────────────────────────────                                            │
│    • Input: "SecurePass123" (UTF-8 bytes)                                 │
│    • Key: Derived key from step 2                                         │
│    • IV: Random IV from step 1                                            │
│    • Mode: CBC (Cipher Block Chaining)                                    │
│    • Padding: PKCS5                                                       │
│    • Output: Encrypted bytes                                              │
└─────────────────────────────────────────────────────────────────────────────┘
  │
  ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 4. Encode for Storage                                                      │
│    ──────────────────────                                                  │
│    • Base64 encode IV: "dGVzdGl2MTIzNDU2Nzg="                            │
│    • Base64 encode encrypted: "ZW5jcnlwdGVkZGF0YQ=="                     │
│    • Combine: "dGVzdGl2MTIzNDU2Nzg=:ZW5jcnlwdGVkZGF0YQ=="                │
└─────────────────────────────────────────────────────────────────────────────┘
  │
  ▼
Output: "dGVzdGl2MTIzNDU2Nzg=:ZW5jcnlwdGVkZGF0YQ=="
        (Stored in encryptedPassword field)


Decryption (Reverse Process):
  │
  ▼
Split "IV:Encrypted" → Decode Base64 → Derive Key → Decrypt → "SecurePass123"
```

## 🔑 Key Takeaways

1. **Triple Protection**:
   - User's original password: Never stored
   - Hashed version (SHA-256): For offline login
   - Encrypted version (AES-256): Temporarily for Firebase migration

2. **Automatic Cleanup**:
   - Encrypted password deleted immediately after successful sync
   - Only Firebase manages the password after migration

3. **Seamless Experience**:
   - User doesn't know or care about the migration
   - Everything happens in the background
   - No manual steps required

4. **Secure by Design**:
   - AES-256 encryption (military-grade)
   - PBKDF2 key derivation (slow brute-force)
   - Random IV per encryption (no pattern)
   - Immediate cleanup after use

