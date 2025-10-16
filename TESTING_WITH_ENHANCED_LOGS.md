# 🔍 Enhanced Logging - Testing Guide

## ✅ Changes Applied

I've added **comprehensive logging** to `AuthViewModel.kt` to help debug the offline signup and login issues.

---

## 📱 How to Test with Logcat

### Step 1: Set Up Logcat Filter

In **Android Studio Logcat**, use this filter to see only relevant logs:

```
AuthViewModel|AuthRepository|SyncWorker|ModicareApp
```

Or in terminal:
```powershell
adb logcat -s AuthViewModel:D AuthRepository:D SyncWorker:D ModicareApp:D
```

---

## 🧪 Test Offline Signup

### Actions:
1. **Turn OFF WiFi** on your device/emulator
2. Open the app
3. Click "Sign Up"
4. Fill in:
   - Email: `test_offline@example.com`
   - Password: `TestOffline123!`
   - Confirm Password: `TestOffline123!`
   - Name: `Test User`
5. Click "Sign Up" button

### Expected Logcat Output:

```
D/AuthViewModel: Initializing AuthViewModel
D/AuthViewModel: checkCurrentUser: Checking authentication status...
D/AuthViewModel: checkCurrentUser: Firebase user = null
D/AuthViewModel: checkCurrentUser: Last logged in userId from SharedPrefs = null
D/AuthViewModel: checkCurrentUser: No saved userId, setting Unauthenticated

D/AuthViewModel: signUp called: email=test_offline@example.com, isOnline=false
D/AuthViewModel: Validation results: [email=true, password=true, confirmPassword=true, displayName=true]
D/AuthViewModel: Validation passed, calling authRepository.signUp()

D/AuthRepository: Offline signup successful: userId=<uuid>, email=test_offline@example.com, syncStatus=PENDING

D/AuthViewModel: signUp state received: AuthState.Success(userId=<uuid>, email=test_offline@example.com, isFirebaseAuth=false)
D/AuthViewModel: Signup successful! userId=<uuid>, isFirebaseAuth=false
D/AuthViewModel: saveLastLoggedInUserId: Saving userId=<uuid> to SharedPreferences
D/AuthViewModel: Device is offline, sync will happen when connected
```

**What to Check**:
- ✅ Does it say `"Signup successful!"`?
- ✅ Does it say `"Saving userId=... to SharedPreferences"`?
- ✅ Does the app navigate to the main screen?

---

## 🔄 Test App Restart (Still Offline)

### Actions:
1. **Keep WiFi OFF**
2. **Close the app** (swipe away from recent apps)
3. **Reopen the app**

### Expected Logcat Output:

```
D/AuthViewModel: Initializing AuthViewModel
D/AuthViewModel: checkCurrentUser: Checking authentication status...
D/AuthViewModel: checkCurrentUser: Firebase user = null
D/AuthViewModel: checkCurrentUser: Last logged in userId from SharedPrefs = <uuid>
D/AuthViewModel: checkCurrentUser: Local user from Room = test_offline@example.com
D/AuthViewModel: checkCurrentUser: Local user found, logging in as test_offline@example.com
```

**What to Check**:
- ✅ Does it find the userId in SharedPreferences?
- ✅ Does it find the user in Room database?
- ✅ Does it say `"Local user found, logging in as..."`?
- ✅ Does the app go directly to the main screen (without showing login)?

---

## 🌐 Test Auto-Sync (Turn ON Internet)

### Actions:
1. **Turn ON WiFi**
2. **Restart the app** (or wait 15 minutes for periodic sync)

### Expected Logcat Output:

```
D/ModicareApp: Device online - triggering immediate sync

D/SyncWorker: Starting sync: type=full, userId=null
D/AuthRepository: Starting sync for 1 unsynced users
D/AuthRepository: Syncing user: test_offline@example.com, isFirebaseAuth=false, hasEncryptedPassword=true
D/AuthRepository: Firebase account created successfully: <firebase_uid>
D/AuthRepository: Synced offline user: test_offline@example.com -> <firebase_uid>
D/SyncWorker: Synced 1 users
D/SyncWorker: Sync completed: users=1, data=0
```

**What to Check**:
- ✅ Does it say `"Starting sync for 1 unsynced users"`?
- ✅ Does it say `"Firebase account created successfully"`?
- ✅ Does it say `"Synced offline user: ... -> <firebase_uid>"`?
- ✅ Check Firebase Console → Authentication → Users → Should see the email!

---

## 🧪 Test Offline Login (After Signup)

### Actions:
1. **Keep WiFi OFF**
2. If still logged in, **sign out first**
3. Click "Login"
4. Enter:
   - Email: `test_offline@example.com`
   - Password: `TestOffline123!`
5. Click "Login" button

