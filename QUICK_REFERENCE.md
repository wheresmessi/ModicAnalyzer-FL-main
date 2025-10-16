# 🚀 Quick Reference Guide - Offline-First Operations

## 📱 Common Operations Cheat Sheet

### 1. User Authentication

#### Signup (Offline/Online)
```kotlin
// In your ViewModel or Activity
authViewModel.signUp(
    email = "user@example.com",
    password = "password123",
    displayName = "John Doe"
)

// Observe result
authViewModel.authState.collect { state ->
    when (state) {
        is AuthState.Success -> {
            // Navigate to main screen
            // state.userId, state.email, state.isFirebaseAuth
        }
        is AuthState.Error -> {
            // Show error: state.message
        }
        is AuthState.Loading -> {
            // Show loading spinner
        }
    }
}
```

#### Login (Offline/Online)
```kotlin
authViewModel.login(
    email = "user@example.com",
    password = "password123"
)
```

#### Logout
```kotlin
authViewModel.signOut()
```

---

### 2. Data Operations

#### Create Data (Always Saves Locally First)
```kotlin
viewModelScope.launch {
    val result = syncViewModel.createData(
        dataType = "medical_scan",
        dataContent = """{"scan_id": "123", "result": "normal"}""",
        metadata = """{"device": "scanner_v2"}"""
    )
    
    if (result.isSuccess) {
        val dataId = result.getOrNull()
        // Data saved! Will sync in background
    }
}
```

#### Update Data
```kotlin
viewModelScope.launch {
    val result = syncViewModel.updateData(
        dataId = "existing-data-id",
        dataContent = """{"updated": true}""",
        metadata = """{"modified_by": "user"}"""
    )
}
```

#### Delete Data
```kotlin
viewModelScope.launch {
    val result = syncViewModel.deleteData(dataId = "data-to-delete")
}
```

#### Get Data by Type
```kotlin
viewModelScope.launch {
    val scans = syncViewModel.getDataByType("medical_scan")
    // Process scans
}
```

#### Observe All User Data (Reactive)
```kotlin
val userData by syncViewModel.userData.collectAsStateWithLifecycle()

// In Composable
LazyColumn {
    items(userData) { data ->
        DataItemCard(data)
    }
}
```

---

### 3. Sync Operations

#### Manual Sync
```kotlin
// Trigger immediate sync
syncViewModel.syncNow()

// Observe sync state
val syncState by syncViewModel.syncState.collectAsStateWithLifecycle()
when (syncState) {
    is SyncState.Syncing -> Text("Syncing...")
    is SyncState.Synced -> Text("All synced ✓")
    is SyncState.Offline -> Text("Offline mode")
    is SyncState.Error -> Text("Sync failed: ${syncState.message}")
}
```

#### Setup Periodic Sync
```kotlin
// In Application onCreate() or MainActivity
syncViewModel.setupPeriodicSync(intervalHours = 6)
```

#### Get Sync Statistics
```kotlin
viewModelScope.launch {
    val stats = syncViewModel.getSyncStats()
    println("Total: ${stats.totalEntries}")
    println("Synced: ${stats.syncedEntries}")
    println("Pending: ${stats.pendingEntries}")
    println("Failed: ${stats.failedEntries}")
}
```

#### Fetch from Firestore
```kotlin
// Pull latest data from cloud
syncViewModel.fetchFromFirestore()
```

---

### 4. Network Monitoring

#### Check Network Status
```kotlin
val isOnline by authViewModel.isOnline.collectAsStateWithLifecycle()

if (isOnline) {
    Text("Online", color = Color.Green)
} else {
    Text("Offline", color = Color.Gray)
}
```

#### Get Network Type
```kotlin
@Inject lateinit var networkObserver: NetworkConnectivityObserver

val networkType = networkObserver.getCurrentNetworkType()
when (networkType) {
    NetworkType.WIFI -> "Wi-Fi"
    NetworkType.CELLULAR -> "Mobile Data"
    NetworkType.NONE -> "No Connection"
}
```

#### Check if Metered
```kotlin
if (networkObserver.isMeteredConnection()) {
    // Don't sync large files on cellular
}
```

---

### 5. UI State Management

#### Observe Unsynced Count
```kotlin
val unsyncedCount by syncViewModel.unsyncedCount.collectAsStateWithLifecycle()

Badge {
    Text("$unsyncedCount unsynced")
}
```

#### Display Sync Status
```kotlin
@Composable
fun SyncStatusIndicator(syncState: SyncState) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        when (syncState) {
            is SyncState.Offline -> {
                Icon(Icons.Default.CloudOff, tint = Color.Gray)
                Text("Offline")
            }
            is SyncState.Syncing -> {
                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                Text("Syncing...")
            }
            is SyncState.Synced -> {
                Icon(Icons.Default.CloudDone, tint = Color.Green)
                Text("Synced")
            }
            is SyncState.Error -> {
                Icon(Icons.Default.Error, tint = Color.Red)
                Text("Error")
            }
        }
    }
}
```

