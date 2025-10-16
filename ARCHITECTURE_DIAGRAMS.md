# 📊 Visual Architecture Diagrams

## 🏗️ System Architecture

```
╔══════════════════════════════════════════════════════════════════════════╗
║                          MODICARE OFFLINE-FIRST APP                       ║
╚══════════════════════════════════════════════════════════════════════════╝

┌──────────────────────────────────────────────────────────────────────────┐
│                            PRESENTATION LAYER                             │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                           │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐     │
│  │  LoginActivity   │    │  MainActivity   │    │ SettingsActivity│     │
│  │  @AndroidEntry   │    │  @AndroidEntry  │    │  @AndroidEntry  │     │
│  │     Point        │    │     Point       │    │     Point       │     │
│  └────────┬─────────┘    └────────┬────────┘    └────────┬────────┘     │
│           │                       │                       │              │
│           ▼                       ▼                       ▼              │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐     │
│  │ ModernLogin     │    │  Dashboard      │    │  SettingsScreen │     │
│  │    Screen       │    │    Screen       │    │                 │     │
│  │  (@Composable)  │    │  (@Composable)  │    │  (@Composable)  │     │
│  └─────────────────┘    └─────────────────┘    └─────────────────┘     │
│                                                                           │
└───────────────────────┬───────────────────────────────────────────────────┘
                        │
                        │ Observes StateFlows
                        │
┌───────────────────────▼───────────────────────────────────────────────────┐
│                           VIEWMODEL LAYER                                 │
├───────────────────────────────────────────────────────────────────────────┤
│                                                                            │
│  ┌─────────────────────────┐        ┌─────────────────────────┐          │
│  │    AuthViewModel         │        │    SyncViewModel         │          │
│  │    @HiltViewModel        │        │    @HiltViewModel        │          │
│  ├─────────────────────────┤        ├─────────────────────────┤          │
│  │ - authState: StateFlow  │        │ - syncState: StateFlow  │          │
│  │ - isOnline: StateFlow   │        │ - userData: StateFlow   │          │
│  │                          │        │ - unsyncedCount: Flow   │          │
│  │ + signUp()              │        │ + createData()          │          │
│  │ + login()               │        │ + syncNow()             │          │
│  │ + signOut()             │        │ + getSyncStats()        │          │
│  └────────┬────────────────┘        └────────┬────────────────┘          │
│           │                                  │                            │
└───────────┼──────────────────────────────────┼────────────────────────────┘
            │                                  │
            │ Uses                             │ Uses
            │                                  │
┌───────────▼──────────────────────────────────▼────────────────────────────┐
│                         REPOSITORY LAYER                                  │
├───────────────────────────────────────────────────────────────────────────┤
│                                                                            │
│  ┌──────────────────────┐              ┌──────────────────────┐          │
│  │   AuthRepository      │              │   DataRepository      │          │
│  │   @Singleton          │              │   @Singleton          │          │
│  ├──────────────────────┤              ├──────────────────────┤          │
│  │ + signUp()            │              │ + createData()        │          │
│  │ + login()             │              │ + updateData()        │          │
│  │ + syncOfflineUsers()  │              │ + deleteData()        │          │
│  │                       │              │ + syncPendingData()   │          │
│  └──────┬───────────┬───┘              └──────┬───────────┬───┘          │
│         │           │                          │           │              │
└─────────┼───────────┼──────────────────────────┼───────────┼──────────────┘
          │           │                          │           │
          │           │                          │           │
┌─────────▼───────┐ ┌▼──────────────┐ ┌─────────▼───────┐ ┌▼─────────────┐
│  LOCAL DATA     │ │  REMOTE DATA   │ │  LOCAL DATA     │ │ REMOTE DATA  │
│   SOURCE        │ │    SOURCE      │ │   SOURCE        │ │   SOURCE     │
├─────────────────┤ ├────────────────┤ ├─────────────────┤ ├──────────────┤
│                 │ │                │ │                 │ │              │
│ ┌─────────────┐ │ │ ┌────────────┐ │ │ ┌─────────────┐ │ │ ┌──────────┐│
│ │   UserDao   │ │ │ │  Firebase  │ │ │ │ LocalData   │ │ │ │Firestore ││
│ │             │ │ │ │    Auth    │ │ │ │    Dao      │ │ │ │DataSource││
│ └──────┬──────┘ │ │ └─────┬──────┘ │ │ └──────┬──────┘ │ │ └────┬─────┘│
│        │        │ │       │        │ │        │        │ │      │      │
│ ┌──────▼──────┐ │ │       │        │ │ ┌──────▼──────┐ │ │      │      │
│ │ AppDatabase │ │ │       │        │ │ │ AppDatabase │ │ │      │      │
│ │   (Room)    │ │ │       │        │ │ │   (Room)    │ │ │      │      │
│ └─────────────┘ │ │       │        │ │ └─────────────┘ │ │      │      │
└─────────────────┘ └───────┼────────┘ └─────────────────┘ └──────┼──────┘
                            │                                     │
                    ┌───────▼──────────┐              ┌──────────▼────────┐
                    │  Firebase Auth   │              │  Cloud Firestore  │
                    │  (Cloud)         │              │  (Cloud)          │
                    │                  │              │                   │
                    │ - User Accounts  │              │ - User Profiles   │
                    │ - Auth Tokens    │              │ - User Data       │
                    └──────────────────┘              │ - Sync Metadata   │
                                                      └───────────────────┘

┌──────────────────────────────────────────────────────────────────────────┐
│                        BACKGROUND PROCESSING                              │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                           │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                         WorkManager                               │   │
│  │                                                                   │   │
│  │   ┌────────────────────────────────────────────────────────┐    │   │
│  │   │                    SyncWorker                           │    │   │
│  │   │                @HiltWorker                              │    │   │
│  │   ├────────────────────────────────────────────────────────┤    │   │
│  │   │  1. Check network connectivity                         │    │   │
│  │   │  2. Sync unsynced users to Firebase                   │    │   │
│  │   │  3. Sync pending data to Firestore                    │    │   │
│  │   │  4. Update sync status in Room                        │    │   │
│  │   │  5. Return Result.success() or Result.retry()         │    │   │
│  │   └────────────────────────────────────────────────────────┘    │   │
│  │                                                                   │   │
│  │   Constraints:                                                    │   │
│  │   - Network: CONNECTED                                           │   │
│  │   - Battery: Not Low                                             │   │
│  │   - Retry: Exponential Backoff                                   │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                                                           │
└──────────────────────────┬────────────────────────────────────────────────┘
                           │
                           │ Triggered by
                           │
┌──────────────────────────▼────────────────────────────────────────────────┐
│                  NETWORK CONNECTIVITY OBSERVER                            │
├───────────────────────────────────────────────────────────────────────────┤
│                                                                            │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │         NetworkConnectivityObserver (Singleton)                    │  │
│  │                                                                     │  │
│  │  Monitors:                          Provides:                      │  │
│  │  - Wi-Fi connection                 - Flow<Boolean> (online/off)   │  │
│  │  - Cellular data                    - isCurrentlyConnected()       │  │
│  │  - Ethernet                          - getCurrentNetworkType()     │  │
│  │  - VPN                               - isMeteredConnection()       │  │
│  └────────────────────────────────────────────────────────────────────┘  │
│                                                                            │
└────────────────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────────────────┐
│                      DEPENDENCY INJECTION (HILT)                           │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌───────────────────────────────────────────────────────────────────┐    │
│  │               ModicareApplication (@HiltAndroidApp)               │    │
│  └───────────────────────────────────────────────────────────────────┘    │
│                                                                             │
│  ┌───────────────────────────────────────────────────────────────────┐    │
│  │                      AppModule (@Module)                          │    │
│  │                                                                    │    │
│  │  Provides:                                                         │    │
│  │  - AppDatabase               (Room database instance)             │    │
│  │  - UserDao                   (From AppDatabase)                   │    │
│  │  - LocalDataDao              (From AppDatabase)                   │    │
│  │  - FirebaseAuth              (Firebase Auth instance)             │    │
│  │  - FirebaseFirestore         (Firestore instance)                 │    │
│  │  - WorkManager               (For background tasks)               │    │
│  │  - Context                   (Application context)                │    │
│  └───────────────────────────────────────────────────────────────────┘    │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 🔄 Data Flow: Offline to Online Sync

```
┌────────────────────────────────────────────────────────────────────────┐
│                        OFFLINE DATA CREATION                            │
└────────────────────────────────────────────────────────────────────────┘

   USER ACTION: Create Medical Scan (Offline)
        │
        ▼
   ┌────────────────────┐
   │  UI Layer          │
   │  SyncViewModel     │ ──► viewModelScope.launch { createData() }
   └──────────┬─────────┘
              │
              ▼
   ┌────────────────────┐
   │  Repository Layer   │
   │  DataRepository    │ ──► createData(userId, type, content)
   └──────────┬─────────┘
              │
              ▼
   ┌────────────────────┐
   │  Data Layer        │
   │  LocalDataDao      │ ──► insertData(LocalDataEntity)
   └──────────┬─────────┘
              │
              ▼
   ┌────────────────────┐
   │  Storage           │
   │  Room Database     │ ──► Save with SyncStatus = PENDING
   └──────────┬─────────┘
              │
              ▼
        SUCCESS ✓
        Return to UI immediately
        Show "Saved locally" message

