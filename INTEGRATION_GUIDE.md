# 🔄 Integration Guide - Migrating to Offline-First Architecture

## Overview

This guide shows you how to integrate the new offline-first MVVM architecture into your existing `LoginActivity`, `SignupActivity`, and `MainActivity`.

---

## 📋 Step-by-Step Migration

### Step 1: Add Hilt Annotation to Activities

**BEFORE:**
```kotlin
class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val authManager = AuthManager(this)
        // ... rest of code
    }
}
```

**AFTER:**
```kotlin
@AndroidEntryPoint  // ← Add this annotation
class LoginActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()  // ← Inject ViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // No more manual AuthManager!
    }
}
```

---

### Step 2: Update LoginActivity

**File:** `app/src/main/java/com/example/modicanalyzer/LoginActivity.kt`

```kotlin
package com.example.modicanalyzer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.example.modicanalyzer.ui.screens.ModernLoginScreen
import com.example.modicanalyzer.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint  // Add this
class LoginActivity : ComponentActivity() {
    
    // Inject ViewModel
    private val authViewModel: AuthViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            com.example.modicanalyzer.ui.theme.ModicAnalyzerTheme(
                darkTheme = false,
                dynamicColor = false
            ) {
                ModernLoginScreen(
                    authViewModel = authViewModel,
                    onLoginSuccess = {
                        startActivity(Intent(this, SimpleMainActivity::class.java))
                        finish()
                    },
                    onNavigateToSignup = {
                        startActivity(Intent(this, SignupActivity::class.java))
                    }
                )
            }
        }
    }
}
```

---

### Step 3: Create ModernSignupScreen

**File:** `app/src/main/java/com/example/modicanalyzer/ui/screens/ModernSignupScreen.kt`

```kotlin
package com.example.modicanalyzer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.modicanalyzer.data.model.AuthState
import com.example.modicanalyzer.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernSignupScreen(
    authViewModel: AuthViewModel,
    onSignupSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val isOnline by authViewModel.isOnline.collectAsStateWithLifecycle()
    
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.Success -> {
                onSignupSuccess()
                authViewModel.resetAuthState()
            }
            is AuthState.Error -> {
                showError = state.message
            }
            else -> {}
        }
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Network status banner
            NetworkStatusBanner(isOnline = isOnline)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Create Account",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = com.example.modicanalyzer.ui.theme.ModicarePrimary
            )
            
            // Form card with fields
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    // Display Name field
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Full Name") },
                        leadingIcon = { Icon(Icons.Default.Person, null) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = authState !is AuthState.Loading
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Email field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Default.Email, null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = authState !is AuthState.Loading
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Password field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, null) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = authState !is AuthState.Loading
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Confirm password field
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, null) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = authState !is AuthState.Loading
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Sign up button
                    Button(
                        onClick = {
                            when {
                                email.isBlank() || password.isBlank() -> {
                                    showError = "Please fill in all fields"
                                }
                                password != confirmPassword -> {
                                    showError = "Passwords don't match"
                                }
                                password.length < 6 -> {
                                    showError = "Password must be at least 6 characters"
                                }
                                else -> {
                                    authViewModel.signUp(
                                        email,
                                        password,
                                        displayName.ifBlank { null }
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = authState !is AuthState.Loading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = com.example.modicanalyzer.ui.theme.ModicarePrimary
                        )
                    ) {
                        if (authState is AuthState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White
                            )
                        } else {
                            Text(
                                if (isOnline) "Create Account" else "Create Account (Offline)",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Login navigation
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Already have an account? ", color = Color.Gray)
                TextButton(onClick = onNavigateToLogin) {
                    Text(
                        "Sign In",
                        color = com.example.modicanalyzer.ui.theme.ModicarePrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
```

---

### Step 4: Update SignupActivity

```kotlin
package com.example.modicanalyzer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.example.modicanalyzer.ui.screens.ModernSignupScreen
import com.example.modicanalyzer.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint  // Add this
class SignupActivity : ComponentActivity() {
    
    private val authViewModel: AuthViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            com.example.modicanalyzer.ui.theme.ModicAnalyzerTheme(
                darkTheme = false,
                dynamicColor = false
            ) {
                ModernSignupScreen(
                    authViewModel = authViewModel,
                    onSignupSuccess = {
                        startActivity(Intent(this, SimpleMainActivity::class.java))
                        finish()
                    },
                    onNavigateToLogin = {
                        finish() // Go back to login
                    }
                )
            }
        }
    }
}
```

---

### Step 5: Add Sync to SimpleMainActivity

```kotlin
@AndroidEntryPoint  // Add this
class SimpleMainActivity : ComponentActivity() {
    
    private val authViewModel: AuthViewModel by viewModels()
    private val syncViewModel: SyncViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set current user for sync
        lifecycleScope.launch {
            authViewModel.authState.collect { state ->
                if (state is AuthState.Success) {
                    syncViewModel.setCurrentUser(state.userId)
                }
            }
        }
        
        setContent {
            val syncState by syncViewModel.syncState.collectAsStateWithLifecycle()
            val unsyncedCount by syncViewModel.unsyncedCount.collectAsStateWithLifecycle()
            
            // Your existing UI with added sync indicators
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("SpinoCare") },
                        actions = {
                            // Sync status
                            when (syncState) {
                                is SyncState.Syncing -> CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp)
                                )
                                is SyncState.Synced -> Icon(
                                    Icons.Default.CloudDone,
                                    "Synced",
                                    tint = Color.Green
                                )
                                else -> {}
                            }
                            
                            // Unsynced count
                            if (unsyncedCount > 0) {
                                Badge { Text("$unsyncedCount") }
                            }
                        }
                    )
                }
            ) { padding ->
                // Your existing content
            }
        }
    }
}
```

---

## 🔑 Key Points to Remember

1. **Add `@AndroidEntryPoint`** to all activities using Hilt
2. **Inject ViewModels** with `by viewModels()`
3. **Collect StateFlows** with `collectAsStateWithLifecycle()`
4. **Handle all states** in `when` expressions
5. **Remove manual** AuthManager/Firebase calls
6. **Let ViewModels** handle all business logic

---

## ✅ Migration Checklist

- [ ] Add `@AndroidEntryPoint` to LoginActivity
- [ ] Add `@AndroidEntryPoint` to SignupActivity
- [ ] Add `@AndroidEntryPoint` to SimpleMainActivity
- [ ] Inject `AuthViewModel` in login/signup
- [ ] Inject `SyncViewModel` in main activity
- [ ] Use `ModernLoginScreen` composable
- [ ] Create `ModernSignupScreen` composable
- [ ] Add sync indicators to main screen
- [ ] Remove old `AuthManager` calls
- [ ] Test offline signup/login
- [ ] Test automatic sync
- [ ] Verify no compilation errors

---

## 🧪 Testing After Migration

1. **Build the project**: `./gradlew build`
2. **Turn off Wi-Fi**: Test offline signup
3. **Create test user**: Verify stored in Room
4. **Turn on Wi-Fi**: Watch automatic sync
5. **Check sync status**: Should show "Synced ✓"
6. **Create data offline**: Should save locally
7. **Go online**: Should sync automatically

---

## 📚 Reference

- See `QUICK_REFERENCE.md` for code examples
- See `OFFLINE_FIRST_ARCHITECTURE.md` for architecture details
- See existing `ModernLoginScreen.kt` for implementation

---

**🎉 Migration complete! Your app now has offline-first capabilities! 🎉**
