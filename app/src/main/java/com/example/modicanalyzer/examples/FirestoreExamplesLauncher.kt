package com.example.modicanalyzer.examples

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Firestore Examples Launcher
 * 
 * Quick way to test all Firestore examples
 */
@AndroidEntryPoint
class FirestoreExamplesLauncher : ComponentActivity() {
    
    @Inject
    lateinit var firebaseAuth: FirebaseAuth
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            com.example.modicanalyzer.ui.theme.ModicAnalyzerTheme(darkTheme = false, dynamicColor = false) {
                ExamplesLauncherScreen()
            }
        }
    }
    
    @Composable
    fun ExamplesLauncherScreen() {
        val currentUser = firebaseAuth.currentUser
        
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))
                
                // Header
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Examples",
                    modifier = Modifier.size(64.dp),
                    tint = com.example.modicanalyzer.ui.theme.ModicarePrimary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Firestore Examples",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = com.example.modicanalyzer.ui.theme.ModicarePrimary
                )
                
                Text(
                    text = "Test Firestore Integration",
                    fontSize = 16.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // User Status Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (currentUser != null) 
                            Color(0xFF4CAF50).copy(alpha = 0.1f)
                        else 
                            Color(0xFFFF9800).copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (currentUser != null) Icons.Default.Check else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (currentUser != null) Color(0xFF4CAF50) else Color(0xFFFF9800),
                            modifier = Modifier.size(32.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column {
                            Text(
                                text = if (currentUser != null) "✅ Logged In" else "⚠️ Not Logged In",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = currentUser?.email ?: "You need to be logged in for some examples",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Example 1: Basic Operations
                ExampleCard(
                    title = "Firestore Operations Example",
                    description = "Test basic Firestore operations:\n• Create/update user profile\n• Add data entries\n• Fetch data\n• Update & delete\n\nCheck Logcat for detailed output.",
                    icon = Icons.Default.Settings,
                    buttonText = "Run Operations Example",
                    requiresAuth = true,
                    isUserLoggedIn = currentUser != null,
                    onClick = {
                        startActivity(Intent(this@FirestoreExamplesLauncher, FirestoreExampleActivity::class.java))
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Example 2: Signup Integration
                ExampleCard(
                    title = "Signup with Firestore",
                    description = "Test complete signup flow:\n• Create Firebase Auth account\n• Store user profile in Firestore\n• View in Firebase Console\n\nThis creates a real account!",
                    icon = Icons.Default.Person,
                    buttonText = "Test Signup Flow",
                    requiresAuth = false,
                    isUserLoggedIn = true, // Always enabled
                    onClick = {
                        startActivity(Intent(this@FirestoreExamplesLauncher, SignupWithFirestoreExample::class.java))
                    }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Instructions
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF2196F3).copy(alpha = 0.1f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF2196F3)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "How to Test",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2196F3)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "1. Run an example\n" +
                                   "2. Open Logcat in Android Studio\n" +
                                   "3. Filter: FirestoreExample or FirestoreHelper\n" +
                                   "4. Check Firebase Console:\n" +
                                   "   → console.firebase.google.com\n" +
                                   "   → Firestore Database → Data\n" +
                                   "5. Look for 'users' collection",
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Logout Button (if logged in)
                if (currentUser != null) {
                    OutlinedButton(
                        onClick = {
                            firebaseAuth.signOut()
                            recreate() // Refresh the screen
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ExitToApp, "Logout")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Logout (${currentUser.email})")
                    }
                }
            }
        }
    }
    
    @Composable
    fun ExampleCard(
        title: String,
        description: String,
        icon: ImageVector,
        buttonText: String,
        requiresAuth: Boolean,
        isUserLoggedIn: Boolean,
        onClick: () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = com.example.modicanalyzer.ui.theme.ModicarePrimary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    lineHeight = 20.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                val canRun = !requiresAuth || isUserLoggedIn
                
                Button(
                    onClick = onClick,
                    enabled = canRun,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = com.example.modicanalyzer.ui.theme.ModicarePrimary
                    )
                ) {
                    Text(buttonText)
                }
                
                if (requiresAuth && !isUserLoggedIn) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "⚠️ Requires authentication",
                        fontSize = 12.sp,
                        color = Color(0xFFFF9800),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