┌────────────────────────────────────────────────────────────────────────┐
│                     NETWORK BECOMES AVAILABLE                           │
└────────────────────────────────────────────────────────────────────────┘

   SYSTEM EVENT: Wi-Fi Connected
        │
        ▼
   ┌────────────────────────────┐
   │ NetworkConnectivity        │
   │ Observer                   │ ──► observe().collect { online }
   └──────────┬─────────────────┘
              │
              ▼
   ┌────────────────────────────┐
   │ AuthViewModel              │
   │ Detects: isOnline = true   │ ──► triggerSync(userId)
   └──────────┬─────────────────┘
              │
              ▼
   ┌────────────────────────────┐
   │ WorkManager                │
   │ Enqueue SyncWorker         │ ──► With CONNECTED network constraint
   └──────────┬─────────────────┘
              │
              ▼
   ┌────────────────────────────┐
   │ SyncWorker                 │
   │ doWork() starts            │
   └──────────┬─────────────────┘
              │
              ├──► Step 1: Get unsynced users
              │     DataRepository.getUnsyncedUsers()
              │
              ├──► Step 2: Sync to Firebase
              │     AuthRepository.syncOfflineUsers()
              │
              ├──► Step 3: Get unsynced data
              │     DataRepository.getUnsyncedData()
              │
              ├──► Step 4: Upload to Firestore
              │     FirestoreDataSource.batchSyncData()
              │
              ├──► Step 5: Update sync status
              │     LocalDataDao.updateSyncStatus(SYNCED)
              │
              ▼
        SYNC COMPLETE ✓
        UI updates automatically via StateFlow
        Show "All synced ✓" message