### Expected Logcat Output:

```
D/AuthViewModel: login called: email=test_offline@example.com, isOnline=false
D/AuthViewModel: Login validation results: [email=true, password=true]
D/AuthViewModel: Login validation passed, calling authRepository.login()
D/AuthViewModel: login state received: AuthState.Success(userId=<uuid>, email=test_offline@example.com, isFirebaseAuth=false)
D/AuthViewModel: Login successful! userId=<uuid>, isFirebaseAuth=false
D/AuthViewModel: saveLastLoggedInUserId: Saving userId=<uuid> to SharedPreferences
D/AuthViewModel: Device is offline, no sync needed
```

**What to Check**:
- ✅ Does validation pass?
- ✅ Does login succeed?
- ✅ Does it save userId to SharedPreferences?
- ✅ Does the app navigate to main screen?

---

## ❌ What If Logs Don't Appear?

### Issue: No "signUp called" log appears when clicking Sign Up

**Possible causes**:
1. Button not connected to `AuthViewModel.signUp()`
2. SignupActivity not using the updated AuthViewModel
3. Validation failing silently

**Check**:
```
D/AuthViewModel: signUp called: email=...
```
If this doesn't appear, the button click isn't reaching the ViewModel.

---

### Issue: "Validation results" shows false for some fields

**Example**:
```
D/AuthViewModel: Validation results: [email=true, password=false, ...]
```

**Meaning**: Password doesn't meet requirements (8+ chars, uppercase, lowercase, digit, special character)

**Fix**: Use a stronger password like `Test123!@#`

---

### Issue: "Local user from Room = null" after signup

**Meaning**: User wasn't saved to Room database

**Check**:
```
D/AuthRepository: Offline signup successful: userId=...
```
If this appears but user is still null, there's a database issue.

**Debug**:
```powershell
adb shell "cd /data/data/com.example.modicanalyzer/databases && sqlite3 modicare_offline_db 'SELECT * FROM users;'"
```

---

### Issue: No sync logs when turning on WiFi

**Check**:
```
D/ModicareApp: Device online - triggering immediate sync
```

If this doesn't appear:
1. Restart the app after turning on WiFi (sync triggers on app startup)
2. Wait 15 minutes for periodic sync
3. Check if WorkManager is working:
   ```powershell
   adb shell dumpsys jobscheduler | findstr modicanalyzer
   ```

---

## 🎯 Complete Test Sequence

Run this complete test to verify everything works:

```
1. Turn OFF WiFi
2. Open app
3. Sign up: offline_test@example.com / Offline123!
   → Check: "Signup successful!" in logs
   → Check: Navigate to main screen
   
4. Close and reopen app (WiFi still OFF)
   → Check: "Local user found, logging in" in logs
   → Check: Still logged in
   
5. Sign out
6. Login: offline_test@example.com / Offline123!
   → Check: "Login successful!" in logs
   → Check: Navigate to main screen
   
7. Turn ON WiFi
8. Restart app
   → Check: "Starting sync for 1 unsynced users" in logs
   → Check: "Firebase account created successfully" in logs
   
9. Check Firebase Console
   → Check: offline_test@example.com appears in Users
```

---

## 📊 Logcat Filter Command

### Android Studio:
```
AuthViewModel|AuthRepository|SyncWorker|ModicareApp
```

### PowerShell:
```powershell
adb logcat -s AuthViewModel:D AuthRepository:D SyncWorker:D ModicareApp:D
```

### Save to file:
```powershell
adb logcat -s AuthViewModel:D AuthRepository:D SyncWorker:D ModicareApp:D > offline_signup_logs.txt
```

---

## 🔍 What to Share

If you still have issues, share these logs:

1. **Full logcat output** from signup attempt
2. **Database query result**:
   ```powershell
   adb shell "cd /data/data/com.example.modicanalyzer/databases && sqlite3 modicare_offline_db 'SELECT userId, email, syncStatus, isFirebaseAuth FROM users;'"
   ```
3. **SharedPreferences content**:
   ```powershell
   adb shell "cat /data/data/com.example.modicanalyzer/shared_prefs/auth_prefs.xml"
   ```

---

## ✅ Success Indicators

You'll know it's working when you see:

✅ `"Signup successful! userId=..., isFirebaseAuth=false"`  
✅ `"Saving userId=... to SharedPreferences"`  
✅ `"Local user found, logging in as ..."`  
✅ `"Starting sync for X unsynced users"`  
✅ `"Firebase account created successfully: ..."`  
✅ User appears in Firebase Console  

---

## 🚀 Build and Test Now!

1. **Build the app** with updated logging
2. **Run the tests** above
3. **Watch the logcat** with the filter
4. **Share the logs** if you see any issues

The enhanced logging will show exactly where the process stops! 🔍
