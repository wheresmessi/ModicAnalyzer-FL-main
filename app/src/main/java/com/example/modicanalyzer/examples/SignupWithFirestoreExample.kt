package com.example.modicanalyzer.examples

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.modicanalyzer.SimpleMainActivity
import com.example.modicanalyzer.data.remote.FirestoreHelper
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Example: SignupActivity with Firestore Integration
 * 
 * This shows how to:
 * 1. Create Firebase Auth account
 * 2. Store user profile in Firestore
 * 3. Navigate to main activity
 */
@AndroidEntryPoint
class SignupWithFirestoreExample : ComponentActivity() {
    
    @Inject
    lateinit var firebaseAuth: FirebaseAuth
    
    @Inject
    lateinit var firestoreHelper: FirestoreHelper
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            com.example.modicanalyzer.ui.theme.ModicAnalyzerTheme(darkTheme = false, dynamicColor = false) {
                SignupScreenWithFirestore(
                    onSignupSuccess = {
                        // Navigate to main activity
                        startActivity(Intent(this@SignupWithFirestoreExample, SimpleMainActivity::class.java))
                        finish()
                    },
                    onNavigateToLogin = {
                        finish()
                    }
                )
            }
        }
    }
    
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SignupScreenWithFirestore(
        onSignupSuccess: () -> Unit,
        onNavigateToLogin: () -> Unit
    ) {
        val context = LocalContext.current
        var fullName by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        var isPasswordVisible by remember { mutableStateOf(false) }
        var acceptTerms by remember { mutableStateOf(false) }
        var isLoading by remember { mutableStateOf(false) }
        var selectedRole by remember { mutableStateOf("Patient") }
        val roles = listOf("Patient", "Doctor", "Radiologist", "Researcher")
        
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
                Text(
                    text = "Create Account with Firestore",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = com.example.modicanalyzer.ui.theme.ModicarePrimary
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Form Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        
                        // Full Name
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Full Name") },
                            leadingIcon = { Icon(Icons.Default.Person, "Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Email
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email") },
                            leadingIcon = { Icon(Icons.Default.Email, "Email") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Phone
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Phone") },
                            leadingIcon = { Icon(Icons.Default.Phone, "Phone") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Role Dropdown
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = selectedRole,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Role") },
                                leadingIcon = { Icon(Icons.Default.AccountCircle, "Role") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                roles.forEach { role ->
                                    DropdownMenuItem(
                                        text = { Text(role) },
                                        onClick = {
                                            selectedRole = role
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Password
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, "Password") },
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        if (isPasswordVisible) Icons.Default.Clear else Icons.Default.Done,
                                        "Toggle"
                                    )
                                }
                            },
                            visualTransformation = if (isPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None 
                                else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Confirm Password
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { Text("Confirm Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, "Confirm") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth(),
                            isError = confirmPassword.isNotBlank() && password != confirmPassword
                        )
                        
                        if (confirmPassword.isNotBlank() && password != confirmPassword) {
                            Text(
                                "Passwords do not match",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Terms
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = acceptTerms,
                                onCheckedChange = { acceptTerms = it }
                            )
                            Text("I agree to Terms & Conditions", fontSize = 14.sp)
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Signup Button
                        Button(
                            onClick = {
                                when {
                                    fullName.isBlank() -> Toast.makeText(context, "Enter full name", Toast.LENGTH_SHORT).show()
                                    email.isBlank() -> Toast.makeText(context, "Enter email", Toast.LENGTH_SHORT).show()
                                    password.isBlank() -> Toast.makeText(context, "Enter password", Toast.LENGTH_SHORT).show()
                                    password != confirmPassword -> Toast.makeText(context, "Passwords don't match", Toast.LENGTH_SHORT).show()
                                    password.length < 6 -> Toast.makeText(context, "Password too short", Toast.LENGTH_SHORT).show()
                                    !acceptTerms -> Toast.makeText(context, "Accept terms", Toast.LENGTH_SHORT).show()
                                    else -> {
                                        isLoading = true
                                        
                                        // ✅ FIRESTORE INTEGRATION HERE
                                        lifecycleScope.launch {
                                            try {
                                                // 1. Create Firebase Auth user
                                                val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
                                                val userId = authResult.user?.uid
                                                
                                                if (userId != null) {
                                                    // 2. Create Firestore profile
                                                    val firestoreResult = firestoreHelper.createOrUpdateUserProfile(
                                                        userId = userId,
                                                        name = fullName,
                                                        email = email,
                                                        role = selectedRole.lowercase()
                                                        // profileImageUrl will be set later when user uploads photo
                                                    )
                                                    
                                                    if (firestoreResult.isSuccess) {
                                                        Toast.makeText(context, "✅ Account created with Firestore!", Toast.LENGTH_LONG).show()
                                                        onSignupSuccess()
                                                    } else {
                                                        Toast.makeText(context, "⚠️ Auth OK, Firestore failed", Toast.LENGTH_LONG).show()
                                                        // Still navigate - can retry Firestore sync later
                                                        onSignupSuccess()
                                                    }
                                                } else {
                                                    Toast.makeText(context, "❌ Signup failed", Toast.LENGTH_SHORT).show()
                                                    isLoading = false
                                                }
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "❌ Error: ${e.message}", Toast.LENGTH_LONG).show()
                                                isLoading = false
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = com.example.modicanalyzer.ui.theme.ModicarePrimary
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White
                                )
                            } else {
                                Text("Create Account", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Login Link
                Row {
                    Text("Already have an account?")
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(onClick = onNavigateToLogin) {
                        Text("Login", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