---

### 6. Repository Direct Access (Advanced)

#### Get User Info
```kotlin
@Inject lateinit var authRepository: AuthRepository

viewModelScope.launch {
    val user = authRepository.getUserById(userId)
    println("Email: ${user?.email}")
    println("Sync Status: ${user?.syncStatus}")
}
```

#### Manual Data Sync
```kotlin
@Inject lateinit var dataRepository: DataRepository

viewModelScope.launch {
    val syncedCount = dataRepository.syncPendingData(userId)
    println("Synced $syncedCount items")
}
```

---

### 7. WorkManager Operations

#### Check Sync Worker Status
```kotlin
@Inject lateinit var workManager: WorkManager

val workInfo = workManager.getWorkInfosForUniqueWork(SyncWorker.WORK_NAME).get()
workInfo.forEach { info ->
    println("State: ${info.state}")
    println("Progress: ${info.progress}")
}
```

#### Cancel Sync
```kotlin
workManager.cancelUniqueWork(SyncWorker.WORK_NAME)
```

---

### 8. Testing Offline Behavior

#### Simulate Offline Mode
```kotlin
// 1. Turn off Wi-Fi/Mobile Data
// 2. Perform operations (signup, create data, etc.)
// 3. Verify data is saved locally:

viewModelScope.launch {
    val stats = syncViewModel.getSyncStats()
    println("Pending items: ${stats.pendingEntries}") // Should be > 0
}

// 4. Turn on internet
// 5. Verify automatic sync:

syncViewModel.syncState.collect { state ->
    if (state is SyncState.Syncing) {
        println("Auto-sync started!")
    }
}
```

---

### 9. Error Handling

#### Handle Auth Errors
```kotlin
authViewModel.authState.collect { state ->
    if (state is AuthState.Error) {
        when {
            state.message.contains("network", ignoreCase = true) -> {
                showSnackbar("No internet connection")
            }
            state.message.contains("invalid", ignoreCase = true) -> {
                showSnackbar("Invalid credentials")
            }
            else -> {
                showSnackbar("Error: ${state.message}")
            }
        }
    }
}
```

#### Handle Sync Errors
```kotlin
syncViewModel.syncState.collect { state ->
    if (state is SyncState.Error) {
        // Show error to user
        showDialog("Sync Error", state.message)
        
        // Optionally retry
        syncViewModel.syncNow()
    }
}
```

---

### 10. Database Queries (Advanced)

#### Direct DAO Access
```kotlin
@Inject lateinit var localDataDao: LocalDataDao

viewModelScope.launch {
    // Get all data
    val allData = localDataDao.getAllData()
    
    // Get unsynced data
    val unsyncedData = localDataDao.getUnsyncedData()
    
    // Get data by type
    val scans = localDataDao.getDataByType(userId, "medical_scan")
    
    // Observe with Flow
    localDataDao.observeDataByUserId(userId).collect { data ->
        // Auto-updates when data changes
    }
}
```

---

## 🎯 Best Practices Summary

1. **Always Use ViewModels**: Never call Repository/DAO directly from UI
2. **Observe with StateFlow**: Use `collectAsStateWithLifecycle()` in Compose
3. **Save Locally First**: Let WorkManager handle sync in background
4. **Handle All States**: Use `when` with sealed classes
5. **Show Network Status**: Let users know when they're offline
6. **Display Sync Progress**: Show unsynced count badge
7. **Enable Manual Sync**: Provide "Sync Now" button
8. **Test Offline**: Always test with Wi-Fi off

---

## 📊 Example Dashboard Screen

```kotlin
@Composable
fun DashboardScreen(
    authViewModel: AuthViewModel,
    syncViewModel: SyncViewModel
) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val syncState by syncViewModel.syncState.collectAsStateWithLifecycle()
    val isOnline by syncViewModel.isOnline.collectAsStateWithLifecycle()
    val unsyncedCount by syncViewModel.unsyncedCount.collectAsStateWithLifecycle()
    val userData by syncViewModel.userData.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") },
                actions = {
                    // Network indicator
                    if (!isOnline) {
                        Icon(Icons.Default.CloudOff, "Offline", tint = Color.Gray)
                    }
                    
                    // Unsynced badge
                    if (unsyncedCount > 0) {
                        Badge { Text("$unsyncedCount") }
                    }
                    
                    // Sync button
                    IconButton(
                        onClick = { syncViewModel.syncNow() },
                        enabled = isOnline
                    ) {
                        Icon(Icons.Default.Sync, "Sync")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            // Sync status banner
            when (syncState) {
                is SyncState.Syncing -> LinearProgressIndicator(Modifier.fillMaxWidth())
                is SyncState.Error -> ErrorBanner((syncState as SyncState.Error).message)
            }
            
            // User data list
            LazyColumn {
                items(userData) { data ->
                    DataCard(data) {
                        // Handle item click
                    }
                }
            }
        }
    }
}
```

---

**Happy Coding! 🎉**