```

---

## 🎯 State Management Flow

```
┌────────────────────────────────────────────────────────────────────────┐
│                         STATE FLOW ARCHITECTURE                         │
└────────────────────────────────────────────────────────────────────────┘

   ViewModel Layer                Repository Layer              UI Layer
   
┌──────────────────┐          ┌──────────────────┐       ┌─────────────────┐
│  AuthViewModel   │          │  AuthRepository  │       │  LoginScreen    │
├──────────────────┤          ├──────────────────┤       ├─────────────────┤
│                  │          │                  │       │                 │
│ _authState:      │          │ signUp() {       │       │ val authState   │
│  MutableState    │◄─────────┤   emit(Loading)  │       │  by viewModel   │
│  Flow            │          │   ...work...     │       │  .authState     │
│                  │          │   emit(Success)  │       │  .collect...()  │
│ authState:       │          │ }                │       │                 │
│  StateFlow       │──────────┼─────────────────►│       │ when(authState) │
│  (Public)        │          │                  │       │  is Loading ->  │
│                  │          │                  │       │    Show Spinner │
└──────────────────┘          └──────────────────┘       │  is Success ->  │
                                                          │    Navigate     │
        │                                                 │  is Error ->    │
        │ Observes Network                                │    Show Error   │
        ▼                                                 └─────────────────┘
