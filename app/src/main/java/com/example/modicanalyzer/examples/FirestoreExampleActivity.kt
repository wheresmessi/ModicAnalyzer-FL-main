package com.example.modicanalyzer.examples

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.modicanalyzer.data.remote.FirestoreHelper
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * EXAMPLE: How to use FirestoreHelper
 * 
 * This is a demonstration of Firestore operations.
 * Copy the code patterns to your actual activities/ViewModels.
 */
@OptIn(ExperimentalMaterial3Api::class)
@AndroidEntryPoint
class FirestoreExampleActivity : ComponentActivity() {
    
    @Inject
    lateinit var firestoreHelper: FirestoreHelper
    
    @Inject
    lateinit var firebaseAuth: FirebaseAuth
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            MaterialTheme {
                FirestoreExampleScreen()
            }
        }
    }
    
    @Composable
    fun FirestoreExampleScreen() {
        var logs by remember { mutableStateOf(listOf<LogEntry>()) }
        var isRunning by remember { mutableStateOf(false) }
        
        fun addLog(message: String, isError: Boolean = false) {
            logs = logs + LogEntry(message, isError)
        }
        
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Firestore Operations Test") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                // User info
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Current User",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Email: ${firebaseAuth.currentUser?.email ?: "Not logged in"}")
                        Text("UID: ${firebaseAuth.currentUser?.uid ?: "N/A"}")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Run button
                Button(
                    onClick = {
                        if (firebaseAuth.currentUser == null) {
                            Toast.makeText(
                                this@FirestoreExampleActivity,
                                "Please login first! Use the Signup example to create an account.",
                                Toast.LENGTH_LONG
                            ).show()
                            return@Button
                        }
                        
                        logs = emptyList()
                        isRunning = true
                        
                        lifecycleScope.launch {
                            addLog("🚀 Starting Firestore operations test...")
                            exampleUserProfile { log, error -> addLog(log, error) }
                            exampleDataEntries { log, error -> addLog(log, error) }
                            addLog("✅ All operations completed!")
                            isRunning = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isRunning && firebaseAuth.currentUser != null
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isRunning) "Running..." else "Run Firestore Operations")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Logs
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1E1E1E)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp)
                    ) {
                        Text(
                            "📋 Operation Logs",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        if (logs.isEmpty()) {
                            Text(
                                "Click 'Run Firestore Operations' to start testing...",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        } else {
                            logs.forEach { log ->
                                Row(
                                    modifier = Modifier.padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        if (log.isError) Icons.Default.Error else Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (log.isError) Color.Red else Color.Green,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        log.message,
                                        color = if (log.isError) Color.Red else Color.White,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    "💡 Tip: Check Logcat (filter: FirestoreExample) and Firebase Console for detailed results",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    
    data class LogEntry(val message: String, val isError: Boolean = false)
    
    // ============================================
    // EXAMPLE 1: User Profile Operations
    // ============================================
    
    private suspend fun exampleUserProfile(log: (String, Boolean) -> Unit) {
        val userId = firebaseAuth.currentUser?.uid ?: return
        
        // 1. Create/Update User Profile
        log("📝 Creating user profile...", false)
        Log.d("FirestoreExample", "📝 Creating user profile...")
        
        val createResult = firestoreHelper.createOrUpdateUserProfile(
            userId = userId,
            name = firebaseAuth.currentUser?.displayName ?: "Test User",
            email = firebaseAuth.currentUser?.email ?: "test@example.com",
            role = "patient",
            profileImageUrl = null // Set after uploading to Storage
        )
        
        if (createResult.isSuccess) {
            log("✅ User profile created!", false)
            Log.d("FirestoreExample", "✅ User profile created!")
        } else {
            log("❌ Failed to create profile: ${createResult.exceptionOrNull()?.message}", true)
            Log.e("FirestoreExample", "❌ Failed to create profile: ${createResult.exceptionOrNull()}")
        }
        
        // 2. Get User Profile
        log("🔍 Fetching user profile...", false)
        Log.d("FirestoreExample", "🔍 Fetching user profile...")
        
        val profileResult = firestoreHelper.getUserProfile(userId)
        
        profileResult.getOrNull()?.let { profile ->
            log("✅ User Profile Retrieved:", false)
            log("  Name: ${profile["name"]}", false)
            log("  Email: ${profile["email"]}", false)
            log("  Role: ${profile["role"]}", false)
            
            Log.d("FirestoreExample", "✅ User Profile Retrieved:")
            Log.d("FirestoreExample", "  Name: ${profile["name"]}")
            Log.d("FirestoreExample", "  Email: ${profile["email"]}")
            Log.d("FirestoreExample", "  Role: ${profile["role"]}")
        } ?: run {
            log("❌ Failed to get profile: ${profileResult.exceptionOrNull()?.message}", true)
        }
        
        // 3. Update Profile Image (after uploading to Firebase Storage)
        log("📝 Updating profile image...", false)
        val exampleImageUrl = "https://firebasestorage.googleapis.com/.../profile.jpg"
        val updateResult = firestoreHelper.updateProfileImage(userId, exampleImageUrl)
        
        if (updateResult.isSuccess) {
            log("✅ Profile image updated!", false)
            Log.d("FirestoreExample", "✅ Profile image updated!")
        } else {
            log("❌ Failed to update image: ${updateResult.exceptionOrNull()?.message}", true)
        }
    }
    
    // ============================================
    // EXAMPLE 2: Data Entries (Images/Scans)
    // ============================================
    
    private suspend fun exampleDataEntries(log: (String, Boolean) -> Unit) {
        val userId = firebaseAuth.currentUser?.uid ?: return
        
        // 1. Add a Data Entry (after uploading image to Storage)
        log("📝 Adding data entry...", false)
        Log.d("FirestoreExample", "📝 Adding data entry...")
        
        val imageUrl = "https://firebasestorage.googleapis.com/.../scan123.jpg"
        
        val entryResult = firestoreHelper.addDataEntry(
            userId = userId,
            imageUrl = imageUrl,
            caption = "X-ray scan - spinal analysis",
            metadata = mapOf(
                "type" to "xray",
                "resolution" to "1920x1080",
                "fileSize" to "2.5MB",
                "analysis" to mapOf(
                    "confidence" to 0.95,
                    "prediction" to "normal"
                )
            )
        )
        
        val entryId = entryResult.getOrNull()
        if (entryId != null) {
            log("✅ Data entry added with ID: $entryId", false)
            Log.d("FirestoreExample", "✅ Data entry added with ID: $entryId")
        } else {
            log("❌ Failed to add entry: ${entryResult.exceptionOrNull()?.message}", true)
        }
        
        // 2. Get All User Data Entries
        log("🔍 Fetching all data entries...", false)
        Log.d("FirestoreExample", "🔍 Fetching all data entries...")
        
        val entriesResult = firestoreHelper.getUserDataEntries(userId)
        
        entriesResult.getOrNull()?.let { entries ->
            log("✅ Retrieved ${entries.size} entries", false)
            Log.d("FirestoreExample", "✅ Retrieved ${entries.size} entries:")
            
            entries.forEach { entry ->
                log("  Entry ID: ${entry.id}", false)
                log("  Caption: ${entry.caption}", false)
                
                Log.d("FirestoreExample", "  Entry ID: ${entry.id}")
                Log.d("FirestoreExample", "  Image URL: ${entry.imageUrl}")
                Log.d("FirestoreExample", "  Caption: ${entry.caption}")
                Log.d("FirestoreExample", "  Metadata: ${entry.metadata}")
            }
        } ?: run {
            log("❌ Failed to get entries: ${entriesResult.exceptionOrNull()?.message}", true)
        }
        
        // 3. Update a Data Entry
        if (entryId != null) {
            log("📝 Updating data entry...", false)
            Log.d("FirestoreExample", "📝 Updating data entry...")
            
            val updateResult = firestoreHelper.updateDataEntry(
                userId = userId,
                entryId = entryId,
                updates = mapOf(
                    "caption" to "Updated caption - test successful!",
                    "metadata" to mapOf(
                        "reviewed" to true,
                        "reviewer" to "Automated Test"
                    )
                )
            )
            
            if (updateResult.isSuccess) {
                log("✅ Data entry updated!", false)
                Log.d("FirestoreExample", "✅ Data entry updated!")
            } else {
                log("❌ Failed to update entry: ${updateResult.exceptionOrNull()?.message}", true)
            }
        }
        
        // 4. Delete a Data Entry
        if (entryId != null) {
            log("🗑️ Deleting data entry...", false)
            Log.d("FirestoreExample", "🗑️ Deleting data entry...")
            
            val deleteResult = firestoreHelper.deleteDataEntry(userId, entryId)
            
            if (deleteResult.isSuccess) {
                log("✅ Data entry deleted!", false)
                Log.d("FirestoreExample", "✅ Data entry deleted!")
            } else {
                log("❌ Failed to delete entry: ${deleteResult.exceptionOrNull()?.message}", true)
            }
        }
    }
}
