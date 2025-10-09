package com.example.modicanalyzer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            com.example.modicanalyzer.ui.theme.ModicAnalyzerTheme(dynamicColor = false) {
                LoginScreen(
                    onLoginSuccess = {
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    var isLogin by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var hospitalName by remember { mutableStateOf("") }
    var specialization by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    
    val animatedVisibility = remember { MutableTransitionState(false) }
    
    LaunchedEffect(Unit) {
        animatedVisibility.targetState = true
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        com.example.modicanalyzer.ui.theme.ModicareBackground,
                        Color.White
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            
            // Header
            AnimatedVisibility(
                visibleState = animatedVisibility,
                enter = slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = tween(600)
                ) + fadeIn(animationSpec = tween(600))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isLogin) "Welcome Back" else "Join ModicAnalyzer",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = com.example.modicanalyzer.ui.theme.ModicarePrimaryVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isLogin) "Sign in to continue" else "Create your medical professional account",
                        fontSize = 16.sp,
                        color = com.example.modicanalyzer.ui.theme.ModicareAccent,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Login/Signup Form
            AnimatedVisibility(
                visibleState = animatedVisibility,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(700, delayMillis = 200)
                ) + fadeIn(animationSpec = tween(700, delayMillis = 200))
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Toggle between Login and Signup
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            TextButton(
                                onClick = { isLogin = true },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = if (isLogin) com.example.modicanalyzer.ui.theme.ModicarePrimary 
                                                  else com.example.modicanalyzer.ui.theme.ModicareAccent
                                )
                            ) {
                                Text(
                                    "Login",
                                    fontWeight = if (isLogin) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                            
                            Text(
                                " | ",
                                color = com.example.modicanalyzer.ui.theme.ModicareAccent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
                            )
                            
                            TextButton(
                                onClick = { isLogin = false },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = if (!isLogin) com.example.modicanalyzer.ui.theme.ModicarePrimary 
                                                  else com.example.modicanalyzer.ui.theme.ModicareAccent
                                )
                            ) {
                                Text(
                                    "Sign Up",
                                    fontWeight = if (!isLogin) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                        
                        // Sign Up Fields (only shown when not login)
                        if (!isLogin) {
                            OutlinedTextField(
                                value = fullName,
                                onValueChange = { fullName = it },
                                label = { Text("Full Name") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = com.example.modicanalyzer.ui.theme.ModicarePrimary,
                                    focusedLabelColor = com.example.modicanalyzer.ui.theme.ModicarePrimary
                                )
                            )
                            
                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = { phoneNumber = it },
                                label = { Text("Phone Number") },
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = com.example.modicanalyzer.ui.theme.ModicarePrimary,
                                    focusedLabelColor = com.example.modicanalyzer.ui.theme.ModicarePrimary
                                )
                            )
                            
                            OutlinedTextField(
                                value = hospitalName,
                                onValueChange = { hospitalName = it },
                                label = { Text("Hospital/Clinic Name") },
                                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = com.example.modicanalyzer.ui.theme.ModicarePrimary,
                                    focusedLabelColor = com.example.modicanalyzer.ui.theme.ModicarePrimary
                                )
                            )
                            
                            OutlinedTextField(
                                value = specialization,
                                onValueChange = { specialization = it },
                                label = { Text("Specialization") },
                                leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = com.example.modicanalyzer.ui.theme.ModicarePrimary,
                                    focusedLabelColor = com.example.modicanalyzer.ui.theme.ModicarePrimary
                                )
                            )
                        }
                        
                        // Email Field
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = com.example.modicanalyzer.ui.theme.ModicarePrimary,
                                focusedLabelColor = com.example.modicanalyzer.ui.theme.ModicarePrimary
                            )
                        )
                        
                        // Password Field
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        if (passwordVisible) Icons.Default.Close 
                                        else Icons.Default.Lock,
                                        contentDescription = null
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None 
                                                 else PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = com.example.modicanalyzer.ui.theme.ModicarePrimary,
                                focusedLabelColor = com.example.modicanalyzer.ui.theme.ModicarePrimary
                            )
                        )
                        
                        // Confirm Password (only for signup)
                        if (!isLogin) {
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                label = { Text("Confirm Password") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                trailingIcon = {
                                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                        Icon(
                                            if (confirmPasswordVisible) Icons.Default.Close 
                                            else Icons.Default.Lock,
                                            contentDescription = null
                                        )
                                    }
                                },
                                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None 
                                                     else PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = com.example.modicanalyzer.ui.theme.ModicarePrimary,
                                    focusedLabelColor = com.example.modicanalyzer.ui.theme.ModicarePrimary
                                )
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Submit Button
                        Button(
                            onClick = { 
                                // For demo purposes, just navigate to main activity
                                onLoginSuccess()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = com.example.modicanalyzer.ui.theme.ModicarePrimary
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = if (isLogin) "Sign In" else "Create Account",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}