┌──────────────────┐
│ NetworkObserver  │
├──────────────────┤
│ observe():       │
│  Flow<Boolean>   │──────► Auto-triggers sync when online
└──────────────────┘

┌────────────────────────────────────────────────────────────────────────┐
│                      SEALED CLASS HIERARCHY                             │
└────────────────────────────────────────────────────────────────────────┘

    AuthState (sealed class)
    ├── Idle
    ├── Loading
    ├── Success(userId, email, isFirebaseAuth)
    ├── Error(message)
    └── Unauthenticated

    SyncState (sealed class)
    ├── Offline
    ├── Online
    ├── Syncing(progress?)
    ├── Synced
    └── Error(message)

    Benefits:
    ✓ Type-safe
    ✓ Compiler-enforced exhaustive when
    ✓ No null checks needed
    ✓ Clear state transitions
```

---

## 💾 Database Schema

```
┌────────────────────────────────────────────────────────────────────────┐
│                         ROOM DATABASE SCHEMA                            │
└────────────────────────────────────────────────────────────────────────┘

╔═══════════════════════════════════════════════════════════════════════╗
║                            USERS TABLE                                 ║
╠═══════════════════════════════════════════════════════════════════════╣
║  Column          │  Type      │  Constraints                          ║
╟──────────────────┼────────────┼───────────────────────────────────────╢
║  userId          │  String    │  PRIMARY KEY                          ║
║  email           │  String    │  NOT NULL                             ║
║  passwordHash    │  String    │  NOT NULL                             ║
║  displayName     │  String?   │  NULLABLE                             ║
║  isFirebaseAuth  │  Boolean   │  NOT NULL, DEFAULT false              ║
║  syncStatus      │  Enum      │  NOT NULL, DEFAULT PENDING            ║
║  createdAt       │  Long      │  NOT NULL                             ║
║  lastSyncedAt    │  Long?     │  NULLABLE                             ║
╚═══════════════════════════════════════════════════════════════════════╝

╔═══════════════════════════════════════════════════════════════════════╗
║                         LOCAL_DATA TABLE                               ║
╠═══════════════════════════════════════════════════════════════════════╣
║  Column          │  Type      │  Constraints                          ║
╟──────────────────┼────────────┼───────────────────────────────────────╢
║  id              │  String    │  PRIMARY KEY                          ║
║  userId          │  String    │  FOREIGN KEY → users(userId)          ║
║  dataType        │  String    │  NOT NULL                             ║
║  dataContent     │  String    │  NOT NULL (JSON)                      ║
║  metadata        │  String?   │  NULLABLE (JSON)                      ║
║  syncStatus      │  Enum      │  NOT NULL, DEFAULT PENDING            ║
║  createdAt       │  Long      │  NOT NULL                             ║
║  modifiedAt      │  Long      │  NOT NULL                             ║
║  syncedAt        │  Long?     │  NULLABLE                             ║
╠═══════════════════════════════════════════════════════════════════════╣
║  INDEXES:                                                              ║
║  - userId (for foreign key lookups)                                    ║
║  - syncStatus (for unsynced queries)                                   ║
╠═══════════════════════════════════════════════════════════════════════╣
║  FOREIGN KEYS:                                                         ║
║  - userId REFERENCES users(userId) ON DELETE CASCADE                   ║
╚═══════════════════════════════════════════════════════════════════════╝

RELATIONSHIPS:
    users (1) ────── (N) local_data
    One user can have many data entries
    Cascade delete: When user is deleted, all their data is deleted
```

---

**📐 Architecture designed for scalability, testability, and offline-first user experience! 📐**
